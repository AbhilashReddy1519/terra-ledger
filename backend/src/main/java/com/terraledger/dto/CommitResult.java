package com.terraledger.dto;

import java.math.BigInteger;

public record CommitResult(
        String txHash,
        int version,
        BigInteger blockNumber,
        String approver,
        String cid,
        String sha256Hex
) {}
