package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entity;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public AuditEvent(String actor, String action, String entity, String entityId, String detail) {
        this();
        this.actor = actor;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
