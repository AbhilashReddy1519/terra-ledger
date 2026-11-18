package com.terraledger.domain;

public enum TransferRequestStatus {
    AWAITING_SELLER_CONSENT,
    AWAITING_REGISTRAR_APPROVAL,
    APPROVED,
    REJECTED
}
