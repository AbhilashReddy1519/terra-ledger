package com.terraledger.dto;

public class FieldDetail {
    private String fieldName;
    private String extractedValue;
    private String correctedValue;
    private Double confidence;

    public FieldDetail() {}

    public FieldDetail(String fieldName, String extractedValue, String correctedValue, Double confidence) {
        this.fieldName = fieldName;
        this.extractedValue = extractedValue;
        this.correctedValue = correctedValue;
        this.confidence = confidence;
    }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getExtractedValue() { return extractedValue; }
    public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }

    public String getCorrectedValue() { return correctedValue; }
    public void setCorrectedValue(String correctedValue) { this.correctedValue = correctedValue; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
