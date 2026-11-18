package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.LandRecord;
import com.terraledger.domain.RecordStatus;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.dto.AuditEventResponse;
import com.terraledger.dto.FieldUpdateRequest;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.repository.AuditEventRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OfficerReviewAndAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LandRecordRepository landRecordRepository;

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
    private User citizenUser;
    private User officerUser;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        landRecordRepository.deleteAll();
        userRepository.deleteAll();

        citizenUser = new User("citizen_m3@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        citizenUser = userRepository.save(citizenUser);
        citizenToken = jwtTokenProvider.generateToken(citizenUser);

        officerUser = new User("officer_m3@example.com", passwordEncoder.encode("password123"), Role.OFFICER);
        officerUser = userRepository.save(officerUser);
        officerToken = jwtTokenProvider.generateToken(officerUser);
    }

    private LandRecord createPendingRecord() {
        LandRecord record = new LandRecord();
        record.setUploadedBy(citizenUser.getId());
        record.setFileName("deed_review.png");
        record.setFileContentType("image/png");
        record.setFileSize(1024L);
        record.setSha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        record.setSurveyNo("SY-501");
        record.setOwnerName("Original Owner");
        record.setArea("2000 Sq. Ft.");
        record.setBoundaries("North: Road, South: Land, East: Plot 1, West: Plot 2");
        record.setDeedType("Sale Deed");
        record.setStatus(RecordStatus.PENDING_REVIEW);
        return landRecordRepository.save(record);
    }

    @Test
    @DisplayName("Non-officer (Citizen) attempting to approve or reject gets 403 Forbidden")
    void nonOfficerCannotApproveOrReject() throws Exception {
        LandRecord record = createPendingRecord();

        ReviewDecisionRequest req = new ReviewDecisionRequest("Attempting citizen approval");

        // Citizen trying to approve -> 403
        mockMvc.perform(post("/api/records/" + record.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // Citizen trying to reject -> 403
        mockMvc.perform(post("/api/records/" + record.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // Verify status remains PENDING_REVIEW
        LandRecord current = landRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(RecordStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Officer review workflow: field correction, approval, and audit log tracking")
    void officerCanCorrectFieldsAndApproveWithAuditLog() throws Exception {
        LandRecord record = createPendingRecord();

        // 1. Officer corrects fields
        FieldUpdateRequest updateReq = new FieldUpdateRequest(Map.of(
                "survey_no", "SY-501-A",
                "owner_name", "Verified Owner Name"
        ));

        mockMvc.perform(put("/api/records/" + record.getId() + "/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // 2. Officer approves record
        ReviewDecisionRequest approveReq = new ReviewDecisionRequest("All physical boundary checks verified and approved.");

        String approveRespJson = mockMvc.perform(post("/api/records/" + record.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecordResponse approveResp = objectMapper.readValue(approveRespJson, RecordResponse.class);
        assertThat(approveResp.getStatus()).isIn(RecordStatus.APPROVED, RecordStatus.COMMITTED);
        assertThat(approveResp.getSurveyNo()).isEqualTo("SY-501-A");
        assertThat(approveResp.getOwnerName()).isEqualTo("Verified Owner Name");

        // 3. Verify audit log captures actor, timestamp, action, and details
        String auditRespJson = mockMvc.perform(get("/api/records/" + record.getId() + "/audit")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<AuditEventResponse> auditEvents = objectMapper.readValue(
                auditRespJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEventResponse.class)
        );

        assertThat(auditEvents).isNotEmpty();

        // Check APPROVE audit event exists
        AuditEventResponse approveEvent = auditEvents.stream()
                .filter(e -> "APPROVE".equals(e.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(approveEvent).isNotNull();
        assertThat(approveEvent.getActor()).isEqualTo("officer_m3@example.com");
        assertThat(approveEvent.getEntityId()).isEqualTo(record.getId());
        assertThat(approveEvent.getDetail()).contains("verified and approved");
        assertThat(approveEvent.getCreatedAt()).isNotNull();

        // Check FIELD_UPDATE audit event exists
        AuditEventResponse updateEvent = auditEvents.stream()
                .filter(e -> "FIELD_UPDATE".equals(e.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(updateEvent).isNotNull();
        assertThat(updateEvent.getActor()).isEqualTo("officer_m3@example.com");
    }

    @Test
    @DisplayName("Officer rejection stops the record from moving forward and logs rejection audit event")
    void officerCanRejectRecordWithAuditLog() throws Exception {
        LandRecord record = createPendingRecord();

        ReviewDecisionRequest rejectReq = new ReviewDecisionRequest("Boundary coordinates conflict with municipal survey records.");

        String rejectRespJson = mockMvc.perform(post("/api/records/" + record.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecordResponse rejectResp = objectMapper.readValue(rejectRespJson, RecordResponse.class);
        assertThat(rejectResp.getStatus()).isEqualTo(RecordStatus.REJECTED);

        // Verify that rejected record cannot be approved later
        mockMvc.perform(post("/api/records/" + record.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewDecisionRequest("Trying to approve rejected")))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isBadRequest());

        // Verify rejection audit event
        String auditRespJson = mockMvc.perform(get("/api/records/" + record.getId() + "/audit")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<AuditEventResponse> auditEvents = objectMapper.readValue(
                auditRespJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEventResponse.class)
        );

        AuditEventResponse rejectEvent = auditEvents.stream()
                .filter(e -> "REJECT".equals(e.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(rejectEvent).isNotNull();
        assertThat(rejectEvent.getActor()).isEqualTo("officer_m3@example.com");
        assertThat(rejectEvent.getDetail()).contains("Boundary coordinates conflict");
    }

    @Test
    @DisplayName("Review queue endpoint returns pending records for officer review")
    void officerReviewQueueReturnsPendingRecords() throws Exception {
        LandRecord rec1 = createPendingRecord();
        LandRecord rec2 = createPendingRecord();

        // Officer approves rec1
        mockMvc.perform(post("/api/records/" + rec1.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewDecisionRequest("Approved")))
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // Queue filter for PENDING_REVIEW should only return rec2
        String queueJson = mockMvc.perform(get("/api/records?status=PENDING_REVIEW")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<RecordResponse> queue = objectMapper.readValue(
                queueJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, RecordResponse.class)
        );

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getId()).isEqualTo(rec2.getId());
    }
}
