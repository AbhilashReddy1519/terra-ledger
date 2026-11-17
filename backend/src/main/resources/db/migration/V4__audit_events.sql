-- V4: Audit events for officer review and state changes

CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_entity_id ON audit_events(entity_id);
CREATE INDEX idx_audit_events_actor ON audit_events(actor);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
