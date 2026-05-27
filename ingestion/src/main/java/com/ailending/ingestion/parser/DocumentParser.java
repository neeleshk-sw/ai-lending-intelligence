package com.ailending.ingestion.parser;

/**
 * Interface for parsing documents of various formats into text.
 */
public interface DocumentParser {

    /**
     * Parses raw document bytes into plain text.
     *
     * @param content  raw document bytes
     * @param mimeType MIME type hint (e.g. "application/pdf", "text/plain")
     * @return extracted plain text
     * @throws IngestionParseException if parsing fails
     */
    String parse(byte[] content, String mimeType);

    /**
     * Returns true if this parser can handle the given MIME type.
     */
    boolean supports(String mimeType);
}
