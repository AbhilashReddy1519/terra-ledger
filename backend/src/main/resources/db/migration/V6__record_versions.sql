-- V6: Add record_versions table for blockchain commit history and MySQL cache

CREATE TABLE IF NOT EXISTS record_versions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    record_id VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    cid VARCHAR(100) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    approved_by VARCHAR(255) NOT NULL,
    committed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_record_versions_record FOREIGN KEY (record_id) REFERENCES land_records(id) ON DELETE CASCADE,
    CONSTRAINT uq_record_version UNIQUE (record_id, version)
);

CREATE INDEX idx_record_versions_record_id ON record_versions(record_id);
CREATE INDEX idx_record_versions_tx_hash ON record_versions(tx_hash);
