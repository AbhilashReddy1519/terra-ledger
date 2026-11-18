package com.terraledger.service;

import com.terraledger.dto.ChainRecord;
import com.terraledger.dto.VerificationResponse;
import com.terraledger.domain.RecordVersion;
import com.terraledger.repository.RecordVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final ContractGatewayService contractGatewayService;
    private final RecordVersionRepository recordVersionRepository;

    public VerificationService(ContractGatewayService contractGatewayService,
                               RecordVersionRepository recordVersionRepository) {
        this.contractGatewayService = contractGatewayService;
        this.recordVersionRepository = recordVersionRepository;
    }

    public VerificationResponse verifyFile(MultipartFile file, String recordId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Verification file must not be empty");
        }

        String computedHash = computeSha256(file);
        log.info("Verifying file: size={}, computedSha256={}, recordId={}",
                file.getSize(), computedHash, recordId);

        if (recordId != null && !recordId.trim().isEmpty()) {
            return verifyByRecordId(recordId.trim(), computedHash);
        } else {
            return verifyByHashLookup(computedHash);
        }
    }

    private VerificationResponse verifyByRecordId(String recordId, String computedHash) {
        ChainRecord chainRecord;
        try {
            chainRecord = contractGatewayService.getLatest(recordId);
        } catch (Exception e) {
            log.warn("Failed to query on-chain record {}: {}", recordId, e.getMessage());
            return VerificationResponse.notFound(recordId, computedHash,
                    "Failed to query blockchain record: " + e.getMessage());
        }

        if (chainRecord == null || chainRecord.version() <= 0) {
            return VerificationResponse.notFound(recordId, computedHash,
                    "No committed record found on the blockchain for record ID: " + recordId);
        }

        String onChainHash = chainRecord.sha256Hex().toLowerCase();
        if (computedHash.equalsIgnoreCase(onChainHash)) {
            log.info("Verification SUCCESS: recordId={} matches on-chain hash at version {}",
                    recordId, chainRecord.version());
            return VerificationResponse.match(
                    recordId,
                    chainRecord.version(),
                    computedHash,
                    chainRecord.cid(),
                    chainRecord.approver(),
                    chainRecord.committedAt()
            );
        } else {
            log.warn("Verification MISMATCH: recordId={} computed={} onChain={}",
                    recordId, computedHash, onChainHash);
            return VerificationResponse.mismatch(
                    recordId,
                    chainRecord.version(),
                    computedHash,
                    onChainHash,
                    chainRecord.cid(),
                    chainRecord.approver(),
                    chainRecord.committedAt()
            );
        }
    }

    private VerificationResponse verifyByHashLookup(String computedHash) {
        Optional<RecordVersion> cachedVersion =
                recordVersionRepository.findTopBySha256OrderByCommittedAtDesc(computedHash);

        if (cachedVersion.isEmpty()) {
            log.info("No record found matching SHA-256 hash {}", computedHash);
            return VerificationResponse.notFound(null, computedHash,
                    "No approved record matching this file was found on the blockchain.");
        }

        RecordVersion version = cachedVersion.get();
        String recordId = version.getRecordId();

        // Always verify directly against the blockchain truth
        ChainRecord chainRecord;
        try {
            chainRecord = contractGatewayService.getLatest(recordId);
        } catch (Exception e) {
            log.warn("Chain query error during hash verification for record {}: {}", recordId, e.getMessage());
            // Fall back to cached verification if gateway is temporarily unreachable
            return VerificationResponse.match(
                    recordId,
                    version.getVersion(),
                    computedHash,
                    version.getCid(),
                    version.getApprovedBy(),
                    version.getCommittedAt()
            );
        }

        if (chainRecord != null && chainRecord.sha256Hex().equalsIgnoreCase(computedHash)) {
            return VerificationResponse.match(
                    recordId,
                    chainRecord.version(),
                    computedHash,
                    chainRecord.cid(),
                    chainRecord.approver(),
                    chainRecord.committedAt()
            );
        } else {
            String onChainHash = chainRecord != null ? chainRecord.sha256Hex() : "null";
            return VerificationResponse.mismatch(
                    recordId,
                    chainRecord != null ? chainRecord.version() : version.getVersion(),
                    computedHash,
                    onChainHash,
                    chainRecord != null ? chainRecord.cid() : version.getCid(),
                    chainRecord != null ? chainRecord.approver() : version.getApprovedBy(),
                    chainRecord != null ? chainRecord.committedAt() : version.getCommittedAt()
            );
        }
    }

    public String computeSha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute SHA-256 hash of file", e);
        }
    }
}
