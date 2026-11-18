package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.dto.VerificationResponse;
import com.terraledger.repository.AuditEventRepository;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.repository.RecordVersionRepository;
import com.terraledger.repository.UserRepository;
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

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LandRecordRepository landRecordRepository;

    @Autowired
    private RecordVersionRepository recordVersionRepository;

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

    @BeforeEach
    void setUp() {
        recordVersionRepository.deleteAll();
        auditEventRepository.deleteAll();
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        User citizenUser = new User("citizen_m6@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizenUser = userRepository.save(citizenUser);
        citizenToken = jwtTokenProvider.generateToken(citizenUser);

        User officerUser = new User("officer_m6@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
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
        return "SYNTHETIC_SALE_DEED_DOCUMENT_BYTES_FOR_M6_VERIFICATION_TEST".getBytes();
    }

    private RecordResponse uploadAndCommitRecord(byte[] deedBytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "deed_sample.png",
                "image/png",
                deedBytes
        );

        String uploadJson = mockMvc.perform(multipart("/api/records")
                        .file(file)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        RecordResponse created = objectMapper.readValue(uploadJson, RecordResponse.class);

        ReviewDecisionRequest decision = new ReviewDecisionRequest();
        decision.setRemarks("Approved for M6 verification testing");

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

    @Test
    @DisplayName("Original approved file matches on-chain cryptographic commitment")
    void testVerifyOriginalFileMatchesOnChain() throws Exception {
        byte[] originalBytes = loadSampleDeedBytes();
        RecordResponse committedRecord = uploadAndCommitRecord(originalBytes);

        assertThat(committedRecord.getStatus().name()).isEqualTo("COMMITTED");
        assertThat(committedRecord.getTxHash()).isNotNull();

        MockMultipartFile verifyFile = new MockMultipartFile(
                "file",
                "deed_original.png",
                "image/png",
                originalBytes
        );

        String verifyResultJson = mockMvc.perform(multipart("/api/verify")
                        .file(verifyFile)
                        .param("recordId", committedRecord.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VerificationResponse response = objectMapper.readValue(verifyResultJson, VerificationResponse.class);

        assertThat(response.isMatch()).isTrue();
        assertThat(response.getStatus()).isEqualTo("MATCH");
        assertThat(response.getMessage()).containsIgnoringCase("identical to the approved file");
        assertThat(response.getRecordId()).isEqualTo(committedRecord.getId());
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getComputedHash()).isEqualTo(committedRecord.getSha256Hash());
        assertThat(response.getOnChainHash()).isEqualTo(committedRecord.getSha256Hash());
        assertThat(response.getCid()).isEqualTo(committedRecord.getIpfsCid());
        assertThat(response.getApprover()).isNotNull();
        assertThat(response.getCommittedAt()).isNotNull();
        assertThat(response.getDisclaimer()).contains("A match confirms that this digital file is bit-for-bit identical");
    }

    @Test
    @DisplayName("Changing a single byte gives a mismatch against on-chain truth")
    void testVerifyTamperedFileGivesMismatch() throws Exception {
        byte[] originalBytes = loadSampleDeedBytes();
        RecordResponse committedRecord = uploadAndCommitRecord(originalBytes);

        // Tamper with exactly one byte
        byte[] tamperedBytes = originalBytes.clone();
        int flipIndex = tamperedBytes.length / 2;
        tamperedBytes[flipIndex] = (byte) (tamperedBytes[flipIndex] ^ 0x01);

        MockMultipartFile tamperedFile = new MockMultipartFile(
                "file",
                "deed_tampered.png",
                "image/png",
                tamperedBytes
        );

        String verifyResultJson = mockMvc.perform(multipart("/api/verify")
                        .file(tamperedFile)
                        .param("recordId", committedRecord.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VerificationResponse response = objectMapper.readValue(verifyResultJson, VerificationResponse.class);

        assertThat(response.isMatch()).isFalse();
        assertThat(response.getStatus()).isEqualTo("MISMATCH");
        assertThat(response.getMessage()).containsIgnoringCase("different from the approved file");
        assertThat(response.getRecordId()).isEqualTo(committedRecord.getId());
        assertThat(response.getComputedHash()).isNotEqualTo(committedRecord.getSha256Hash());
        assertThat(response.getOnChainHash()).isEqualTo(committedRecord.getSha256Hash());
        assertThat(response.getDisclaimer()).contains("A match proves the digital document has not been altered");
    }

    @Test
    @DisplayName("File verified without record ID matches if committed in records")
    void testVerifyFileWithoutRecordIdMatches() throws Exception {
        byte[] originalBytes = loadSampleDeedBytes();
        RecordResponse committedRecord = uploadAndCommitRecord(originalBytes);

        MockMultipartFile verifyFile = new MockMultipartFile(
                "file",
                "deed_original.png",
                "image/png",
                originalBytes
        );

        String verifyResultJson = mockMvc.perform(multipart("/api/verify")
                        .file(verifyFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VerificationResponse response = objectMapper.readValue(verifyResultJson, VerificationResponse.class);

        assertThat(response.isMatch()).isTrue();
        assertThat(response.getStatus()).isEqualTo("MATCH");
        assertThat(response.getRecordId()).isEqualTo(committedRecord.getId());
        assertThat(response.getComputedHash()).isEqualTo(committedRecord.getSha256Hash());
    }

    @Test
    @DisplayName("Unknown file without record ID returns NOT_FOUND")
    void testVerifyUnknownFileWithoutRecordIdReturnsNotFound() throws Exception {
        byte[] unknownBytes = "TOTALLY_UNKNOWN_UNCOMMITTED_BYTES_123456789".getBytes();

        MockMultipartFile verifyFile = new MockMultipartFile(
                "file",
                "unknown.png",
                "image/png",
                unknownBytes
        );

        String verifyResultJson = mockMvc.perform(multipart("/api/verify")
                        .file(verifyFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VerificationResponse response = objectMapper.readValue(verifyResultJson, VerificationResponse.class);

        assertThat(response.isMatch()).isFalse();
        assertThat(response.getStatus()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Unauthenticated request to verify endpoint is refused with 401")
    void testVerifyUnauthenticatedUserRefused() throws Exception {
        MockMultipartFile verifyFile = new MockMultipartFile(
                "file",
                "deed.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/verify")
                        .file(verifyFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Empty file is refused with 400 Bad Request")
    void testVerifyEmptyFileRefused() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/verify")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isBadRequest());
    }
}
