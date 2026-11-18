package com.terraledger.dto;

import java.time.Instant;

public record ChainRecord(
        int version,
        String cid,
        String sha256Hex,
        String approver,
        Instant committedAt
) {}
