package com.terraledger.dto;

public class ReviewDecisionRequest {
    private String remarks;

    public ReviewDecisionRequest() {}

    public ReviewDecisionRequest(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
