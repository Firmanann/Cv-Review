package com.mann.cvreview.aianalysis.extraction.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mann.cvreview.aianalysis.extraction.dto.ExtractedContent;
import com.mann.cvreview.aianalysis.extraction.dto.StructuralInfo;
import com.mann.cvreview.aianalysis.extraction.exception.TextExtractionException;

@Service
public class TextExtractionService {

    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|\\+?\\d[\\d -]{8,}\\d)"
    );

    private static final Set<String> COMMON_SECTION_KEYWORDS = Set.of(
            "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT", "EDUCATION", "SKILLS",
            "TECHNICAL SKILLS", "SUMMARY", "PROFILE", "PROJECTS", "CERTIFICATIONS",
            "CERTIFICATES", "ORGANIZATION", "LEADERSHIP", "AWARDS", "PUBLICATIONS",
            "LANGUAGES", "PENGALAMAN", "PENDIDIKAN", "KEAHLIAN", "RINGKASAN", "PROYEK"
    );

    public ExtractedContent extract(MultipartFile file) {
        String contentType = file.getContentType();
        try {
            if ("application/pdf".equals(contentType)) {
                return extractFromPdf(file);
            } else {
                return extractFromDocx(file);
            }
        } catch (Exception e) {
            throw new TextExtractionException("Failed to read CV content, ensure file is not locked or corrupted", e);
        }
    }

    // Extract text content and structure from PDF using PDFBox
    private ExtractedContent extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            ColumnDetectorStripper stripper = new ColumnDetectorStripper();
            String rawText = stripper.getText(document);

            PDPage firstPage = document.getNumberOfPages() > 0 ? document.getPage(0) : null;
            boolean isMultiColumn = stripper.isMultiColumn(firstPage);

            StructuralInfo info = analyzePdfStructure(document, rawText, isMultiColumn);
            return new ExtractedContent(rawText, info);
        }
    }

    // Extract text content and structure from DOCX using Apache POI
    private ExtractedContent extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String rawText = extractor.getText();

            StructuralInfo info = analyzeDocxStructure(document, rawText);
            return new ExtractedContent(rawText, info);
        }
    }

    // Analyze PDF document structure and extract layout metadata
    private StructuralInfo analyzePdfStructure(PDDocument document, String rawText, boolean isMultiColumn) {
        Set<String> fontsUsed = new LinkedHashSet<>();
        boolean hasImage = false;

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources != null) {
                
                // 1. Extract font names
                for (COSName fontName : resources.getFontNames()) {
                    try {
                        PDFont font = resources.getFont(fontName);
                        if (font != null && font.getName() != null) {
                            fontsUsed.add(cleanFontName(font.getName()));
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Detect images
                if (!hasImage) {
                    for (COSName xName : resources.getXObjectNames()) {
                        try {
                            PDXObject xObject = resources.getXObject(xName);
                            if (xObject instanceof PDImageXObject) {
                                hasImage = true;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (fontsUsed.isEmpty()) {
            fontsUsed.add("Arial");
        }

        List<String> detectedHeadings = extractHeadingsFromText(rawText);
        boolean contactInHeaderFooter = checkContactInHeaderFooterText(rawText);

        return new StructuralInfo(
                isMultiColumn,
                false, // Tables in PDF are usually drawn as vector paths, default to false
                false, // Text boxes in PDF
                hasImage,
                new ArrayList<>(fontsUsed),
                detectedHeadings,
                contactInHeaderFooter,
                rawText != null ? rawText.length() : 0
        );
    }

    // Analyze DOCX document structure and extract layout metadata
    private StructuralInfo analyzeDocxStructure(XWPFDocument document, String rawText) {
        Set<String> fontsUsed = new LinkedHashSet<>();
        List<String> detectedHeadings = new ArrayList<>();

        // 1. Extract fonts and headings from paragraphs
        for (XWPFParagraph p : document.getParagraphs()) {
            for (XWPFRun r : p.getRuns()) {
                String font = r.getFontFamily();
                if (font != null && !font.isBlank()) {
                    fontsUsed.add(font.trim());
                }
            }

            String text = p.getText() != null ? p.getText().trim() : "";
            if (!text.isEmpty()) {
                String style = p.getStyle();
                if (style != null && style.toUpperCase().contains("HEADING")) {
                    detectedHeadings.add(text.toUpperCase());
                } else if (text.length() <= 35 && isLikelyHeading(text)) {
                    detectedHeadings.add(text.toUpperCase());
                }
            }
        }

        // Check tables in DOCX
        boolean hasTable = !document.getTables().isEmpty();
        if (hasTable) {
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                String font = r.getFontFamily();
                                if (font != null && !font.isBlank()) {
                                    fontsUsed.add(font.trim());
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Detect images
        boolean hasImage = !document.getAllPictures().isEmpty();

        // 3. Detect text boxes
        boolean hasTextBox = false;
        try {
            String docXml = document.getDocument().toString();
            hasTextBox = docXml.contains("txbxContent") || docXml.contains("w:txbxContent") || docXml.contains("v:textbox");
        } catch (Exception ignored) {}

        // 4. Detect multi-column section
        boolean isMultiColumn = false;
        try {
            if (document.getDocument().getBody().isSetSectPr()) {
                var sectPr = document.getDocument().getBody().getSectPr();
                if (sectPr.isSetCols() && sectPr.getCols().isSetNum()) {
                    isMultiColumn = sectPr.getCols().getNum().intValue() > 1;
                }
            }
        } catch (Exception ignored) {}

        // 5. Detect contact info in Header / Footer
        boolean contactInHeaderFooter = false;
        for (XWPFHeader header : document.getHeaderList()) {
            if (header.getText() != null && CONTACT_PATTERN.matcher(header.getText()).find()) {
                contactInHeaderFooter = true;
                break;
            }
        }
        if (!contactInHeaderFooter) {
            for (XWPFFooter footer : document.getFooterList()) {
                if (footer.getText() != null && CONTACT_PATTERN.matcher(footer.getText()).find()) {
                    contactInHeaderFooter = true;
                    break;
                }
            }
        }

        if (fontsUsed.isEmpty()) {
            fontsUsed.add("Calibri");
        }

        if (detectedHeadings.isEmpty()) {
            detectedHeadings = extractHeadingsFromText(rawText);
        }

        return new StructuralInfo(
                isMultiColumn,
                hasTable,
                hasTextBox,
                hasImage,
                new ArrayList<>(fontsUsed),
                detectedHeadings,
                contactInHeaderFooter,
                rawText != null ? rawText.length() : 0
        );
    }

    private String cleanFontName(String rawFontName) {
        String name = rawFontName;
        if (name.contains("+")) {
            name = name.substring(name.indexOf("+") + 1);
        }
        if (name.contains(",")) {
            name = name.split(",")[0];
        }
        if (name.contains("-")) {
            name = name.split("-")[0];
        }
        return name.trim();
    }

    private List<String> extractHeadingsFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) return Collections.emptyList();
        List<String> headings = new ArrayList<>();
        String[] lines = rawText.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= 35 && isLikelyHeading(trimmed)) {
                headings.add(trimmed.toUpperCase());
            }
        }
        return headings;
    }

    private boolean isLikelyHeading(String line) {
        String upper = line.toUpperCase();
        for (String keyword : COMMON_SECTION_KEYWORDS) {
            if (upper.equals(keyword) || upper.startsWith(keyword + " ") || upper.endsWith(" " + keyword)) {
                return true;
            }
        }
        return line.length() >= 3 && line.length() <= 25 && line.equals(upper) && line.matches("^[A-Z\\s&/]+$");
    }

    private boolean checkContactInHeaderFooterText(String rawText) {
        if (rawText == null || rawText.isBlank()) return false;
        String[] lines = rawText.split("\\R");
        if (lines.length <= 4) return false;

        // Check first 2 lines (Header area) or last 2 lines (Footer area)
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            if (CONTACT_PATTERN.matcher(lines[i]).find()) {
                return true;
            }
        }
        for (int i = Math.max(0, lines.length - 2); i < lines.length; i++) {
            if (CONTACT_PATTERN.matcher(lines[i]).find()) {
                return true;
            }
        }
        return false;
    }

    // Helper Stripper to detect multi-column PDF layouts via character X-coordinate distribution
    private static class ColumnDetectorStripper extends PDFTextStripper {
        private final List<Float> xPositions = new ArrayList<>();

        public ColumnDetectorStripper() throws IOException {
            super();
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            if (text != null && text.getUnicode() != null && !text.getUnicode().isBlank()) {
                xPositions.add(text.getXDirAdj());
            }
            super.processTextPosition(text);
        }

        public boolean isMultiColumn(PDPage firstPage) {
            if (xPositions.size() < 60) return false;

            float pageWidth = 595f; // Default A4 width in pt
            if (firstPage != null && firstPage.getMediaBox() != null) {
                pageWidth = firstPage.getMediaBox().getWidth();
            }

            float midLeft = pageWidth * 0.42f;
            float midRight = pageWidth * 0.58f;

            long leftCount = xPositions.stream().filter(x -> x < midLeft).count();
            long rightCount = xPositions.stream().filter(x -> x > midRight).count();
            long gutterCount = xPositions.stream().filter(x -> x >= midLeft && x <= midRight).count();

            double total = xPositions.size();
            return (leftCount / total > 0.20) && (rightCount / total > 0.20) && (gutterCount / total < 0.15);
        }
    }
}