package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signing_requests")
public class SigningRequest {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String document;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "expected_signer", nullable = false, length = 64)
    private String expectedSigner;

    @Column(nullable = false, unique = true, length = 64)
    private String nonce;

    @Lob
    @Column(name = "canonical_payload", nullable = false, columnDefinition = "TEXT")
    private String canonicalPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SigningRequestStatus status = SigningRequestStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(length = 132)
    private String signature;

    @Column(name = "signer_address", length = 64)
    private String signerAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "signed_at")
    private Instant signedAt;

    public SigningRequest() {
        this.id = UUID.randomUUID().toString();
    }

    public SigningRequest(String document, String action, String expectedSigner,
                          String nonce, String canonicalPayload, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.document = document;
        this.action = action;
        this.expectedSigner = expectedSigner != null ? expectedSigner.toLowerCase() : null;
        this.nonce = nonce;
        this.canonicalPayload = canonicalPayload;
        this.expiresAt = expiresAt;
        this.status = SigningRequestStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Getters and Setters

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
        this.expectedSigner = expectedSigner != null ? expectedSigner.toLowerCase() : null;
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
        if (status == SigningRequestStatus.PENDING && isExpired()) {
            return SigningRequestStatus.EXPIRED;
        }
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
