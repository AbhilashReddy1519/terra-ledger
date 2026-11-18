package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.RecordStatus;
import com.terraledger.domain.Role;
import com.terraledger.domain.TransferRequestStatus;
import com.terraledger.domain.User;
import com.terraledger.dto.*;
import com.terraledger.repository.*;
import com.terraledger.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MutationAndTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LandRecordRepository landRecordRepository;

    @Autowired
    private RecordVersionRepository recordVersionRepository;

    @Autowired
    private SigningRequestRepository signingRequestRepository;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;
    private String officerToken;
    private String registrarToken;
    private ECKeyPair citizenKeyPair;
    private String citizenAddress;

    @BeforeEach
    void setUp() throws Exception {
        transferRequestRepository.deleteAll();
        signingRequestRepository.deleteAll();
        recordVersionRepository.deleteAll();
        auditEventRepository.deleteAll();
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        citizenKeyPair = Keys.createEcKeyPair();
        citizenAddress = "0x" + Keys.getAddress(citizenKeyPair).toLowerCase();

        User citizen = new User("alice_citizen@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizen = userRepository.save(citizen);
        citizenToken = jwtTokenProvider.generateToken(citizen);

        User officer = new User("officer_m8@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
        officer = userRepository.save(officer);
        officerToken = jwtTokenProvider.generateToken(officer);

        User registrar = new User("registrar_m8@example.com", passwordEncoder.encode("password123"), Role.REGISTRAR);
        registrar = userRepository.save(registrar);
        registrarToken = jwtTokenProvider.generateToken(registrar);
    }

    private byte[] loadSampleDeedBytes() throws Exception {
        File sampleDir = new File("../sample-data");
        if (!sampleDir.exists()) sampleDir = new File("sample-data");
        File deedFile = new File(sampleDir, "record_01_sale_deed.png");
        if (deedFile.exists()) return Files.readAllBytes(deedFile.toPath());
        return "DEED_FOR_MUTATION_TRANSFER_TESTING".getBytes();
    }

    private RecordResponse uploadAndCommitInitialRecord() throws Exception {
        byte[] deedBytes = loadSampleDeedBytes();
        MockMultipartFile file = new MockMultipartFile("file", "deed.png", "image/png", deedBytes);

        String uploadJson = mockMvc.perform(multipart("/api/records")
                        .file(file)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        RecordResponse created = objectMapper.readValue(uploadJson, RecordResponse.class);

        ReviewDecisionRequest decision = new ReviewDecisionRequest("Approved initial deed v1");
        String approveJson = mockMvc.perform(post("/api/records/" + created.getId() + "/approve")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(approveJson, RecordResponse.class);
    }

    private String signPersonalMessage(String message, ECKeyPair keyPair) {
        Sign.SignatureData signatureData = Sign.signPrefixedMessage(
                message.getBytes(StandardCharsets.UTF_8),
                keyPair
        );
        byte[] sigBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, sigBytes, 32, 32);
        sigBytes[64] = signatureData.getV()[0];
        return Numeric.toHexString(sigBytes);
    }

    @Test
    @DisplayName("Full Transfer Flow: Seller consent + Registrar approval appends v2; old versions stay readable")
    void testTransferFullWorkflowWithConsentAndRegistrarApproval() throws Exception {
        // 1. Initial record committed at v1
        RecordResponse initialRecord = uploadAndCommitInitialRecord();
        assertThat(initialRecord.getStatus()).isEqualTo(RecordStatus.COMMITTED);
        assertThat(initialRecord.getCurrentVersion()).isEqualTo(1);
        String v1Sha256 = initialRecord.getSha256Hash();
        String v1Cid = initialRecord.getIpfsCid();

        // 2. Initiate Transfer to buyer "Bob Singh"
        CreateTransferRequest transferDto = new CreateTransferRequest(
                initialRecord.getId(),
                "Bob Singh",
                "0x1111111111111111111111111111111111111111",
                citizenAddress
        );

        String createTransferJson = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TransferResponse transfer = objectMapper.readValue(createTransferJson, TransferResponse.class);
        assertThat(transfer.getStatus()).isEqualTo(TransferRequestStatus.AWAITING_SELLER_CONSENT);
        assertThat(transfer.getSigningRequestId()).isNotNull();

        // 3. Registrar attempts to approve BEFORE seller consent -> must be refused
        mockMvc.perform(post("/api/registrar/transfers/" + transfer.getId() + "/approve")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 4. Seller Alice signs consent using EIP-191 personal_sign
        String signingReqJson = mockMvc.perform(get("/api/signing/requests/" + transfer.getSigningRequestId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse signingReq = objectMapper.readValue(signingReqJson, SigningRequestResponse.class);
        String consentSignature = signPersonalMessage(signingReq.getCanonicalPayload(), citizenKeyPair);

        SubmitSignatureRequest submitSigDto = new SubmitSignatureRequest(consentSignature);
        mockMvc.perform(post("/api/signing/requests/" + signingReq.getId() + "/submit")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitSigDto)))
                .andExpect(status().isOk());

        // 5. Transfer status advances to AWAITING_REGISTRAR_APPROVAL
        String transferAfterConsentJson = mockMvc.perform(get("/api/transfers/" + transfer.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TransferResponse transferAfterConsent = objectMapper.readValue(transferAfterConsentJson, TransferResponse.class);
        assertThat(transferAfterConsent.getStatus()).isEqualTo(TransferRequestStatus.AWAITING_REGISTRAR_APPROVAL);

        // 6. Registrar approves transfer -> appends v2 to chain and database
        ApproveTransferRequest approveDto = new ApproveTransferRequest("Approved and deed registered in registry");
        String approveTransferJson = mockMvc.perform(post("/api/registrar/transfers/" + transfer.getId() + "/approve")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TransferResponse approvedTransfer = objectMapper.readValue(approveTransferJson, TransferResponse.class);
        assertThat(approvedTransfer.getStatus()).isEqualTo(TransferRequestStatus.APPROVED);
        assertThat(approvedTransfer.getTxHash()).isNotNull();

        // 7. Check Land Record state: owner updated to Bob Singh, version is 2
        String updatedRecordJson = mockMvc.perform(get("/api/records/" + initialRecord.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        RecordResponse updatedRecord = objectMapper.readValue(updatedRecordJson, RecordResponse.class);
        assertThat(updatedRecord.getOwnerName()).isEqualTo("Bob Singh");
        assertThat(updatedRecord.getCurrentVersion()).isEqualTo(2);

        // 8. Verify OLD VERSIONS STAY READABLE:
        // Database versions list
        String versionsListJson = mockMvc.perform(get("/api/records/" + initialRecord.getId() + "/versions")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<?> versionsList = objectMapper.readValue(versionsListJson, List.class);
        assertThat(versionsList).hasSize(2);

        // Historical Version 1 still readable with original values
        String v1Json = mockMvc.perform(get("/api/records/" + initialRecord.getId() + "/versions/1")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(v1Json).contains(v1Sha256);
        assertThat(v1Json).contains(v1Cid);

        // New Version 2 readable with mutated values
        String v2Json = mockMvc.perform(get("/api/records/" + initialRecord.getId() + "/versions/2")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(v2Json).contains(approvedTransfer.getSaleDeedSha256());
        assertThat(v2Json).contains(approvedTransfer.getSaleDeedCid());

        // On-chain historical Version 1 still readable
        String onChainV1Json = mockMvc.perform(get("/api/records/" + initialRecord.getId() + "/chain/versions/1")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ChainRecord onChainV1 = objectMapper.readValue(onChainV1Json, ChainRecord.class);
        assertThat(onChainV1.version()).isEqualTo(1);
        assertThat(onChainV1.sha256Hex()).isEqualTo(v1Sha256);
        assertThat(onChainV1.cid()).isEqualTo(v1Cid);
    }

    @Test
    @DisplayName("Non-registrar cannot approve a transfer (refused with 403 Forbidden)")
    void testTransferRefusalByNonRegistrar() throws Exception {
        RecordResponse initialRecord = uploadAndCommitInitialRecord();

        CreateTransferRequest transferDto = new CreateTransferRequest(
                initialRecord.getId(),
                "Bob Singh",
                "0x1111111111111111111111111111111111111111",
                citizenAddress
        );

        String createTransferJson = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TransferResponse transfer = objectMapper.readValue(createTransferJson, TransferResponse.class);

        // Citizen attempts to approve -> 403 Forbidden
        mockMvc.perform(post("/api/registrar/transfers/" + transfer.getId() + "/approve")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Transfer approval without seller consent is refused with 400 Bad Request")
    void testTransferRefusalWithoutSellerConsent() throws Exception {
        RecordResponse initialRecord = uploadAndCommitInitialRecord();

        CreateTransferRequest transferDto = new CreateTransferRequest(
                initialRecord.getId(),
                "Bob Singh",
                "0x1111111111111111111111111111111111111111",
                citizenAddress
        );

        String createTransferJson = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TransferResponse transfer = objectMapper.readValue(createTransferJson, TransferResponse.class);

        // Registrar attempts to approve without Alice signing -> 400 Bad Request
        mockMvc.perform(post("/api/registrar/transfers/" + transfer.getId() + "/approve")
                        .header("Authorization", "Bearer " + registrarToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
