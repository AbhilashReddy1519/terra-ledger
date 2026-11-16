-- V3: Land Records and OCR Extractions

CREATE TABLE IF NOT EXISTS land_records (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    uploaded_by VARCHAR(36) NOT NULL,
    survey_no VARCHAR(100),
    owner_name VARCHAR(255),
    area VARCHAR(100),
    boundaries TEXT,
    deed_type VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    file_name VARCHAR(255) NOT NULL,
    file_content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    storage_path VARCHAR(500),
    current_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_land_records_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ocr_extractions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    record_id VARCHAR(36) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    extracted_value TEXT,
    corrected_value TEXT,
    confidence DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ocr_extractions_record FOREIGN KEY (record_id) REFERENCES land_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_land_records_uploaded_by ON land_records(uploaded_by);
CREATE INDEX idx_land_records_status ON land_records(status);
CREATE INDEX idx_ocr_extractions_record_id ON ocr_extractions(record_id);
