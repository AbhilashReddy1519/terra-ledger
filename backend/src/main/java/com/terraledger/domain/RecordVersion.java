package com.terraledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "record_versions")
public class RecordVersion {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "record_id", length = 36, nullable = false)
    private String recordId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "cid", length = 100, nullable = false)
    private String cid;

    @Column(name = "sha256", length = 64, nullable = false)
    private String sha256;

    @Column(name = "tx_hash", length = 66, nullable = false)
    private String txHash;

    @Column(name = "approved_by", length = 255, nullable = false)
    private String approvedBy;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt = Instant.now();

    public RecordVersion() {
        this.id = UUID.randomUUID().toString();
    }

    public RecordVersion(String recordId, Integer version, String cid, String sha256, String txHash, String approvedBy) {
        this();
        this.recordId = recordId;
        this.version = version;
        this.cid = cid;
        this.sha256 = sha256;
        this.txHash = txHash;
        this.approvedBy = approvedBy;
        this.committedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getCommittedAt() { return committedAt; }
    public void setCommittedAt(Instant committedAt) { this.committedAt = committedAt; }
}
