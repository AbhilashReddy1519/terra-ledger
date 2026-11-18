package com.terraledger.dto;

import java.time.Instant;

public class VerificationResponse {

    private boolean match;
    private String status; // "MATCH", "MISMATCH", "NOT_FOUND"
    private String message;
    private String recordId;
    private Integer version;
    private String computedHash;
    private String onChainHash;
    private String cid;
    private String approver;
    private Instant committedAt;
    private String disclaimer;

    public VerificationResponse() {
    }

    public VerificationResponse(boolean match, String status, String message, String recordId,
                                Integer version, String computedHash, String onChainHash,
                                String cid, String approver, Instant committedAt, String disclaimer) {
        this.match = match;
        this.status = status;
        this.message = message;
        this.recordId = recordId;
        this.version = version;
        this.computedHash = computedHash;
        this.onChainHash = onChainHash;
        this.cid = cid;
        this.approver = approver;
        this.committedAt = committedAt;
        this.disclaimer = disclaimer;
    }

    public static VerificationResponse match(String recordId, Integer version, String computedHash,
                                             String cid, String approver, Instant committedAt) {
        return new VerificationResponse(
                true,
                "MATCH",
                "Identical to the approved file",
                recordId,
                version,
                computedHash,
                computedHash,
                cid,
                approver,
                committedAt,
                "A match confirms that this digital file is bit-for-bit identical to the document approved by the revenue officer and anchored to the blockchain. A match does not prove that the original paper document or OCR data was accurate, and does not constitute a title guarantee or legal ownership."
        );
    }

    public static VerificationResponse mismatch(String recordId, Integer version, String computedHash,
                                                String onChainHash, String cid, String approver, Instant committedAt) {
        return new VerificationResponse(
                false,
                "MISMATCH",
                "Different from the approved file",
                recordId,
                version,
                computedHash,
                onChainHash,
                cid,
                approver,
                committedAt,
                "A match proves the digital document has not been altered since officer approval. It does not prove the original paper document or OCR data was accurate, and does not constitute a title guarantee."
        );
    }

    public static VerificationResponse notFound(String recordId, String computedHash, String message) {
        return new VerificationResponse(
                false,
                "NOT_FOUND",
                message,
                recordId,
                null,
                computedHash,
                null,
                null,
                null,
                null,
                "No approved record matching this file was found on the blockchain."
        );
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getComputedHash() {
        return computedHash;
    }

    public void setComputedHash(String computedHash) {
        this.computedHash = computedHash;
    }

    public String getOnChainHash() {
        return onChainHash;
    }

    public void setOnChainHash(String onChainHash) {
        this.onChainHash = onChainHash;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
