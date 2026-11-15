package com.terraledger.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyChallengeRequest {

    @NotBlank
    private String nonce;

    @NotBlank
    private String signature; // hex-encoded EIP-191 signature (0x...)

    public VerifyChallengeRequest() {}

    public VerifyChallengeRequest(String nonce, String signature) {
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
