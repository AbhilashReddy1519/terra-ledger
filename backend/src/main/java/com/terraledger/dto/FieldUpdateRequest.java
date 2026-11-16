package com.terraledger.dto;

import java.util.HashMap;
import java.util.Map;

public class FieldUpdateRequest {
    private Map<String, String> fields = new HashMap<>();

    public FieldUpdateRequest() {}

    public FieldUpdateRequest(Map<String, String> fields) {
        this.fields = fields;
    }

    public Map<String, String> getFields() { return fields; }
    public void setFields(Map<String, String> fields) { this.fields = fields; }
}
