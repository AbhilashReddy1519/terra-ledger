package com.terraledger.service;

import com.terraledger.domain.SigningRequest;
import com.terraledger.domain.SigningRequestStatus;
import com.terraledger.dto.CreateSigningRequest;
import com.terraledger.dto.SigningRequestResponse;
import com.terraledger.repository.SigningRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SigningRequestService {

    private static final Logger log = LoggerFactory.getLogger(SigningRequestService.class);

    private final SigningRequestRepository signingRequestRepository;
    private final SignatureService signatureService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SigningRequestService(SigningRequestRepository signingRequestRepository,
                                 SignatureService signatureService,
                                 AuditService auditService) {
        this.signingRequestRepository = signingRequestRepository;
        this.signatureService = signatureService;
        this.auditService = auditService;
    }

    @Transactional
    public SigningRequestResponse createRequest(CreateSigningRequest dto) {
        String expectedSigner = dto.getExpectedSigner().trim().toLowerCase();
        if (!expectedSigner.startsWith("0x") || expectedSigner.length() != 42) {
            throw new IllegalArgumentException("Invalid Ethereum address format for expected signer: " + expectedSigner);
        }

        byte[] nonceBytes = new byte[32];
        secureRandom.nextBytes(nonceBytes);
        String nonce = HexFormat.of().formatHex(nonceBytes);

        Instant expiresAt;
        if (dto.getExpirySeconds() != null && dto.getExpirySeconds() > 0) {
            expiresAt = Instant.now().plus(Duration.ofSeconds(dto.getExpirySeconds()));
        } else {
            int expiryMin = (dto.getExpiryMinutes() != null && dto.getExpiryMinutes() > 0)
                    ? dto.getExpiryMinutes()
                    : 15;
            expiresAt = Instant.now().plus(Duration.ofMinutes(expiryMin));
        }

        String canonicalPayload = buildCanonicalPayload(
                dto.getAction().trim(),
                dto.getDocument().trim(),
                expectedSigner,
                nonce,
                expiresAt
        );

        SigningRequest request = new SigningRequest(
                dto.getDocument().trim(),
                dto.getAction().trim(),
                expectedSigner,
                nonce,
                canonicalPayload,
                expiresAt
        );

        SigningRequest saved = signingRequestRepository.save(request);
        log.info("Created signing request id={} action={} document={} expectedSigner={} expiresAt={}",
                saved.getId(), saved.getAction(), saved.getDocument(), saved.getExpectedSigner(), saved.getExpiresAt());

        return SigningRequestResponse.from(saved);
    }

    public static String buildCanonicalPayload(String action, String document, String expectedSigner,
                                              String nonce, Instant expiresAt) {
        return "TerraLedger Signing Request\n" +
                "Action: " + action + "\n" +
                "Document: " + document + "\n" +
                "Signer: " + expectedSigner.toLowerCase() + "\n" +
                "Nonce: " + nonce + "\n" +
                "Expires-At: " + expiresAt.toString();
    }

    @Transactional
    public SigningRequestResponse getRequest(String id) {
        SigningRequest request = signingRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Signing request not found: " + id));

        if (request.getStatus() == SigningRequestStatus.PENDING && request.isExpired()) {
            request.setStatus(SigningRequestStatus.EXPIRED);
            request = signingRequestRepository.save(request);
        }

        return SigningRequestResponse.from(request);
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class, IllegalStateException.class})
    public SigningRequestResponse submitSignature(String id, String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature must not be empty");
        }

        SigningRequest request = signingRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Signing request not found: " + id));

        // 1. Replay check
        if (request.getStatus() == SigningRequestStatus.COMPLETED) {
            log.warn("Replay attempt detected for signing request {}", id);
            throw new IllegalStateException("Replay attack detected: signing request has already been completed.");
        }
        if (request.getStatus() == SigningRequestStatus.REJECTED) {
            throw new IllegalStateException("Signing request has already been rejected.");
        }

        // 2. Expiry check
        if (request.isExpired() || request.getStatus() == SigningRequestStatus.EXPIRED) {
            request.setStatus(SigningRequestStatus.EXPIRED);
            signingRequestRepository.save(request);
            log.warn("Signature submitted for expired signing request {}", id);
            throw new IllegalArgumentException("Signing request has expired.");
        }

        // 3. EIP-191 signature verification & Wrong Signer check
        String recoveredSigner;
        try {
            recoveredSigner = signatureService.recoverAddress(request.getCanonicalPayload(), signature.trim());
        } catch (Exception e) {
            log.warn("Invalid signature submitted for signing request {}: {}", id, e.getMessage());
            throw new IllegalArgumentException("Invalid cryptographic signature: " + e.getMessage(), e);
        }

        if (!recoveredSigner.equalsIgnoreCase(request.getExpectedSigner())) {
            log.warn("Wrong signer for request {}: expected {} but recovered {}",
                    id, request.getExpectedSigner(), recoveredSigner);
            throw new IllegalArgumentException("Signature verification failed: expected signer " +
                    request.getExpectedSigner() + " but recovered " + recoveredSigner);
        }

        // 4. Mark completed and store signature
        request.setSignature(signature.trim());
        request.setSignerAddress(recoveredSigner);
        request.setSignedAt(Instant.now());
        request.setStatus(SigningRequestStatus.COMPLETED);

        SigningRequest completed = signingRequestRepository.save(request);
        log.info("Signing request {} successfully verified and completed by {}", id, recoveredSigner);

        auditService.logEvent(
                recoveredSigner,
                "SIGN_REQUEST",
                "SigningRequest",
                id,
                "Completed " + request.getAction() + " for " + request.getDocument()
        );

        return SigningRequestResponse.from(completed);
    }

    public List<SigningRequestResponse> getPendingRequestsForSigner(String signerAddress) {
        if (signerAddress == null || signerAddress.trim().isEmpty()) {
            return List.of();
        }
        return signingRequestRepository.findByExpectedSignerIgnoreCaseAndStatus(
                signerAddress.trim().toLowerCase(),
                SigningRequestStatus.PENDING
        ).stream()
         .filter(req -> !req.isExpired())
         .map(SigningRequestResponse::from)
         .collect(Collectors.toList());
    }
}
