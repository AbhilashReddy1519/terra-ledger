package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.LandRecord;
import com.terraledger.domain.RecordStatus;
import com.terraledger.domain.RecordVersion;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.dto.ChainRecord;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReconcileReport;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.repository.RecordVersionRepository;
import com.terraledger.repository.UserRepository;
import com.terraledger.security.JwtTokenProvider;
import com.terraledger.service.ContractGatewayService;
import com.terraledger.service.ReconcileService;
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
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ContractAndCommitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LandRecordRepository landRecordRepository;

    @Autowired
    private RecordVersionRepository recordVersionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractGatewayService contractGatewayService;

    @Autowired
    private ReconcileService reconcileService;

    private String citizenToken;
    private String officerToken;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        recordVersionRepository.deleteAll();
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        citizenUser = new User("citizen_m5@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizenUser = userRepository.save(citizenUser);
        citizenToken = jwtTokenProvider.generateToken(citizenUser);

        User officerUser = new User("officer_m5@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
        officerUser = userRepository.save(officerUser);
        officerToken = jwtTokenProvider.generateToken(officerUser);
    }

    private byte[] loadSampleDeedBytes() throws Exception {
        File sampleDir = new File("../sample-data");
        if (!sampleDir.exists()) {
            sampleDir = new File("sample-data");
        }
        File deedFile = new File(sampleDir, "record_01_sale_deed.png");
        if (deedFile.exists()) {
            return Files.readAllBytes(deedFile.toPath());
        }
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
                0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00,
                0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
                0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    @Test
    @DisplayName("Done when 1: An approved record commits to contract, updates MySQL cache, and chain matches")
    void testApprovedRecordCommitsToContractAndUpdatesMySQL() throws Exception {
        byte[] deedBytes = loadSampleDeedBytes();
        MockMultipartFile deedFile = new MockMultipartFile("file", "record_m5.png", "image/png", deedBytes);

        // 1. Citizen uploads deed
        String uploadJson = mockMvc.perform(multipart("/api/records")
                        .file(deedFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        RecordResponse uploaded = objectMapper.readValue(uploadJson, RecordResponse.class);
        String recordId = uploaded.getId();
        assertThat(recordId).isNotBlank();

        // 2. Officer approves record -> triggers IPFS encryption AND contract commit
        ReviewDecisionRequest approveReq = new ReviewDecisionRequest("All physical boundary checks verified and approved.");
        String approveJson = mockMvc.perform(post("/api/records/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecordResponse approved = objectMapper.readValue(approveJson, RecordResponse.class);
        assertThat(approved.getStatus()).isEqualTo(RecordStatus.COMMITTED);
        assertThat(approved.getCurrentVersion()).isEqualTo(1);
        assertThat(approved.getTxHash()).isNotBlank().startsWith("0x");
        assertThat(approved.getIpfsCid()).isNotBlank();

        // 3. Verify MySQL cache (record_versions table)
        Optional<RecordVersion> rvOpt = recordVersionRepository.findByRecordIdAndVersion(recordId, 1);
        assertThat(rvOpt).isPresent();
        RecordVersion rv = rvOpt.get();
        assertThat(rv.getVersion()).isEqualTo(1);
        assertThat(rv.getCid()).isEqualTo(approved.getIpfsCid());
        assertThat(rv.getSha256()).isEqualTo(approved.getSha256Hash());
        assertThat(rv.getTxHash()).isEqualTo(approved.getTxHash());

        // 4. Query smart contract directly via getLatest
        ChainRecord chainRecord = contractGatewayService.getLatest(recordId);
        assertThat(chainRecord.version()).isEqualTo(1);
        assertThat(chainRecord.cid()).isEqualTo(approved.getIpfsCid());
        assertThat(chainRecord.sha256Hex()).isEqualTo(approved.getSha256Hash());
    }

    @Test
    @DisplayName("Done when 2: A non-officer transaction reverts with NotOfficer")
    void testNonOfficerTransactionReverts() throws Exception {
        // Hardhat Account #2 (pre-funded with 10,000 ETH, NOT an authorized officer)
        String nonOfficerPrivateKey = "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a";
        Credentials unauthorizedCredentials = Credentials.create(nonOfficerPrivateKey);

        String testRecordId = "unauthorized-record-" + System.currentTimeMillis();
        String testCid = "QmUnauthorizedCid123456789";
        String testSha256 = "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff";

        // Must fail with revert / NotOfficer
        Exception ex = assertThrows(RuntimeException.class, () -> {
            contractGatewayService.commitWithCredentials(
                    unauthorizedCredentials,
                    testRecordId,
                    testCid,
                    testSha256,
                    unauthorizedCredentials.getAddress()
            );
        });

        assertThat(ex.getMessage().toLowerCase()).containsAnyOf("notofficer", "revert");
    }

    @Test
    @DisplayName("Done when 3: A cache rebuilt from the chain matches")
    void testCacheRebuiltFromChainMatches() throws Exception {
        byte[] deedBytes = loadSampleDeedBytes();
        MockMultipartFile deedFile = new MockMultipartFile("file", "record_reconcile.png", "image/png", deedBytes);

        // 1. Upload and approve record -> committed to chain
        String uploadJson = mockMvc.perform(multipart("/api/records")
                        .file(deedFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readValue(uploadJson, RecordResponse.class).getId();

        String approveJson = mockMvc.perform(post("/api/records/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Approved for reconciliation test\"}")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecordResponse approved = objectMapper.readValue(approveJson, RecordResponse.class);
        String expectedTxHash = approved.getTxHash();
        String expectedCid = approved.getIpfsCid();
        String expectedSha256 = approved.getSha256Hash();

        // 2. Simulate total cache loss in MySQL
        recordVersionRepository.deleteAll();
        assertThat(recordVersionRepository.count()).isEqualTo(0);

        // Reset record in MySQL to simulate desynchronization
        LandRecord rec = landRecordRepository.findById(recordId).orElseThrow();
        rec.setCurrentVersion(0);
        rec.setStatus(RecordStatus.PENDING_REVIEW);
        landRecordRepository.save(rec);

        // 3. Trigger reconciliation job
        String reconcileJson = mockMvc.perform(post("/api/records/reconcile")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ReconcileReport report = objectMapper.readValue(reconcileJson, ReconcileReport.class);
        assertThat(report.isCacheMatchesChain()).isTrue();
        assertThat(report.getRebuiltCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.getDiscrepancies()).isEmpty();

        // 4. Verify cache rebuilt in MySQL matches the on-chain truth
        Optional<RecordVersion> rebuiltOpt = recordVersionRepository.findByRecordIdAndVersion(recordId, 1);
        assertThat(rebuiltOpt).isPresent();
        RecordVersion rebuilt = rebuiltOpt.get();
        assertThat(rebuilt.getCid()).isEqualTo(expectedCid);
        assertThat(rebuilt.getSha256()).isEqualTo(expectedSha256);
        assertThat(rebuilt.getTxHash()).isEqualTo(expectedTxHash);

        // Verify entity state updated back to COMMITTED
        LandRecord updatedRecord = landRecordRepository.findById(recordId).orElseThrow();
        assertThat(updatedRecord.getStatus()).isEqualTo(RecordStatus.COMMITTED);
        assertThat(updatedRecord.getCurrentVersion()).isEqualTo(1);
    }
}
