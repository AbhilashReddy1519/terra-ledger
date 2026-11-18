package com.terraledger.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateSigningRequest {

    @NotBlank(message = "Document reference or ID must not be blank")
    private String document;

    @NotBlank(message = "Action must not be blank")
    private String action;

    @NotBlank(message = "Expected signer address must not be blank")
    private String expectedSigner;

    private Integer expiryMinutes;
    private Integer expirySeconds;

    public CreateSigningRequest() {}

    public CreateSigningRequest(String document, String action, String expectedSigner, Integer expiryMinutes) {
        this.document = document;
        this.action = action;
        this.expectedSigner = expectedSigner;
        this.expiryMinutes = expiryMinutes;
    }

    public Integer getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(Integer expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedSigner() {
        return expectedSigner;
    }

    public void setExpectedSigner(String expectedSigner) {
        this.expectedSigner = expectedSigner;
    }

    public Integer getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(Integer expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
