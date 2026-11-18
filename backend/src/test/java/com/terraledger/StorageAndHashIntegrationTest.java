package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.dto.StorageVerificationResponse;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.repository.UserRepository;
import com.terraledger.security.JwtTokenProvider;
import com.terraledger.service.EncryptionService;
import com.terraledger.service.IpfsClientService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StorageAndHashIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LandRecordRepository landRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IpfsClientService ipfsClientService;

    @Autowired
    private EncryptionService encryptionService;

    private String citizenToken;
    private String officerToken;
    private String otherCitizenToken;

    @BeforeEach
    void setUp() {
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        User citizen = new User("citizen_m4@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizen = userRepository.save(citizen);
        citizenToken = jwtTokenProvider.generateToken(citizen);

        User officer = new User("officer_m4@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
        officer = userRepository.save(officer);
        officerToken = jwtTokenProvider.generateToken(officer);

        User otherCitizen = new User("other_citizen_m4@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        otherCitizen = userRepository.save(otherCitizen);
        otherCitizenToken = jwtTokenProvider.generateToken(otherCitizen);
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
        // Valid 1x1 PNG bytes
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
    @DisplayName("Done when: A stored file decrypts to bytes whose hash equals the stored hash")
    void testStoredFileDecryptsToBytesWhoseHashEqualsStoredHash() throws Exception {
        byte[] originalDeedBytes = loadSampleDeedBytes();
        String originalHash = sha256(originalDeedBytes);

        // 1. Citizen uploads deed document
        MockMultipartFile deedFile = new MockMultipartFile(
                "file",
                "deed_sample.png",
                "image/png",
                originalDeedBytes
        );

        String uploadRespJson = mockMvc.perform(multipart("/api/records")
                        .file(deedFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        RecordResponse uploadResp = objectMapper.readValue(uploadRespJson, RecordResponse.class);
        String recordId = uploadResp.getId();
        assertThat(recordId).isNotBlank();
        assertThat(uploadResp.getSha256Hash()).isEqualTo(originalHash);

        // 2. Officer approves record -> triggers AES-256-GCM encryption & IPFS upload
        ReviewDecisionRequest decision = new ReviewDecisionRequest("Verified deed authenticity and boundary markers.");
        String approveRespJson = mockMvc.perform(post("/api/records/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecordResponse approveResp = objectMapper.readValue(approveRespJson, RecordResponse.class);
        String ipfsCid = approveResp.getIpfsCid();
        assertThat(ipfsCid).isNotBlank();
        assertThat(approveResp.getStatus()).isIn(com.terraledger.domain.RecordStatus.APPROVED, com.terraledger.domain.RecordStatus.COMMITTED);

        // 3. Directly inspect IPFS content: verify IPFS stores CIPHERTEXT, not plaintext
        byte[] rawIpfsBytes = ipfsClientService.cat(ipfsCid);
        assertThat(rawIpfsBytes).isNotNull();
        assertThat(rawIpfsBytes).isNotEqualTo(originalDeedBytes);
        // Verify sensitive text does not leak in raw IPFS bytes
        String rawIpfsString = new String(rawIpfsBytes, StandardCharsets.ISO_8859_1);
        assertThat(rawIpfsString).doesNotContain("Ramesh Chandra");
        assertThat(rawIpfsString).doesNotContain("SY-902/B");

        // 4. Download and decrypt through API endpoint
        byte[] downloadedBytes = mockMvc.perform(get("/api/records/" + recordId + "/download")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Document-SHA256", originalHash))
                .andReturn().getResponse().getContentAsByteArray();

        // 5. Verify the Done When condition:
        // "A stored file decrypts to bytes whose hash equals the stored hash"
        assertThat(downloadedBytes).isEqualTo(originalDeedBytes);
        String decryptedHash = sha256(downloadedBytes);
        assertThat(decryptedHash).isEqualTo(originalHash);
        assertThat(decryptedHash).isEqualTo(approveResp.getSha256Hash());

        // 6. Verify through storage verification endpoint
        String verifyJson = mockMvc.perform(get("/api/records/" + recordId + "/verify-storage")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StorageVerificationResponse verifyResp = objectMapper.readValue(verifyJson, StorageVerificationResponse.class);
        assertThat(verifyResp.isVerified()).isTrue();
        assertThat(verifyResp.getIpfsCid()).isEqualTo(ipfsCid);
        assertThat(verifyResp.getSha256Hash()).isEqualTo(originalHash);
        assertThat(verifyResp.getDecryptedHash()).isEqualTo(originalHash);
    }

    @Test
    @DisplayName("Unauthorized citizen cannot download or verify other citizen's stored record")
    void testUnauthorizedCitizenCannotDownloadOrVerify() throws Exception {
        byte[] deedBytes = loadSampleDeedBytes();
        MockMultipartFile deedFile = new MockMultipartFile("file", "title.png", "image/png", deedBytes);

        String uploadJson = mockMvc.perform(multipart("/api/records")
                        .file(deedFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readValue(uploadJson, RecordResponse.class).getId();

        mockMvc.perform(post("/api/records/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // Other citizen tries to download -> 403 Forbidden
        mockMvc.perform(get("/api/records/" + recordId + "/download")
                        .header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isForbidden());

        // Other citizen tries to verify -> 403 Forbidden
        mockMvc.perform(get("/api/records/" + recordId + "/verify-storage")
                        .header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tampered ciphertext in storage causes decryption to fail authentication")
    void testTamperedCiphertextFailsDecryption() {
        byte[] original = "TAMPER TEST DOCUMENT".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptionService.encrypt(original);

        // Tamper with one byte in the ciphertext body
        byte[] tampered = Arrays.copyOf(encrypted, encrypted.length);
        tampered[14] ^= 0xFF;

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class, () -> {
            encryptionService.decrypt(tampered);
        });
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
