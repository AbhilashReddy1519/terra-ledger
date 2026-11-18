package com.terraledger.service;

import com.terraledger.domain.*;
import com.terraledger.dto.*;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.repository.RecordVersionRepository;
import com.terraledger.repository.TransferRequestRepository;
import com.terraledger.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRequestRepository transferRequestRepository;
    private final LandRecordRepository landRecordRepository;
    private final RecordVersionRepository recordVersionRepository;
    private final SigningRequestService signingRequestService;
    private final EncryptionService encryptionService;
    private final IpfsClientService ipfsClientService;
    private final ContractGatewayService contractGatewayService;
    private final AuditService auditService;

    public TransferService(TransferRequestRepository transferRequestRepository,
                           LandRecordRepository landRecordRepository,
                           RecordVersionRepository recordVersionRepository,
                           SigningRequestService signingRequestService,
                           EncryptionService encryptionService,
                           IpfsClientService ipfsClientService,
                           ContractGatewayService contractGatewayService,
                           AuditService auditService) {
        this.transferRequestRepository = transferRequestRepository;
        this.landRecordRepository = landRecordRepository;
        this.recordVersionRepository = recordVersionRepository;
        this.signingRequestService = signingRequestService;
        this.encryptionService = encryptionService;
        this.ipfsClientService = ipfsClientService;
        this.contractGatewayService = contractGatewayService;
        this.auditService = auditService;
    }

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest dto, UserPrincipal sellerUser) {
        LandRecord record = landRecordRepository.findById(dto.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Land record not found: " + dto.getRecordId()));

        if (record.getStatus() != RecordStatus.COMMITTED) {
            throw new IllegalStateException("Only COMMITTED records can be transferred. Current status: " + record.getStatus());
        }

        String sellerUserId = (sellerUser != null) ? sellerUser.getId() : "ANONYMOUS";
        String sellerEmail = (sellerUser != null) ? sellerUser.getEmail() : "anonymous@example.com";

        // 1. Create seller consent signing request
        String consentDocument = "TRANSFER-MUTATION: Record " + record.getId() +
                " (Survey " + record.getSurveyNo() + ") to " + dto.getBuyerName();

        CreateSigningRequest signingReqDto = new CreateSigningRequest(
                consentDocument,
                "TRANSFER_CONSENT",
                dto.getSellerAddress(),
                60
        );

        SigningRequestResponse signingReq = signingRequestService.createRequest(signingReqDto);

        // 2. Persist transfer request
        TransferRequest transfer = new TransferRequest(
                record.getId(),
                sellerUserId,
                dto.getSellerAddress(),
                dto.getBuyerName(),
                dto.getBuyerAddress(),
                signingReq.getId()
        );

        TransferRequest saved = transferRequestRepository.save(transfer);
        log.info("Created transfer request {} for record {} to buyer {} with signing request {}",
                saved.getId(), record.getId(), saved.getBuyerName(), signingReq.getId());

        auditService.logEvent(
                sellerEmail,
                "CREATE_TRANSFER_REQUEST",
                "TransferRequest",
                saved.getId(),
                "Initiated transfer of record " + record.getId() + " to " + saved.getBuyerName()
        );

        return TransferResponse.from(saved);
    }

    @Transactional
    public TransferResponse getTransfer(String id) {
        TransferRequest transfer = transferRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer request not found: " + id));

        // Check if consent has been signed
        if (transfer.getStatus() == TransferRequestStatus.AWAITING_SELLER_CONSENT) {
            SigningRequestResponse signingReq = signingRequestService.getRequest(transfer.getSigningRequestId());
            if (signingReq.getStatus() == SigningRequestStatus.COMPLETED) {
                transfer.setStatus(TransferRequestStatus.AWAITING_REGISTRAR_APPROVAL);
                transfer = transferRequestRepository.save(transfer);
            }
        }

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse approveTransfer(String transferId, ApproveTransferRequest approveDto, UserPrincipal registrarUser) {
        String registrarEmail = (registrarUser != null) ? registrarUser.getEmail() : "registrar@example.com";
        TransferRequest transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer request not found: " + transferId));

        // Check seller consent
        SigningRequestResponse signingReq = signingRequestService.getRequest(transfer.getSigningRequestId());
        if (signingReq.getStatus() != SigningRequestStatus.COMPLETED) {
            log.warn("Transfer {} approval refused: seller consent not signed (status={})",
                    transferId, signingReq.getStatus());
            throw new IllegalStateException("Transfer cannot be approved without seller consent signature.");
        }

        if (transfer.getStatus() == TransferRequestStatus.APPROVED) {
            throw new IllegalStateException("Transfer request has already been approved.");
        }

        LandRecord record = landRecordRepository.findById(transfer.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Land record not found: " + transfer.getRecordId()));

        int currentVer = record.getCurrentVersion() != null ? record.getCurrentVersion() : 1;
        int nextVer = currentVer + 1;

        // 1. Generate mutation transfer document payload
        String transferDocContent = "TERRA_LEDGER_LAND_TRANSFER_MUTATION_DEED\n" +
                "Record ID: " + record.getId() + "\n" +
                "Survey No: " + record.getSurveyNo() + "\n" +
                "Prior Owner: " + record.getOwnerName() + "\n" +
                "New Owner: " + transfer.getBuyerName() + "\n" +
                "Seller Consent Signature: " + signingReq.getSignature() + "\n" +
                "Seller Consent Signer: " + signingReq.getSignerAddress() + "\n" +
                "Registrar Approver: " + registrarEmail + "\n" +
                "Version: " + nextVer + "\n" +
                "Timestamp: " + Instant.now().toString();

        byte[] transferDocBytes = transferDocContent.getBytes(StandardCharsets.UTF_8);
        String sha256Hex = computeSha256(transferDocBytes);

        // 2. Encrypt with AES-256-GCM and upload to IPFS
        byte[] encryptedPayload = encryptionService.encrypt(transferDocBytes);
        String ipfsCid = ipfsClientService.upload(
                encryptedPayload,
                "mutation_transfer_v" + nextVer + ".enc"
        );

        // 3. Commit new version to LandRegistry smart contract on-chain
        CommitResult commitResult = contractGatewayService.commitRecord(
                record.getId(),
                ipfsCid,
                sha256Hex,
                registrarEmail
        );

        // 4. Record new version row in MySQL record_versions cache
        RecordVersion newVersion = new RecordVersion(
                record.getId(),
                nextVer,
                ipfsCid,
                sha256Hex,
                commitResult.txHash(),
                registrarEmail
        );
        recordVersionRepository.save(newVersion);

        // 5. Update LandRecord state
        record.setOwnerName(transfer.getBuyerName());
        record.setCurrentVersion(nextVer);
        record.setSha256Hash(sha256Hex);
        record.setIpfsCid(ipfsCid);
        landRecordRepository.save(record);

        // 6. Update TransferRequest state
        transfer.setStatus(TransferRequestStatus.APPROVED);
        transfer.setApprovedBy(registrarEmail);
        transfer.setTxHash(commitResult.txHash());
        transfer.setSaleDeedCid(ipfsCid);
        transfer.setSaleDeedSha256(sha256Hex);
        TransferRequest updated = transferRequestRepository.save(transfer);

        log.info("Transfer {} APPROVED by registrar {}. Record {} mutated to v{} (new owner: {}) tx={}",
                transferId, registrarEmail, record.getId(), nextVer, transfer.getBuyerName(), commitResult.txHash());

        auditService.logEvent(
                registrarEmail,
                "APPROVE_TRANSFER",
                "LandRecord",
                record.getId(),
                "Approved transfer to " + transfer.getBuyerName() + " v" + nextVer + " tx=" + commitResult.txHash()
        );

        return TransferResponse.from(updated);
    }

    public List<TransferResponse> getPendingRegistrarTransfers() {
        return transferRequestRepository.findByStatus(TransferRequestStatus.AWAITING_REGISTRAR_APPROVAL)
                .stream()
                .map(TransferResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransferResponse> getTransfersForRecord(String recordId) {
        return transferRequestRepository.findByRecordId(recordId)
                .stream()
                .map(TransferResponse::from)
                .collect(Collectors.toList());
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}
