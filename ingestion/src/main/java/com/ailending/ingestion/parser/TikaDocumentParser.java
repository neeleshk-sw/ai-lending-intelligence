package com.ailending.ingestion.parser;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class TikaDocumentParser implements DocumentParser {

    private static final int MAX_STRING_LENGTH = 10 * 1024 * 1024; // 10 MB text cap

    private final Tika tika = new Tika();

    public TikaDocumentParser() {
        tika.setMaxStringLength(MAX_STRING_LENGTH);
    }

    @Override
    public String parse(byte[] content, String mimeType) {
        try {
            return tika.parseToString(new ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new IngestionParseException("Failed to parse document: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return true; // Tika handles all common document formats
    }
}
