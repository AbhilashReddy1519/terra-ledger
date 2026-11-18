package com.terraledger.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTransferRequest {

    @NotBlank(message = "Record ID is required")
    private String recordId;

    @NotBlank(message = "Buyer name is required")
    private String buyerName;

    private String buyerAddress;

    @NotBlank(message = "Seller wallet address is required")
    private String sellerAddress;

    public CreateTransferRequest() {}

    public CreateTransferRequest(String recordId, String buyerName, String buyerAddress, String sellerAddress) {
        this.recordId = recordId;
        this.buyerName = buyerName;
        this.buyerAddress = buyerAddress;
        this.sellerAddress = sellerAddress;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
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

    public String getSellerAddress() {
        return sellerAddress;
    }

    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }
}
