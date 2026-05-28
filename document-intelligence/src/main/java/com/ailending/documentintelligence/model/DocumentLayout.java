package com.ailending.documentintelligence.model;

import java.util.Collections;
import java.util.List;

public final class DocumentLayout {

    private final List<FormField> formFields;
    private final List<ExtractedTable> tables;

    public DocumentLayout(List<FormField> formFields, List<ExtractedTable> tables) {
        this.formFields = Collections.unmodifiableList(formFields);
        this.tables     = Collections.unmodifiableList(tables);
    }

    public List<FormField> getFormFields()     { return formFields; }
    public List<ExtractedTable> getTables()    { return tables; }

    public static DocumentLayout empty() {
        return new DocumentLayout(Collections.emptyList(), Collections.emptyList());
    }
}
