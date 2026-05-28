package com.ailending.documentintelligence.service;

import com.ailending.documentintelligence.exception.DocumentIntelligenceException;
import com.ailending.documentintelligence.extraction.FormFieldExtractor;
import com.ailending.documentintelligence.extraction.TableExtractor;
import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.documentintelligence.model.ExtractedTable;
import com.ailending.documentintelligence.model.FormField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DocumentIntelligenceServiceImplTest {

    private FormFieldExtractor formFieldExtractor;
    private TableExtractor tableExtractor;
    private DocumentIntelligenceService service;

    @BeforeEach
    void setUp() {
        formFieldExtractor = mock(FormFieldExtractor.class);
        tableExtractor     = mock(TableExtractor.class);
        service            = new DocumentIntelligenceServiceImpl(formFieldExtractor, tableExtractor);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void analyze_callsBothExtractors_andMergesResults() {
        FormField field = new FormField("Income", "75000", 0.9);
        ExtractedTable table = new ExtractedTable(
                List.of("Month", "Amount"),
                List.of(List.of("Jan", "75000")));

        when(formFieldExtractor.extract(anyString())).thenReturn(List.of(field));
        when(tableExtractor.extract(anyString())).thenReturn(List.of(table));

        DocumentLayout layout = service.analyze("some document text", "SALARY_SLIP");

        assertEquals(1, layout.getFormFields().size(), "FormFieldExtractor result should be merged");
        assertEquals(1, layout.getTables().size(), "TableExtractor result should be merged");
        assertEquals("Income", layout.getFormFields().get(0).getFieldName());
        verify(formFieldExtractor).extract("some document text");
        verify(tableExtractor).extract("some document text");
    }

    @Test
    void analyze_blankText_returnsEmptyLayout() {
        DocumentLayout layout = service.analyze("   ", "BANK_STATEMENT");

        assertTrue(layout.getFormFields().isEmpty());
        assertTrue(layout.getTables().isEmpty());
        verifyNoInteractions(formFieldExtractor, tableExtractor);
    }

    @Test
    void analyze_nullText_returnsEmptyLayout() {
        DocumentLayout layout = service.analyze(null, "LOAN_APPLICATION");

        assertTrue(layout.getFormFields().isEmpty());
        verifyNoInteractions(formFieldExtractor, tableExtractor);
    }

    @Test
    void analyze_extractorThrows_wrapsAsDocumentIntelligenceException() {
        when(formFieldExtractor.extract(anyString()))
                .thenThrow(new RuntimeException("parse failure"));

        assertThrows(DocumentIntelligenceException.class,
                () -> service.analyze("some text", "FORM_16"));
    }

    @Test
    void analyze_noFieldsNoTables_returnsEmptyLists() {
        when(formFieldExtractor.extract(anyString())).thenReturn(Collections.emptyList());
        when(tableExtractor.extract(anyString())).thenReturn(Collections.emptyList());

        DocumentLayout layout = service.analyze("plain text without structure", "ID_PROOF");

        assertTrue(layout.getFormFields().isEmpty());
        assertTrue(layout.getTables().isEmpty());
    }
}
