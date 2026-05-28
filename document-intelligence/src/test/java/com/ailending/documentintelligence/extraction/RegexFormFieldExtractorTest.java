package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.FormField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegexFormFieldExtractorTest {

    private RegexFormFieldExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RegexFormFieldExtractor();
    }

    @Test
    void extractsColonSeparatedPair_withHighConfidence() {
        List<FormField> fields = extractor.extract("Monthly Income: 75000");

        assertEquals(1, fields.size());
        assertEquals("Monthly Income", fields.get(0).getFieldName());
        assertEquals("75000", fields.get(0).getRawValue());
        assertEquals(0.9, fields.get(0).getConfidence(), 0.001);
    }

    @Test
    void extractsDashSeparatedPair_withLowerConfidence() {
        List<FormField> fields = extractor.extract("Employer — Infosys Ltd");

        assertEquals(1, fields.size());
        assertEquals("Employer", fields.get(0).getFieldName());
        assertEquals("Infosys Ltd", fields.get(0).getRawValue());
        assertEquals(0.7, fields.get(0).getConfidence(), 0.001);
    }

    @Test
    void lineWithNoSeparator_isIgnored() {
        List<FormField> fields = extractor.extract("This is a plain sentence with no separator.");
        assertTrue(fields.isEmpty());
    }

    @Test
    void multiplePairsOnSeparateLines_allExtracted() {
        String text = "Applicant Name: Ravi Kumar\nMonthly Income: 50000\nLoan Amount: 500000";

        List<FormField> fields = extractor.extract(text);

        assertEquals(3, fields.size());
        assertEquals("Applicant Name", fields.get(0).getFieldName());
        assertEquals("Monthly Income", fields.get(1).getFieldName());
        assertEquals("Loan Amount",    fields.get(2).getFieldName());
    }

    @Test
    void blankText_returnsEmptyList() {
        assertTrue(extractor.extract("   ").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
    }

    @Test
    void mixedContent_onlyMatchingLinesExtracted() {
        String text = "LOAN APPLICATION FORM\n" +
                      "Applicant Name: Priya Sharma\n" +
                      "Some random text here.\n" +
                      "City: Mumbai\n";

        List<FormField> fields = extractor.extract(text);

        assertEquals(2, fields.size());
        assertEquals("Applicant Name", fields.get(0).getFieldName());
        assertEquals("City",           fields.get(1).getFieldName());
    }
}
