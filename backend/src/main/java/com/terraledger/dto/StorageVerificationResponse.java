package com.terraledger.dto;

public class StorageVerificationResponse {
    private String recordId;
    private String ipfsCid;
    private String sha256Hash;
    private String decryptedHash;
    private boolean verified;
    private long fileSize;

    public StorageVerificationResponse() {}

    public StorageVerificationResponse(String recordId, String ipfsCid, String sha256Hash, String decryptedHash, boolean verified, long fileSize) {
        this.recordId = recordId;
        this.ipfsCid = ipfsCid;
        this.sha256Hash = sha256Hash;
        this.decryptedHash = decryptedHash;
        this.verified = verified;
        this.fileSize = fileSize;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getIpfsCid() { return ipfsCid; }
    public void setIpfsCid(String ipfsCid) { this.ipfsCid = ipfsCid; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public String getDecryptedHash() { return decryptedHash; }
    public void setDecryptedHash(String decryptedHash) { this.decryptedHash = decryptedHash; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
