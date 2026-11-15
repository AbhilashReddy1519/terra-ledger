package com.terraledger.dto;

import java.time.Instant;

public class ChallengeResponse {
    private String nonce;
    private String message;
    private Instant expiresAt;

    public ChallengeResponse() {}

    public ChallengeResponse(String nonce, String message, Instant expiresAt) {
        this.nonce = nonce;
        this.message = message;
        this.expiresAt = expiresAt;
    }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
