package com.genai.knowitall.service;

import com.genai.knowitall.service.exception.TextExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Service for extracting text content from various document formats.
 *
 * Supported formats:
 * - PDF (using Apache PDFBox)
 * - DOCX (using Apache POI)
 *
 * Text is extracted page-by-page with metadata preservation.
 */
@Service
public class TextExtractionService {

    private static final Logger logger = Logger.getLogger(TextExtractionService.class.getName());

    /**
     * Extract text from a PDF document.
     *
     * @param inputStream PDF file input stream
     * @param filename Original filename for error messages
     * @return Extracted text content
     * @throws TextExtractionException if extraction fails
     */
    public String extractFromPdf(InputStream inputStream, String filename) {
        logger.info("Extracting text from PDF: " + filename);

        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new TextExtractionException("PDF contains no extractable text: " + filename);
            }

            logger.info("Successfully extracted " + text.length() + " characters from PDF: " + filename);
            return text;

        } catch (IOException e) {
            logger.severe("Failed to extract text from PDF: " + filename + " - " + e.getMessage());
            throw new TextExtractionException("Failed to extract text from PDF: " + filename, e);
        }
    }

    /**
     * Extract text from a DOCX document.
     *
     * @param inputStream DOCX file input stream
     * @param filename Original filename for error messages
     * @return Extracted text content
     * @throws TextExtractionException if extraction fails
     */
    public String extractFromDocx(InputStream inputStream, String filename) {
        logger.info("Extracting text from DOCX: " + filename);

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder textBuilder = new StringBuilder();

            // Extract text from all paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    textBuilder.append(paragraphText).append("\n");
                }
            }

            String text = textBuilder.toString();

            if (text.trim().isEmpty()) {
                throw new TextExtractionException("DOCX contains no extractable text: " + filename);
            }

            logger.info("Successfully extracted " + text.length() + " characters from DOCX: " + filename);
            return text;

        } catch (IOException e) {
            logger.severe("Failed to extract text from DOCX: " + filename + " - " + e.getMessage());
            throw new TextExtractionException("Failed to extract text from DOCX: " + filename, e);
        }
    }

    /**
     * Extract text from a document based on content type.
     *
     * @param inputStream Document input stream
     * @param contentType MIME content type
     * @param filename Original filename
     * @return Extracted text content
     * @throws TextExtractionException if extraction fails or format is unsupported
     */
    public String extractText(InputStream inputStream, String contentType, String filename) {
        logger.info("Extracting text from document: " + filename + " (type: " + contentType + ")");

        if (contentType == null) {
            throw new TextExtractionException("Content type is null for file: " + filename);
        }

        if (contentType.equals("application/pdf")) {
            return extractFromPdf(inputStream, filename);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractFromDocx(inputStream, filename);
        } else if (contentType.equals("text/plain")) {
            return extractFromText(inputStream, filename);
        } else {
            throw new TextExtractionException("Unsupported content type: " + contentType + " for file: " + filename);
        }
    }

    /**
     * Extract text from a plain text file (for testing).
     */
    private String extractFromText(InputStream inputStream, String filename) {
        logger.info("Extracting text from plain text file: " + filename);
        try {
            byte[] bytes = inputStream.readAllBytes();
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            if (text.trim().isEmpty()) {
                throw new TextExtractionException("Text file is empty: " + filename);
            }

            logger.info("Successfully extracted " + text.length() + " characters from text file: " + filename);
            return text;
        } catch (IOException e) {
            logger.severe("Failed to read text file: " + filename + " - " + e.getMessage());
            throw new TextExtractionException("Failed to read text file: " + filename, e);
        }
    }
}
