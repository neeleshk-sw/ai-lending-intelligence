package com.ailending.documentintelligence.model;

public final class FormField {

    private final String fieldName;
    private final String rawValue;
    private final double confidence;

    public FormField(String fieldName, String rawValue, double confidence) {
        this.fieldName  = fieldName;
        this.rawValue   = rawValue;
        this.confidence = confidence;
    }

    public String getFieldName()  { return fieldName; }
    public String getRawValue()   { return rawValue; }
    public double getConfidence() { return confidence; }
}
