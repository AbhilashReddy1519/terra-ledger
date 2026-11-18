package com.terraledger.dto;

import jakarta.validation.constraints.NotBlank;

public class SubmitSignatureRequest {

    @NotBlank(message = "Signature is required")
    private String signature;

    public SubmitSignatureRequest() {}

    public SubmitSignatureRequest(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
