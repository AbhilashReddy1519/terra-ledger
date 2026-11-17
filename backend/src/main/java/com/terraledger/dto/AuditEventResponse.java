package com.terraledger.dto;

import com.terraledger.domain.AuditEvent;

import java.time.Instant;

public class AuditEventResponse {
    private String id;
    private String actor;
    private String action;
    private String entity;
    private String entityId;
    private String detail;
    private Instant createdAt;

    public AuditEventResponse() {}

    public static AuditEventResponse fromEntity(AuditEvent event) {
        AuditEventResponse resp = new AuditEventResponse();
        resp.setId(event.getId());
        resp.setActor(event.getActor());
        resp.setAction(event.getAction());
        resp.setEntity(event.getEntity());
        resp.setEntityId(event.getEntityId());
        resp.setDetail(event.getDetail());
        resp.setCreatedAt(event.getCreatedAt());
        return resp;
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
