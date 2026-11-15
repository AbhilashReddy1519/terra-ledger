package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_challenges")
public class AuthChallenge {

    @Id
    @Column(length = 64)
    private String nonce;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public AuthChallenge() {}

    public AuthChallenge(String nonce, String userId, String message, Instant expiresAt) {
        this.nonce = nonce;
        this.userId = userId;
        this.message = message;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
