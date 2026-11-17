-- V5: Add IPFS CID to land_records

ALTER TABLE land_records ADD COLUMN ipfs_cid VARCHAR(100) NULL AFTER storage_path;

CREATE INDEX idx_land_records_ipfs_cid ON land_records(ipfs_cid);
