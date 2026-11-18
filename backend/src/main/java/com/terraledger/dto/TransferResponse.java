package com.terraledger.dto;

import com.terraledger.domain.TransferRequest;
import com.terraledger.domain.TransferRequestStatus;

import java.time.Instant;

public class TransferResponse {

    private String id;
    private String recordId;
    private String sellerUserId;
    private String sellerAddress;
    private String buyerName;
    private String buyerAddress;
    private String saleDeedCid;
    private String saleDeedSha256;
    private String signingRequestId;
    private TransferRequestStatus status;
    private String approvedBy;
    private String txHash;
    private Instant createdAt;
    private Instant updatedAt;

    public TransferResponse() {}

    public static TransferResponse from(TransferRequest entity) {
        TransferResponse resp = new TransferResponse();
        resp.setId(entity.getId());
        resp.setRecordId(entity.getRecordId());
        resp.setSellerUserId(entity.getSellerUserId());
        resp.setSellerAddress(entity.getSellerAddress());
        resp.setBuyerName(entity.getBuyerName());
        resp.setBuyerAddress(entity.getBuyerAddress());
        resp.setSaleDeedCid(entity.getSaleDeedCid());
        resp.setSaleDeedSha256(entity.getSaleDeedSha256());
        resp.setSigningRequestId(entity.getSigningRequestId());
        resp.setStatus(entity.getStatus());
        resp.setApprovedBy(entity.getApprovedBy());
        resp.setTxHash(entity.getTxHash());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

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
        this.sellerAddress = sellerAddress;
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
        this.buyerAddress = buyerAddress;
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
