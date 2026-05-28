package com.ailending.documentintelligence.service;

import com.ailending.documentintelligence.exception.DocumentIntelligenceException;
import com.ailending.documentintelligence.extraction.FormFieldExtractor;
import com.ailending.documentintelligence.extraction.TableExtractor;
import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.documentintelligence.model.ExtractedTable;
import com.ailending.documentintelligence.model.FormField;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIntelligenceServiceImpl implements DocumentIntelligenceService {

    private final FormFieldExtractor formFieldExtractor;
    private final TableExtractor tableExtractor;

    public DocumentIntelligenceServiceImpl(FormFieldExtractor formFieldExtractor,
                                           TableExtractor tableExtractor) {
        this.formFieldExtractor = formFieldExtractor;
        this.tableExtractor     = tableExtractor;
    }

    @Override
    public DocumentLayout analyze(String parsedText, String documentType) {
        if (parsedText == null || parsedText.isBlank()) {
            return DocumentLayout.empty();
        }

        try {
            List<FormField> fields = formFieldExtractor.extract(parsedText);
            List<ExtractedTable> tables = tableExtractor.extract(parsedText);
            return new DocumentLayout(fields, tables);
        } catch (Exception e) {
            throw new DocumentIntelligenceException(
                    "Failed to analyze document of type " + documentType + ": " + e.getMessage(), e);
        }
    }
}
