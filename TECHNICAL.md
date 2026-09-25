# CvReview — Technical Documentation

> Backend service for AI-powered CV analysis against job descriptions, built with Spring Boot, Apache PDFBox, Apache POI, and Groq LLM API.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture](#2-architecture)
3. [Module Breakdown](#3-module-breakdown)
   - [Orchestration](#31-orchestration)
   - [File Validation](#32-file-validation)
   - [Text Extraction](#33-text-extraction)
   - [Parsing (ATS Layout Check)](#34-parsing-ats-layout-check)
   - [AI Analysis](#35-ai-analysis)
   - [Scoring](#36-scoring)
   - [Rate Limiting](#37-rate-limiting)
   - [Util (Cross-cutting Concerns)](#38-util-cross-cutting-concerns)
4. [Request Lifecycle](#4-request-lifecycle)
5. [Data Models (DTOs)](#5-data-models-dtos)
6. [Configuration Reference](#6-configuration-reference)
7. [Error Handling Strategy](#7-error-handling-strategy)
8. [Security Design](#8-security-design)
9. [Infrastructure Dependencies](#9-infrastructure-dependencies)
10. [Design Decisions & Trade-offs](#10-design-decisions--trade-offs)

---

## 1. System Overview

CvReview is a stateless REST API that accepts a CV file (PDF or DOCX) and a job description text, then returns a structured analysis report covering:

- **ATS Layout Score** — whether the CV's format will survive automated parsing systems
- **Keyword Score** — how well the CV's content matches the job description
- **Completeness Score** — quality of writing (active verbs, quantification, concreteness)
- **Rewrite Suggestions** — AI-generated improvements for the weakest bullet points
- **Issue List** — actionable warnings for layout, font, structure, and keyword problems

The service is designed to be **publicly accessible without authentication**, protected instead by a **per-IP, per-day rate limit** enforced at the Redis level.

---

## 2. Architecture

```
Client (HTTP)
     │
     ▼
OrchestratorController   ← Single REST endpoint: POST /api/v1/cv/analyze
     │
     ▼
OrchestratorService      ← Pipeline coordinator; calls each sub-service in order
     │
     ├──► RateLimitService          → Check + consume 1 token from Redis bucket4j
     ├──► FileValidationService      → Validate file type, size, and JD content
     ├──► TextExtractionService      → Parse PDF/DOCX into raw text + structural metadata
     ├──► ParsingService             → Evaluate ATS compliance from structural metadata
     ├──► AiAnalysisService          → Sanitize, mask PII, call Groq LLM, parse response
     └──► ScoringService             → Compute weighted total score + critical warning
```

All services are Spring `@Service` beans managed by DI. The `OrchestratorService` is the only consumer of the full pipeline; sub-services have no awareness of each other.

---

## 3. Module Breakdown

### 3.1 Orchestration

**Package:** `aianalysis.orchestration`

| File | Role |
|---|---|
| `OrchestratorController.java` | REST controller; extracts `clientKey` from request IP; delegates to `OrchestratorService` |
| `OrchestratorService.java` | Coordinates the full analysis pipeline; merges issue lists from parsing and AI |
| `dto/UserInput.java` | Request DTO: `file` (MultipartFile) + `jobDescription` (String) |
| `dto/AnalysisResponse.java` | Response DTO: all scores, issues, suggestions, and critical warning |

**Key behaviors:**
- The `clientKey` used for rate limiting is derived from the client's IP address
- Issue lists from `ParsingService` and `AiAnalysisService` are **merged into a single flat list** in the response
- The `criticalWarning` field is only non-null when `parsingScore < 40`

---

### 3.2 File Validation

**Package:** `aianalysis.filevalidation`

| File | Role |
|---|---|
| `FileValidationService.java` | Validates MIME type (PDF/DOCX), file size limit, and non-empty job description |
| `exception/InvalidInputException.java` | Thrown on validation failure → maps to HTTP 400 |

**Validation rules:**
- Accepted MIME types: `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- File size limit: configurable (default enforced by Spring's multipart config)
- Job description: must not be blank

---

### 3.3 Text Extraction

**Package:** `aianalysis.extraction`

| File | Role |
|---|---|
| `TextExtractionService.java` | Dispatches to PDF or DOCX extraction path; returns `ExtractedContent` |
| `dto/ExtractedContent.java` | Record containing `rawText` (String) and `structuralInfo` (StructuralInfo) |
| `dto/StructuralInfo.java` | Record with layout flags: multiColumn, hasTable, hasTextBox, hasImage, fontsUsed, detectedHeadings, contactInHeaderFooter, extractedTextLength |
| `exception/TextExtractionException.java` | Thrown on IO/parse failure → maps to HTTP 422 |

**PDF extraction** (`extractFromPdf`):
- Uses **Apache PDFBox** (`PDFTextStripper`) to extract raw text
- Detects fonts via `PDResources.getFontNames()`; strips variant suffixes (e.g. `ABCDEF+Arial-Bold` → `Arial`)
- Detects images via `PDXObject` / `PDImageXObject`
- Multi-column detection is performed by `ColumnDetectorStripper` (inner class):
  - Collects all character X-positions during text stripping
  - Splits page width into left zone (< 42%), gutter (42–58%), right zone (> 58%)
  - Flags multi-column if both left and right zones each contain > 20% of characters AND the gutter < 15%
- Table and text-box detection in PDF default to `false` (these are rendered as vector paths by most PDF generators and are not detectable via text-layer analysis)

**DOCX extraction** (`extractFromDocx`):
- Uses **Apache POI** (`XWPFDocument`, `XWPFWordExtractor`)
- Reads fonts from `XWPFRun.getFontFamily()` across all paragraphs and table cells
- Detects headings from Word style (`Heading1`, `Heading2`, etc.) or heuristic: all-caps, ≤ 35 characters
- Detects tables via `document.getTables().isEmpty()`
- Detects text boxes via raw XML inspection (`txbxContent`, `w:txbxContent`, `v:textbox`)
- Detects multi-column via `SectPr.getCols().getNum() > 1`
- Detects images via `document.getAllPictures()`
- Detects contact in header/footer by running a regex (`email | phone`) against `XWPFHeader`/`XWPFFooter` text

**Heading fallback:** If no headings are detected via DOCX styles, a text-based heuristic (`extractHeadingsFromText`) scans all lines ≤ 35 chars that match known section keywords (e.g., EXPERIENCE, EDUCATION, PENGALAMAN) or are fully uppercase alphabetic.

---

### 3.4 Parsing (ATS Layout Check)

**Package:** `aianalysis.parsing`

| File | Role |
|---|---|
| `ParsingService.java` | Evaluates `StructuralInfo` against ATS rules; produces an issue list and parsing score |
| `dto/ParsingResult.java` | Record: `parsingScore` (int) + `issues` (List\<Issue\>) |

**ATS checks performed (in order):**

| Check | Trigger | Severity | Penalty |
|---|---|---|---|
| Multi-column layout | `isMultiColumn == true` | `high` | -20 |
| Table detected | `hasTable == true` | `high` | -20 |
| Text box detected | `hasTextBox == true` | `high` | -20 |
| Image detected | `hasImage == true` | `medium` | -10 |
| Non-standard font | Any font not in `fontWhitelist` | `medium` | -10 |
| Contact in header/footer | `contactInHeaderFooter == true` | `high` | -20 |
| Insufficient text length | `extractedTextLength < minTextLength` | `high` | -20 |
| No recognized section headings | None of `headingWhitelist` detected | `high` | -20 |

**Score formula:**
```
parsingScore = max(100 - Σ(penalties), 0)
```

---

### 3.5 AI Analysis

**Package:** `aianalysis.ai`

| File | Role |
|---|---|
| `AiAnalysisService.java` | Sanitizes text, masks PII, builds prompt, calls Groq API, parses JSON response |
| `dto/AiAnalysisResult.java` | Mapped from Groq JSON: scores, matched/missing keywords, issues, rewrite suggestions |
| `dto/Issue.java` | Record: `category`, `description`, `severity` |
| `dto/RewriteSuggestion.java` | Record: `original`, `suggestion`, `reason` |
| `exception/AiResponseParsingException.java` | Thrown on JSON parse failure after retry → maps to HTTP 502 |

**Processing pipeline inside `analyze()`:**

```
cvText + jobDescription
        │
        ▼
   sanitizeText()          ← Normalize whitespace, strip control chars, neutralize prompt injection
        │
        ▼ (CV only)
     maskPii()             ← Replace email, phone, URL, address, DOB, NIK with placeholders
        │
        ▼
  buildSystemPrompt()      ← Constructs structured evaluation rules with scoring guidelines
        │
        ▼
   callLlmApi()            ← HTTP POST to Groq API via WebClient (blocking)
        │
        ▼
 parseAndValidate()        ← Deserialize JSON to AiAnalysisResult; retry once on failure
        │
        ▼
   AiAnalysisResult
```

**`sanitizeText()` — steps applied to both CV and JD:**
1. Replace non-breaking space variants (`\u00A0`, `\u2007`, `\u202F`) with regular space
2. Strip non-printable control characters (preserves `\t`, `\n`, `\r`)
3. Normalize tabs to single space
4. Collapse 3+ consecutive blank lines to max 2
5. Collapse multiple spaces on same line to 1
6. Replace prompt injection patterns with `[removed]`

**`maskPii()` — applied to CV text only (not JD):**

| PII Type | Placeholder | Notes |
|---|---|---|
| Email address | `[EMAIL]` | Must run before URL to avoid double-replacement |
| URL / social profiles | `[PROFIL_URL]` | Matches `https://`, `www.`, `linkedin.com`, `github.com` |
| Indonesian NIK (16-digit) | `[NIK]` | Matches with or without label prefix |
| Phone numbers | `[NOMOR_TELEPON]` | Indonesian (+62/0xx) and international formats |
| Indonesian street address | `[ALAMAT]` | Heuristic: line starting with Jl., Jalan, RT/RW, Kel., etc. |
| Date of birth | `[TANGGAL_LAHIR]` | Numeric and month-name formats, Indonesian and English |

> **Intentionally NOT masked:** Full name (too error-prone; indistinguishable from company names) and city/province alone (needed for job location context).

**Groq API call (`callLlmApi()`):**
- Uses Spring `WebClient` (reactive HTTP client in blocking mode via `.block()`)
- Request: `model`, `messages` (system + user), `temperature: 0.2` (low for deterministic JSON output), `response_format: {type: "json_object"}` (enforces pure JSON mode)
- Response: navigates `choices[0].message.content` to extract the JSON string

**Retry logic (`parseAndValidate()`):**
- Attempts to parse Groq response as `AiAnalysisResult` via Jackson `ObjectMapper`
- If parsing fails (malformed JSON), retries the Groq API call once
- If the retry also fails, throws `AiResponseParsingException` → HTTP 502

**System prompt key evaluation criteria:**
- **Keyword matching:** Literal match with tolerance for standard abbreviations/acronyms; keyword density 2–3 repetitions optimal; rewards keywords in achievement bullets over skill lists
- **Writing quality (Harvard CV principles):** Active verbs, sentence fragments, no personal pronouns, concrete context + quantification
- **Structure:** Important information visible early; sections appropriate to role
- **Output language:** Detects dominant CV language (Indonesian/English) and responds in the same language

---

### 3.6 Scoring

**Package:** `aianalysis.scoring`

| File | Role |
|---|---|
| `ScoringService.java` | Calculates weighted total score; determines if parsing score is critical |

**Weighted total score formula:**
```
totalScore = round(
    parsingScore      × weightParsing      +   // default: 0.4
    keywordScore      × weightKeyword      +   // default: 0.4
    completenessScore × weightCompleteness     // default: 0.2
)
```

**Critical parsing threshold:**
```
CRITICAL_PARSING_THRESHOLD = 40
```
If `parsingScore < 40`, `isCritical = true` and `criticalWarning` is set to a fixed Indonesian-language string advising the user to fix layout issues before anything else.

---

### 3.7 Rate Limiting

**Package:** `ratelimit`

| File | Role |
|---|---|
| `RateLimitService.java` | Enforces per-client daily usage quota using Bucket4j + Redis |
| `exception/RateLimitExceededException.java` | Thrown when quota is exhausted → maps to HTTP 429 |

**Implementation detail:**
- Uses **Bucket4j** with a `LettuceBasedProxyManager` (Lettuce = async Redis client)
- Each `clientKey` (typically the client IP address) maps to a unique Redis key holding bucket state
- Bucket configuration: **3 tokens maximum**, refilled to 3 **every 24 hours** (`Refill.intervally`)
- Each successful `checkLimit()` call consumes 1 token via `bucket.tryConsume(1)`
- Because bucket state is stored in Redis, limits persist across application restarts and work correctly across multiple backend instances

**Why distributed rate limiting?**  
An in-memory counter would reset on restart and would be incorrect in a horizontally scaled deployment. Redis ensures the counter is shared and durable.

---

### 3.8 Util (Cross-cutting Concerns)

**Package:** `util`

#### Config

| File | Role |
|---|---|
| `config/ParsingConfig.java` | `@ConfigurationProperties(prefix = "ats.rules")` — exposes all ATS rule thresholds and weights as typed beans |
| `config/RedisConfig.java` | Creates the `LettuceBasedProxyManager` bean wired into `RateLimitService` |

#### Exception

| File | Role |
|---|---|
| `exception/ErrorCode.java` | Enum of application error codes, each carrying an `HttpStatus` and a message |
| `exception/BusinessException.java` | Base runtime exception carrying an `ErrorCode`; used for domain-specific errors |
| `exception/GlobalException.java` | `@RestControllerAdvice` — centralized exception-to-HTTP-response mapping (see §7) |

#### Logging

| File | Role |
|---|---|
| `logging/TraceIdFilter.java` | Servlet filter that injects a `traceId` into MDC (Mapped Diagnostic Context) for every request, enabling correlated log tracing |

#### Response

| File | Role |
|---|---|
| `response/ApiResponse.java` | Generic wrapper: `{ "success": bool, "data": T, "message": String }` — used for all API responses |

---

## 4. Request Lifecycle

```
POST /api/v1/cv/analyze
Content-Type: multipart/form-data
Body: file=<CV file>, jobDescription=<JD text>
```

Step-by-step flow:

```
1. OrchestratorController receives request
   └─ Extracts clientKey from X-Forwarded-For or remoteAddr
   └─ Wraps in UserInput DTO, delegates to OrchestratorService

2. RateLimitService.checkLimit(clientKey)
   └─ Loads bucket from Redis; throws RateLimitExceededException if 0 tokens remain
   └─ Consumes 1 token on success

3. FileValidationService.validate(file, jobDescription)
   └─ Validates MIME type and non-empty JD; throws InvalidInputException on failure

4. TextExtractionService.extract(file)
   └─ Dispatches to PDF or DOCX parser
   └─ Returns ExtractedContent { rawText, structuralInfo }

5. ParsingService.evaluate(structuralInfo, rawText)
   └─ Runs ATS compliance checks
   └─ Returns ParsingResult { parsingScore, issues[] }

6. AiAnalysisService.analyze(rawText, jobDescription)
   └─ Sanitizes + masks PII from CV text
   └─ Calls Groq LLM API
   └─ Returns AiAnalysisResult { keywordScore, completenessScore, keywordIssues[], rewriteSuggestions[], ... }

7. ScoringService.calculateTotalScore(...)
   └─ Returns weighted integer total score

8. OrchestratorService merges issues, builds AnalysisResponse
   └─ Returns to controller → wrapped in ApiResponse → HTTP 200

On any exception:
   └─ GlobalException handler maps to appropriate HTTP status and ApiResponse.error(message)
```

---

## 5. Data Models (DTOs)

### Request

```java
// Sent as multipart/form-data
UserInput {
    MultipartFile file           // CV file (PDF or DOCX)
    String        jobDescription // Target job description text
}
```

### Response

```java
AnalysisResponse {
    int           totalScore           // Weighted aggregate score (0–100)
    int           parsingScore         // ATS layout compliance score (0–100)
    int           keywordScore         // Keyword match score from AI (0–100)
    int           completenessScore    // Writing quality score from AI (0–100)
    boolean       isCritical           // true if parsingScore < 40
    String        criticalWarning      // Non-null only when isCritical == true
    List<Issue>   issues               // Merged layout + keyword issues
    List<RewriteSuggestion> rewriteSuggestions // AI-generated bullet rewrites (max 3)
}

Issue {
    String category    // "layout" | "font" | "contact" | "integrity" | "structure" | "keyword"
    String description // Human-readable explanation + action hint
    String severity    // "high" | "medium" | "low"
}

RewriteSuggestion {
    String original    // Original bullet text from CV
    String suggestion  // AI-rewritten version
    String reason      // Explanation of why it was improved
}
```

### Internal DTOs

```java
ExtractedContent {
    String        rawText        // Full plain-text content of the CV
    StructuralInfo structuralInfo
}

StructuralInfo {
    boolean       isMultiColumn
    boolean       hasTable
    boolean       hasTextBox
    boolean       hasImage
    List<String>  fontsUsed
    List<String>  detectedHeadings
    boolean       contactInfoInHeaderFooter
    int           extractedTextLength
}

ParsingResult {
    int           parsingScore
    List<Issue>   issues
}

AiAnalysisResult {
    int              keywordScore
    int              completenessScore
    List<String>     matchedKeywords
    List<String>     missingKeywords
    List<Issue>      keywordIssues
    List<RewriteSuggestion> rewriteSuggestions
}
```

---

## 6. Configuration Reference

All properties are in `application.properties`:

### Redis

| Property | Default | Description |
|---|---|---|
| `spring.data.redis.host` | `localhost` | Redis server hostname |
| `spring.data.redis.port` | `6380` | Redis server port |
| `spring.data.redis.password` | `redismann` | Redis auth password |
| `spring.data.redis.timeout` | `2000ms` | Connection timeout |

### AI API

| Property | Description |
|---|---|
| `ai.api.key` | Groq API key (externalize via env var in production) |
| `ai.api.url` | Groq chat completions endpoint |
| `ai.api.model` | LLM model to use (e.g. `llama-3.3-70b-versatile`) |

### ATS Rules (`ats.rules.*`)

| Property | Default | Description |
|---|---|---|
| `heading-whitelist` | `EXPERIENCE,EDUCATION,SKILLS,SUMMARY,CERTIFICATIONS` | Required CV section headings |
| `font-whitelist` | `Arial,Calibri,Times New Roman,Georgia` | ATS-safe font names |
| `min-text-length` | `200` | Minimum extracted characters before flagging as image-based CV |
| `keyword-density-min` | `2` | Minimum keyword repetitions considered optimal |
| `keyword-density-max` | `3` | Maximum keyword repetitions before flagging as stuffing |
| `weight-parsing` | `0.4` | Weight for parsingScore in total score formula |
| `weight-keyword` | `0.4` | Weight for keywordScore in total score formula |
| `weight-completeness` | `0.2` | Weight for completenessScore in total score formula |

> Weights must sum to 1.0. If changed, update all three properties together.

---

## 7. Error Handling Strategy

All exceptions are caught by `GlobalException` (`@RestControllerAdvice`) and returned as `ApiResponse.error(message)`:

| Exception | HTTP Status | Logged as |
|---|---|---|
| `BusinessException` | Defined by `ErrorCode` enum | `WARN` |
| `MethodArgumentNotValidException` | `400 Bad Request` | `WARN` |
| `InvalidInputException` | `400 Bad Request` | `WARN` |
| `RateLimitExceededException` | `429 Too Many Requests` | `WARN` |
| `TextExtractionException` | `422 Unprocessable Entity` | `ERROR` with stack trace |
| `AiResponseParsingException` | `502 Bad Gateway` | `ERROR` with stack trace |
| `Exception` (catch-all) | `500 Internal Server Error` | `ERROR` with stack trace |

The catch-all handler returns a **generic message** (not the raw exception message) to avoid leaking internal details to the client.

---

## 8. Security Design

### Prompt Injection Prevention
Before any text is sent to the Groq API, `sanitizeText()` runs a regex (`INJECTION_PATTERN`) that replaces known injection phrases (e.g., "ignore previous instructions", "act as", "forget everything") with `[removed]`. This prevents a malicious CV from hijacking the LLM's behavior.

### PII Minimization
The `maskPii()` method replaces all personally identifiable information from the CV text with neutral placeholders **before** it leaves the backend to a third-party LLM provider. This includes email, phone, URLs, addresses, dates of birth, and Indonesian national ID numbers (NIK).

### Rate Limiting
Enforced at the Redis level (per IP, 3 requests/day) to prevent abuse of the Groq API quota and to control costs.

### API Key Management
The Groq API key is read from `application.properties` via `@Value("${ai.api.key}")`. In production, this **must** be injected as an environment variable and not committed to version control.

---

## 9. Infrastructure Dependencies

| Component | Technology | Purpose |
|---|---|---|
| **Web Framework** | Spring Boot + Spring WebFlux (WebClient) | HTTP server + reactive HTTP client for Groq API calls |
| **PDF Parsing** | Apache PDFBox 3.x | Extract text, fonts, images from PDF files |
| **DOCX Parsing** | Apache POI (XWPF) | Extract text, fonts, structure from DOCX files |
| **AI Provider** | Groq Cloud (Llama 3.3 70B) | Keyword + writing quality analysis |
| **Rate Limiting** | Bucket4j + Lettuce | Distributed token-bucket rate limiting |
| **Cache / State** | Redis | Stores Bucket4j rate limit state per client IP |
| **JSON** | Jackson `ObjectMapper` | Serialize Groq request, deserialize AI response |
| **Logging** | SLF4J + Logback + MDC | Structured logging with trace ID per request |

---

## 10. Design Decisions & Trade-offs

### Why a single public endpoint without authentication?
The service is intended as a public utility tool. Authentication would add friction for end users. Rate limiting at the IP level provides sufficient abuse protection without requiring account management.

### Why Groq instead of OpenAI?
Groq provides significantly faster inference (LPU-based hardware) at lower cost for the Llama family of models, which is important for keeping per-request latency acceptable in a synchronous pipeline.

### Why is `WebClient` used in blocking mode (`.block()`)?
The rest of the pipeline is synchronous (file I/O, PDFBox, POI). Using blocking WebClient avoids mixing reactive and imperative programming models in the same service, keeping the code simpler and easier to reason about. The downside is that each request holds a thread during the Groq API wait time.

### Why no database?
The analysis result is stateless and returned directly in the response. There is no history, user profile, or stored analysis needed for the current feature scope. This simplifies deployment significantly.

### Why mask PII before sending to the LLM instead of anonymizing after?
Anonymizing after would require the full PII to have already left the backend. Masking before ensures no identifying data is ever transmitted to a third-party service, which is the more privacy-preserving approach.

### Why `Refill.intervally` instead of `Refill.greedy` for rate limiting?
`intervally` refills all tokens at once after the period (midnight-equivalent behavior), meaning a user gets exactly 3 uses per day window. `greedy` would spread the refill (0.0347 tokens/minute), allowing users to get additional uses within the same day by spacing requests. `intervally` better matches the intended "3 per day" UX.

### Why not truncate CV text before sending to the AI?
CV length varies per person and all content is needed for accurate analysis (skills, experience, projects, education all contribute to keyword coverage). Truncating would produce biased, incomplete scores. The trade-off is higher token cost per request.
