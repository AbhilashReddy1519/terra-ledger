-- M0: minimal schema so Flyway runs on first boot.
-- Real tables are added in M1-M5.

CREATE TABLE IF NOT EXISTS schema_info (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(10) NOT NULL,
    note    VARCHAR(255),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_info (version, note) VALUES ('M0', 'Walking skeleton baseline');
