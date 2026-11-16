package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.dto.FieldUpdateRequest;
import com.terraledger.dto.RecordResponse;
import com.terraledger.repository.LandRecordRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RecordUploadAndOcrIntegrationTest {

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

    private String citizenToken;
    private String citizen2Token;
    private String officerToken;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        citizenUser = new User("citizen_uploader@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizenUser = userRepository.save(citizenUser);
        citizenToken = jwtTokenProvider.generateToken(citizenUser);

        User citizen2 = new User("other_citizen@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizen2 = userRepository.save(citizen2);
        citizen2Token = jwtTokenProvider.generateToken(citizen2);

        User officer = new User("officer_tester@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
        officer = userRepository.save(officer);
        officerToken = jwtTokenProvider.generateToken(officer);
    }

    @Test
    @DisplayName("Uploads with wrong file types (.txt, .exe) are refused with HTTP 400")
    void wrongFileTypesAreRefused() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "This is not a land deed scan".getBytes()
        );

        mockMvc.perform(multipart("/api/records")
                        .file(textFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isBadRequest());

        MockMultipartFile binFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                new byte[]{0x4D, 0x5A, 0x00, 0x00}
        );

        mockMvc.perform(multipart("/api/records")
                        .file(binFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated upload is refused with HTTP 401")
    void unauthenticatedUploadRefused() throws Exception {
        MockMultipartFile sampleFile = new MockMultipartFile(
                "file",
                "deed.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/records")
                        .file(sampleFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ten synthetic scans are uploaded, OCR extracted, and return editable fields")
    void tenSyntheticScansGiveEditableFields() throws Exception {
        File sampleDir = new File("../sample-data");
        if (!sampleDir.exists()) {
            sampleDir = new File("sample-data");
        }
        assertThat(sampleDir.exists()).isTrue();

        File[] files = sampleDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".pdf"));
        assertThat(files).isNotNull();
        assertThat(files.length).isGreaterThanOrEqualTo(10);

        int processedCount = 0;

        for (File file : files) {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String contentType = file.getName().endsWith(".pdf") ? "application/pdf" : "image/png";

            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    file.getName(),
                    contentType,
                    fileBytes
            );

            // 1. Upload and OCR
            String jsonResp = mockMvc.perform(multipart("/api/records")
                            .file(multipartFile)
                            .header("Authorization", "Bearer " + citizenToken))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            RecordResponse response = objectMapper.readValue(jsonResp, RecordResponse.class);

            assertThat(response.getId()).isNotBlank();
            assertThat(response.getStatus().name()).isEqualTo("PENDING_REVIEW");
            assertThat(response.getSha256Hash()).hasSize(64);
            assertThat(response.getFileName()).isEqualTo(file.getName());

            // Check extracted fields are present
            assertThat(response.getFields()).isNotEmpty();
            assertThat(response.getOwnerName()).isNotBlank();
            assertThat(response.getSurveyNo()).isNotBlank();
            assertThat(response.getArea()).isNotBlank();

            // 2. Edit fields (simulate officer/user correction)
            FieldUpdateRequest updateReq = new FieldUpdateRequest(Map.of(
                    "survey_no", response.getSurveyNo() + "-CORRECTED",
                    "owner_name", response.getOwnerName() + " (Verified)"
            ));

            String updatedJson = mockMvc.perform(put("/api/records/" + response.getId() + "/fields")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq))
                            .header("Authorization", "Bearer " + officerToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            RecordResponse updated = objectMapper.readValue(updatedJson, RecordResponse.class);
            assertThat(updated.getSurveyNo()).endsWith("-CORRECTED");
            assertThat(updated.getOwnerName()).endsWith("(Verified)");
            assertThat(updated.getFields().get("survey_no").getCorrectedValue()).endsWith("-CORRECTED");

            processedCount++;
        }

        assertThat(processedCount).isEqualTo(10);
    }

    @Test
    @DisplayName("Role rules: Citizen cannot access another citizen's record, but Officer can")
    void roleRulesForRecordAccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_deed.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        // Citizen 1 creates a record directly in repo
        var rec = new com.terraledger.domain.LandRecord();
        rec.setUploadedBy(citizenUser.getId());
        rec.setFileName("my_deed.png");
        rec.setFileContentType("image/png");
        rec.setFileSize(4L);
        rec.setSha256Hash("abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234");
        rec.setSurveyNo("SY-999");
        rec.setOwnerName("Citizen One");
        rec.setArea("1000 Sq. Ft.");
        rec = landRecordRepository.save(rec);

        // Citizen 1 can view own record
        mockMvc.perform(get("/api/records/" + rec.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());

        // Citizen 2 is forbidden (403)
        mockMvc.perform(get("/api/records/" + rec.getId())
                        .header("Authorization", "Bearer " + citizen2Token))
                .andExpect(status().isForbidden());

        // Officer can view record (200)
        mockMvc.perform(get("/api/records/" + rec.getId())
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }
}
