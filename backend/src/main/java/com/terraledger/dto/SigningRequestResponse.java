package com.terraledger.dto;

import com.terraledger.domain.SigningRequest;
import com.terraledger.domain.SigningRequestStatus;

import java.time.Instant;

public class SigningRequestResponse {

    private String id;
    private String document;
    private String action;
    private String expectedSigner;
    private String nonce;
    private String canonicalPayload;
    private SigningRequestStatus status;
    private Instant expiresAt;
    private String signature;
    private String signerAddress;
    private Instant createdAt;
    private Instant signedAt;

    public SigningRequestResponse() {}

    public static SigningRequestResponse from(SigningRequest req) {
        SigningRequestResponse resp = new SigningRequestResponse();
        resp.setId(req.getId());
        resp.setDocument(req.getDocument());
        resp.setAction(req.getAction());
        resp.setExpectedSigner(req.getExpectedSigner());
        resp.setNonce(req.getNonce());
        resp.setCanonicalPayload(req.getCanonicalPayload());
        resp.setStatus(req.getStatus());
        resp.setExpiresAt(req.getExpiresAt());
        resp.setSignature(req.getSignature());
        resp.setSignerAddress(req.getSignerAddress());
        resp.setCreatedAt(req.getCreatedAt());
        resp.setSignedAt(req.getSignedAt());
        return resp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCanonicalPayload() {
        return canonicalPayload;
    }

    public void setCanonicalPayload(String canonicalPayload) {
        this.canonicalPayload = canonicalPayload;
    }

    public SigningRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SigningRequestStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignerAddress() {
        return signerAddress;
    }

    public void setSignerAddress(String signerAddress) {
        this.signerAddress = signerAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }
}
