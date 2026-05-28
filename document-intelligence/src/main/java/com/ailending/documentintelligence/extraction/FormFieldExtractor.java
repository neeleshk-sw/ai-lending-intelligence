package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.FormField;

import java.util.List;

public interface FormFieldExtractor {

    List<FormField> extract(String text);
}
