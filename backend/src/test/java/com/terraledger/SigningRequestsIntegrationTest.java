package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.Role;
import com.terraledger.domain.SigningRequest;
import com.terraledger.domain.SigningRequestStatus;
import com.terraledger.domain.User;
import com.terraledger.dto.CreateSigningRequest;
import com.terraledger.dto.SigningRequestResponse;
import com.terraledger.dto.SubmitSignatureRequest;
import com.terraledger.repository.AuditEventRepository;
import com.terraledger.repository.SigningRequestRepository;
import com.terraledger.repository.UserRepository;
import com.terraledger.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SigningRequestsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SigningRequestRepository signingRequestRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private ECKeyPair aliceKeyPair;
    private String aliceAddress;
    private ECKeyPair bobKeyPair;
    private String bobAddress;

    @BeforeEach
    void setUp() throws Exception {
        signingRequestRepository.deleteAll();
        auditEventRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User("citizen_m7@example.com", passwordEncoder.encode("password123"), Role.CITIZEN);
        user = userRepository.save(user);
        userToken = jwtTokenProvider.generateToken(user);

        aliceKeyPair = Keys.createEcKeyPair();
        aliceAddress = "0x" + Keys.getAddress(aliceKeyPair).toLowerCase();

        bobKeyPair = Keys.createEcKeyPair();
        bobAddress = "0x" + Keys.getAddress(bobKeyPair).toLowerCase();
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
    @DisplayName("Happy path: valid EIP-191 signature completes signing request")
    void testHappyPathSigningRequestVerification() throws Exception {
        CreateSigningRequest createDto = new CreateSigningRequest(
                "DEED-SURVEY-142/3A",
                "TRANSFER_CONSENT",
                aliceAddress,
                15
        );

        String createJson = mockMvc.perform(post("/api/signing/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse created = objectMapper.readValue(createJson, SigningRequestResponse.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(SigningRequestStatus.PENDING);
        assertThat(created.getCanonicalPayload()).contains("TRANSFER_CONSENT");
        assertThat(created.getCanonicalPayload()).contains(aliceAddress);

        // Sign with Alice's key
        String signature = signPersonalMessage(created.getCanonicalPayload(), aliceKeyPair);

        SubmitSignatureRequest submitDto = new SubmitSignatureRequest(signature);
        String submitJson = mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse completed = objectMapper.readValue(submitJson, SigningRequestResponse.class);
        assertThat(completed.getStatus()).isEqualTo(SigningRequestStatus.COMPLETED);
        assertThat(completed.getSignerAddress()).isEqualTo(aliceAddress);
        assertThat(completed.getSignature()).isEqualTo(signature);
        assertThat(completed.getSignedAt()).isNotNull();
    }

    @Test
    @DisplayName("Expired signing request is strictly rejected")
    void testExpiredSigningRequestRejected() throws Exception {
        CreateSigningRequest createDto = new CreateSigningRequest(
                "DEED-SURVEY-142/3A",
                "TRANSFER_CONSENT",
                aliceAddress,
                15
        );

        String createJson = mockMvc.perform(post("/api/signing/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse created = objectMapper.readValue(createJson, SigningRequestResponse.class);

        // Fast-forward expiry in the database
        SigningRequest entity = signingRequestRepository.findById(created.getId()).orElseThrow();
        entity.setExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        signingRequestRepository.save(entity);

        String signature = signPersonalMessage(created.getCanonicalPayload(), aliceKeyPair);

        SubmitSignatureRequest submitDto = new SubmitSignatureRequest(signature);
        mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isBadRequest());

        // Verify status transitioned to EXPIRED
        SigningRequest updated = signingRequestRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SigningRequestStatus.EXPIRED);
    }

    @Test
    @DisplayName("Replayed signing request is strictly rejected with 409 Conflict")
    void testReplayedSigningRequestRejected() throws Exception {
        CreateSigningRequest createDto = new CreateSigningRequest(
                "DEED-SURVEY-142/3A",
                "TRANSFER_CONSENT",
                aliceAddress,
                15
        );

        String createJson = mockMvc.perform(post("/api/signing/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse created = objectMapper.readValue(createJson, SigningRequestResponse.class);
        String signature = signPersonalMessage(created.getCanonicalPayload(), aliceKeyPair);

        SubmitSignatureRequest submitDto = new SubmitSignatureRequest(signature);

        // First submission succeeds
        mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isOk());

        // Replay submission fails with 409 Conflict
        mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Wrong-signer signature is strictly rejected")
    void testWrongSignerSignatureRejected() throws Exception {
        CreateSigningRequest createDto = new CreateSigningRequest(
                "DEED-SURVEY-142/3A",
                "TRANSFER_CONSENT",
                aliceAddress, // Alice is expected
                15
        );

        String createJson = mockMvc.perform(post("/api/signing/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse created = objectMapper.readValue(createJson, SigningRequestResponse.class);

        // Signed by Bob instead of Alice
        String bobSignature = signPersonalMessage(created.getCanonicalPayload(), bobKeyPair);

        SubmitSignatureRequest submitDto = new SubmitSignatureRequest(bobSignature);
        mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isBadRequest());

        // Still PENDING
        SigningRequest entity = signingRequestRepository.findById(created.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(SigningRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Tampered payload signature is strictly rejected")
    void testTamperedPayloadSignatureRejected() throws Exception {
        CreateSigningRequest createDto = new CreateSigningRequest(
                "DEED-SURVEY-142/3A",
                "TRANSFER_CONSENT",
                aliceAddress,
                15
        );

        String createJson = mockMvc.perform(post("/api/signing/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SigningRequestResponse created = objectMapper.readValue(createJson, SigningRequestResponse.class);

        // Alice signs a modified string instead of the exact canonical payload
        String tamperedPayload = created.getCanonicalPayload() + "\nTampered: true";
        String signature = signPersonalMessage(tamperedPayload, aliceKeyPair);

        SubmitSignatureRequest submitDto = new SubmitSignatureRequest(signature);
        mockMvc.perform(post("/api/signing/requests/" + created.getId() + "/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated request to signing endpoints is refused with 401")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/signing/requests/pending?address=" + aliceAddress))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/signing/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
