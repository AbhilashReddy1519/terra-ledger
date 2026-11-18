package com.terraledger.dto;

public class ApproveTransferRequest {

    private String remarks;

    public ApproveTransferRequest() {}

    public ApproveTransferRequest(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
