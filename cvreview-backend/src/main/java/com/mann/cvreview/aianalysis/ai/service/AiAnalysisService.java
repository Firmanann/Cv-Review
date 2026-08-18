package com.mann.cvreview.aianalysis.ai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mann.cvreview.aianalysis.ai.dto.AiAnalysisResult;
import com.mann.cvreview.aianalysis.ai.exception.AiResponseParsingException;
import com.mann.cvreview.util.config.ParsingConfig;

@Service
public class AiAnalysisService {

    private final ParsingConfig config;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.model}")
    private String modelName;

    // Constructor: manually instantiate ObjectMapper & WebClient without DI beans
    public AiAnalysisService(ParsingConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.webClient = WebClient.create();
    }

    // Prompt injection patterns to neutralize before sending to AI
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore (all )?(previous|prior|above) instructions?|disregard .{0,40}prompt|" +
                    "you are now|forget everything|act as|pretend (you are|to be)|" +
                    "system:|<system>|\\[INST\\]|\\[/INST\\])");

    // PII patterns — replaced with neutral placeholders before text reaches the AI
    // provider
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+62|0)[\\s\\-]?[0-9]{2,4}[\\s\\-]?[0-9]{3,4}[\\s\\-]?[0-9]{3,6}" + // Indonesian format
                    "|(?:\\+[1-9][0-9]{0,2}[\\s\\-]?)?(?:\\([0-9]{1,4}\\)[\\s\\-]?)?[0-9]{6,14}" // International format
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?:https?://|www\\.|linkedin\\.com|github\\.com)[^\\s<>\"']{3,}");
    // Indonesian street address heuristic: detects lines starting with
    // Jl./Jalan/Gang/Gg. etc.
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(?i)(?:jl\\.|jalan|gang|gg\\.|perum\\.|perumahan|komplek|blok|rt\\s?/\\s?rw|" +
                    "kelurahan|kecamatan|kabupaten|kota|provinsi|kel\\.|kec\\.|kab\\.)[^\\n]{3,60}");
    // Date of birth patterns: DD/MM/YYYY, DD-MM-YYYY, Month DD YYYY, DD Month YYYY
    private static final Pattern DOB_PATTERN = Pattern.compile(
            "(?i)(?:(?:tanggal lahir|ttl|tgl lahir|date of birth|dob|born)[\\s:,]+)?" +
                    "(?:(?:0?[1-9]|[12][0-9]|3[01])[\\s/\\-.])" +
                    "(?:(?:0?[1-9]|1[0-2])[\\s/\\-.](?:19|20)[0-9]{2}|" +
                    "(?:januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember|" +
                    "january|february|march|april|may|june|july|august|september|october|november|december)" +
                    "[\\s,]+(?:19|20)[0-9]{2})");
    // Indonesian NIK (16-digit national ID number)
    private static final Pattern NIK_PATTERN = Pattern.compile(
            "(?i)(?:nik|no\\.?\\s*ktp|ktp)[\\s:]*[0-9]{16}|(?<![0-9])[0-9]{16}(?![0-9])");

    public AiAnalysisResult analyze(String cvText, String jobDescription) {

        // Step 1: Sanitize raw text (whitespace, control chars, injection)
        String sanitizedCv = sanitizeText(cvText);
        String sanitizedJd = sanitizeText(jobDescription);

        // Step 2: Mask PII from CV text only — JD is a job posting, no personal data
        // expected
        String maskedCv = maskPii(sanitizedCv);

        String systemPrompt = buildSystemPrompt();
        String userMessage = "CV:\n" + maskedCv + "\n\nJob Description:\n" + sanitizedJd;

        // 1. Call Groq LLM API
        String rawResponse = callLlmApi(systemPrompt, userMessage);

        // 2. Parse AI JSON response (retry once if format is invalid)
        return parseAndValidate(rawResponse, () -> callLlmApi(systemPrompt, userMessage));
    }

    /**
     * Sanitize raw text before sending to AI:
     * - Normalize whitespace (tabs, excessive blank lines, non-breaking spaces)
     * - Remove non-printable control characters from OCR artifacts
     * - Neutralize prompt injection patterns
     * NOTE: PII masking is handled separately by maskPii() and applied to CV text
     * only.
     * NOTE: No length truncation — CV length varies per person and all content is
     * needed for analysis.
     */
    private String sanitizeText(String text) {
        if (text == null || text.isBlank())
            return "";

        String result = text;

        // 1. Replace non-breaking spaces and other Unicode whitespace variants with
        // regular space
        result = result.replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ');

        // 2. Remove non-printable control characters (except \t, \n, \r which are
        // meaningful whitespace)
        result = result.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 3. Normalize tabs to a single space
        result = result.replace('\t', ' ');

        // 4. Collapse 3+ consecutive blank lines to a maximum of 2
        result = result.replaceAll("(\\r?\\n){3,}", "\n\n");

        // 5. Collapse multiple spaces on the same line to a single space
        result = result.replaceAll("[ ]{2,}", " ");

        // 6. Neutralize prompt injection patterns (replace with a safe placeholder)
        result = INJECTION_PATTERN.matcher(result).replaceAll("[removed]");

        return result.strip();
    }

    /**
     * Mask PII from CV text before sending to the AI provider.
     * Replaces sensitive personal data with neutral placeholders so that
     * no identifying information leaves the backend to a third-party service.
     *
     * Masking targets (in safe order):
     * 1. Email addresses
     * 2. URLs and social/portfolio profile links
     * 3. Phone numbers (Indonesian + international)
     * 4. Indonesian street addresses (heuristic: Jl., Jalan, RT/RW, Kel., Kec.,
     * etc.)
     * 5. Dates of birth (numeric & month-name formats)
     * 6. Indonesian NIK / KTP number (16-digit)
     *
     * Intentionally NOT masked:
     * - Full name: extremely error-prone — indistinguishable from company/job
     * names.
     * - City/province alone: too broad, needed for job location context.
     */
    private String maskPii(String text) {
        if (text == null || text.isBlank())
            return "";

        String result = text;

        // 1. Mask email addresses (must run before URL to avoid double-processing)
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL]");

        // 2. Mask URLs and social profile links (LinkedIn, GitHub, portfolio, etc.)
        result = URL_PATTERN.matcher(result).replaceAll("[PROFIL_URL]");

        // 3. Mask phone numbers (Indonesian and international formats)
        result = PHONE_PATTERN.matcher(result).replaceAll("[NOMOR_TELEPON]");

        // 4. Mask Indonesian street addresses (heuristic based on address keywords)
        result = ADDRESS_PATTERN.matcher(result).replaceAll("[ALAMAT]");

        // 5. Mask dates of birth
        result = DOB_PATTERN.matcher(result).replaceAll("[TANGGAL_LAHIR]");

        // 6. Mask Indonesian NIK / KTP number (16-digit)
        result = NIK_PATTERN.matcher(result).replaceAll("[NIK]");

        return result;
    }

    private String buildSystemPrompt() {
        return """
                Kamu adalah sistem analisa CV profesional yang mengevaluasi kesesuaian CV terhadap job description
                berdasarkan standar ATS dan prinsip penulisan CV Harvard Career Center.
                CV yang baik harus menjawab pertanyaan: "Why should we select YOU?"

                ATURAN EVALUASI:

                [A. KEYWORD & RELEVANSI]
                1. Matching keyword bersifat literal. NAMUN kenali variasi penulisan yang secara umum merujuk
                   hal yang sama berdasarkan konteks field/industri pada JD (singkatan resmi, akronim umum,
                   variasi ejaan standar). Selain itu, jangan asumsikan sinonim yang tidak berkaitan langsung.
                2. Nilai posisi keyword: keyword di bullet pencapaian lebih kredibel daripada di skill list saja.
                3. Density optimal: %d-%d repetisi per keyword. Di atas itu anggap keyword stuffing.
                4. Gunakan terminologi yang sesuai dengan field dan audience pada JD.

                [B. KUALITAS PENULISAN (Harvard CV Principles)]
                5. Evaluasi apakah setiap bullet pengalaman sudah menggunakan:
                   - Active verb di awal kalimat (bukan passive seperti "bertanggung jawab untuk...")
                   - Sentence fragment yang padat dan konkret (bukan narasi panjang atau bertele-tele)
                   - Tidak menggunakan personal pronoun (I, me, my, saya, aku)
                   - Konteks konkret: metode, scope, tanggung jawab, dan hasil yang dicapai
                6. Kuantifikasi (angka, jumlah, skala, hasil) sangat dianjurkan jika relevan dan tersedia di CV.
                   DILARANG mengarang angka yang tidak ada di CV asli.
                   Jika data kuantitatif tidak tersedia, gunakan placeholder: "[isi: ...]".

                [C. STRUKTUR & HIERARKI]
                7. Evaluasi apakah informasi yang paling relevan dengan JD mudah ditemukan dan tidak terselip
                   di bagian bawah CV (Harvard principle: informasi terpenting harus menonjol lebih awal).
                8. Evaluasi apakah kategori/section yang ditampilkan sesuai dengan kebutuhan posisi pada JD
                   (bukan berdasarkan template generic, tapi konteks field dan tujuan posisi).

                [D. PANDUAN SKOR]
                9. Panduan keywordScore:
                   90-100: >80%% keyword penting dari JD ada di CV dengan konteks pencapaian konkret
                   70-89 : 60-80%% keyword ada, sebagian hanya di skill list tanpa konteks
                   50-69 : 40-60%% keyword ada, banyak yang missing atau stuffing
                   <50   : <40%% keyword cocok atau CV tidak relevan dengan posisi

                10. Panduan completenessScore (menilai kualitas isi dan cara penulisan):
                    90-100: Setiap bullet menggunakan active verb + konteks konkret + kuantifikasi terukur
                    70-89 : Mayoritas bullet sudah konkret tapi minim kuantifikasi
                    50-69 : Banyak bullet hanya berupa deskripsi tanggung jawab tanpa pencapaian
                    <50   : Hampir semua bullet pasif, generik, atau tidak relevan dengan posisi

                [E. OUTPUT]
                11. Deteksi bahasa dominan CV (Indonesia/Inggris). Gunakan bahasa yang SAMA untuk semua
                    field 'description' dan 'suggestion'. Jangan campur bahasa.
                12. Untuk rewriteSuggestions: Pilih MAKSIMAL 3 bullet yang paling lemah DAN paling
                    relevan dengan JD. Prioritaskan bullet yang tidak menggunakan active verb atau tidak ada
                    konteks konkret. Jangan beri saran untuk bullet yang sudah baik.
                    Formula rewrite: Active Verb + Konteks Konkret + Kuantifikasi (jika tersedia).
                13. Untuk keywordIssues: Akhiri setiap deskripsi dengan format "→ [Saran: tindakan konkret]".

                WAJIB balas HANYA dalam format JSON berikut tanpa teks tambahan apapun:
                {
                  "keywordScore": 0-100,
                  "completenessScore": 0-100,
                  "matchedKeywords": ["..."],
                  "missingKeywords": ["..."],
                  "keywordIssues": [{"category": "keyword", "description": "... → [Saran: ...]", "severity": "medium"}],
                  "rewriteSuggestions": [{"original": "...", "suggestion": "...", "reason": "..."}]
                }
                """.formatted(config.getKeywordDensityMin(), config.getKeywordDensityMax());
    }

    private String callLlmApi(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)));
        requestBody.put("temperature", 0.2); // Low temperature for consistent JSON output
        requestBody.put("response_format", Map.of("type", "json_object")); // Enforce pure JSON mode

        try {
            // Send HTTP POST request to Groq API via WebClient
            Map<?, ?> response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
            throw new RuntimeException("Empty response from Groq API");
        } catch (Exception e) {
            throw new RuntimeException("Failed to contact Groq API: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null)
            return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private AiAnalysisResult parseAndValidate(String rawResponse, Supplier<String> retryFn) {
        try {
            String cleaned = cleanJsonResponse(rawResponse);
            return objectMapper.readValue(cleaned, AiAnalysisResult.class);
        } catch (Exception e) {
            try {
                // Retry once if first JSON format parsing failed
                String retriedResponse = retryFn.get();
                String cleanedRetry = cleanJsonResponse(retriedResponse);
                return objectMapper.readValue(cleanedRetry, AiAnalysisResult.class);
            } catch (Exception e2) {
                throw new AiResponseParsingException("Failed to parse AI JSON response after retry", e2);
            }
        }
    }
}