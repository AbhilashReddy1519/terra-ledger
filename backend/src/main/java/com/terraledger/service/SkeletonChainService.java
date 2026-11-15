package com.terraledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M0 skeleton: writes and reads one hardcoded record via Web3j.
 * The LandRegistry contract address is read from the deployments/localhost.json
 * file written by the Hardhat deploy script (via volume mount in Docker).
 * <p>
 * In production all contract interactions move to a dedicated ContractGateway
 * component with proper error handling, retry, and nonce management.
 */
@Service
public class SkeletonChainService {

    private static final Logger log = LoggerFactory.getLogger(SkeletonChainService.class);

    // Hardcoded skeleton record values (M0 only — no DB, no IPFS)
    private static final String SKELETON_RECORD_ID = "skeleton-record-001";
    private static final String SKELETON_CID       =
            "bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi";

    @Value("${app.hardhat.rpc-url}")
    private String rpcUrl;

    @Value("${app.hardhat.private-key}")
    private String privateKey;

    @Value("${LAND_REGISTRY_ADDRESS:}")
    private String contractAddress;

    private Web3j web3j;
    private Credentials credentials;

    @PostConstruct
    void init() {
        web3j       = Web3j.build(new HttpService(rpcUrl));
        credentials = Credentials.create(privateKey);
        log.info("SkeletonChainService connected to {} as {}",
                rpcUrl, credentials.getAddress());

        if (contractAddress == null || contractAddress.isBlank()) {
            java.io.File file = new java.io.File("/deployments/localhost.json");
            if (!file.exists()) {
                file = new java.io.File("contracts/deployments/localhost.json");
            }
            if (!file.exists()) {
                file = new java.io.File("../contracts/deployments/localhost.json");
            }
            if (file.exists()) {
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"address\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                    if (m.find()) {
                        contractAddress = m.group(1);
                        log.info("Loaded LandRegistry address from {}: {}", file.getPath(), contractAddress);
                    }
                } catch (Exception e) {
                    log.warn("Failed to read deployments file: {}", e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> commitHardcodedRecord() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", SKELETON_RECORD_ID);
        result.put("cid",      SKELETON_CID);
        result.put("sha256",   sha256Hex(SKELETON_RECORD_ID));
        result.put("submittedAt", Instant.now().toString());
        result.put("note", "M0 skeleton — chain write requires contract deployed via Hardhat");

        if (contractAddress == null || contractAddress.isBlank()) {
            result.put("chainStatus", "skipped — LAND_REGISTRY_ADDRESS not set");
            return result;
        }

        try {
            // Dynamically load contract wrapper (generated in M5; here we use raw call)
            result.put("chainStatus", "address=" + contractAddress + " (write deferred to M5)");
        } catch (Exception e) {
            log.warn("Chain write failed: {}", e.getMessage());
            result.put("chainStatus", "error: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> readRecord(String recordId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", recordId);
        result.put("readAt", Instant.now().toString());

        if (contractAddress == null || contractAddress.isBlank()) {
            result.put("chainStatus", "skipped — LAND_REGISTRY_ADDRESS not set");
            result.put("data", Map.of(
                    "version", 1,
                    "cid",     SKELETON_CID,
                    "sha256",  sha256Hex(SKELETON_RECORD_ID),
                    "approver","0x0000000000000000000000000000000000000000",
                    "note",    "M0 mock — real read in M5"
            ));
            return result;
        }

        result.put("chainStatus", "read deferred to M5 when Web3j wrappers are generated");
        return result;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
