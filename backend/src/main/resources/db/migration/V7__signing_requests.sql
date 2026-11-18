CREATE TABLE signing_requests (
    id VARCHAR(36) PRIMARY KEY,
    document VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    expected_signer VARCHAR(64) NOT NULL,
    nonce VARCHAR(64) NOT NULL UNIQUE,
    canonical_payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP(6) NOT NULL,
    signature VARCHAR(132),
    signer_address VARCHAR(64),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    signed_at TIMESTAMP(6) NULL,
    INDEX idx_sr_expected_signer (expected_signer),
    INDEX idx_sr_nonce (nonce),
    INDEX idx_sr_status (status)
);
