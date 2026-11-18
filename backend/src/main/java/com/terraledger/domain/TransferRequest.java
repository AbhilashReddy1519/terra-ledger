package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_requests")
public class TransferRequest {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "record_id", nullable = false, length = 36)
    private String recordId;

    @Column(name = "seller_user_id", nullable = false, length = 36)
    private String sellerUserId;

    @Column(name = "seller_address", nullable = false, length = 64)
    private String sellerAddress;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "buyer_address", length = 64)
    private String buyerAddress;

    @Column(name = "sale_deed_cid", length = 128)
    private String saleDeedCid;

    @Column(name = "sale_deed_sha256", length = 64)
    private String saleDeedSha256;

    @Column(name = "signing_request_id", nullable = false, length = 36)
    private String signingRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferRequestStatus status = TransferRequestStatus.AWAITING_SELLER_CONSENT;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TransferRequest() {
        this.id = UUID.randomUUID().toString();
    }

    public TransferRequest(String recordId, String sellerUserId, String sellerAddress,
                           String buyerName, String buyerAddress, String signingRequestId) {
        this.id = UUID.randomUUID().toString();
        this.recordId = recordId;
        this.sellerUserId = sellerUserId;
        this.sellerAddress = sellerAddress != null ? sellerAddress.toLowerCase() : null;
        this.buyerName = buyerName;
        this.buyerAddress = buyerAddress != null ? buyerAddress.toLowerCase() : null;
        this.signingRequestId = signingRequestId;
        this.status = TransferRequestStatus.AWAITING_SELLER_CONSENT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(String sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress != null ? sellerAddress.toLowerCase() : null;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress != null ? buyerAddress.toLowerCase() : null;
    }

    public String getSaleDeedCid() {
        return saleDeedCid;
    }

    public void setSaleDeedCid(String saleDeedCid) {
        this.saleDeedCid = saleDeedCid;
    }

    public String getSaleDeedSha256() {
        return saleDeedSha256;
    }

    public void setSaleDeedSha256(String saleDeedSha256) {
        this.saleDeedSha256 = saleDeedSha256;
    }

    public String getSigningRequestId() {
        return signingRequestId;
    }

    public void setSigningRequestId(String signingRequestId) {
        this.signingRequestId = signingRequestId;
    }

    public TransferRequestStatus getStatus() {
        return status;
    }

    public void setStatus(TransferRequestStatus status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
