package com.terraledger.dto;

public record DecryptedFile(
        String filename,
        String contentType,
        byte[] data,
        String sha256Hash
) {}
