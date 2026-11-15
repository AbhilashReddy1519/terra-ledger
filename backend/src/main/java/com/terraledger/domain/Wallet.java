package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "linked_at")
    private Instant linkedAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public Wallet() {
        this.id = UUID.randomUUID().toString();
    }

    public Wallet(String userId, String address) {
        this();
        this.userId = userId;
        this.address = address.toLowerCase();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address != null ? address.toLowerCase() : null; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
