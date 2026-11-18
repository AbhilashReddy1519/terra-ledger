package com.terraledger.dto;

import java.math.BigInteger;

public record ChainRecordEvent(
        String recordIdHex,
        int version,
        String sha256Hex,
        String cid,
        String approver,
        String txHash,
        BigInteger blockNumber
) {}
