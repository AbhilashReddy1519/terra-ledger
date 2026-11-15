package com.terraledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraledger.domain.Role;
import com.terraledger.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthAndRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Citizen cannot call officer endpoint -> 403 Forbidden")
    void citizenCallingOfficerEndpointGets403() throws Exception {
        RegisterRequest reg = new RegisterRequest("citizen1@example.com", "pass1234", Role.CITIZEN);
        MvcResult regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);
        String token = auth.getToken();

        // Citizen accessing citizen dashboard -> 200 OK
        mockMvc.perform(get("/api/citizen/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Citizen accessing officer dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/officer/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Officer calling officer endpoint -> 200 OK")
    void officerCallingOfficerEndpointGets200() throws Exception {
        RegisterRequest reg = new RegisterRequest("officer1@example.com", "pass1234", Role.OFFICER);
        MvcResult regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);
        String token = auth.getToken();

        mockMvc.perform(get("/api/officer/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Challenge replay is rejected")
    void replayedChallengeIsRejected() throws Exception {
        RegisterRequest reg = new RegisterRequest("walletuser@example.com", "pass1234", Role.CITIZEN);
        MvcResult regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);
        String token = auth.getToken();

        // 1. Request challenge
        MvcResult chalRes = mockMvc.perform(post("/auth/wallet/challenge")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        ChallengeResponse chal = objectMapper.readValue(chalRes.getResponse().getContentAsString(), ChallengeResponse.class);

        // 2. Sign message using test EC key pair
        ECKeyPair keyPair = Keys.createEcKeyPair();
        String sigHex = signPrefixedMessage(chal.getMessage(), keyPair);

        // 3. First verify -> 200 OK
        VerifyChallengeRequest verifyReq = new VerifyChallengeRequest(chal.getNonce(), sigHex);
        mockMvc.perform(post("/auth/wallet/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        // 4. Replay the same challenge -> must be rejected (400 Bad Request)
        mockMvc.perform(post("/auth/wallet/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invalid signature or expired challenge is rejected")
    void invalidSignatureRejected() throws Exception {
        RegisterRequest reg = new RegisterRequest("walletuser2@example.com", "pass1234", Role.CITIZEN);
        MvcResult regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);
        String token = auth.getToken();

        MvcResult chalRes = mockMvc.perform(post("/auth/wallet/challenge")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        ChallengeResponse chal = objectMapper.readValue(chalRes.getResponse().getContentAsString(), ChallengeResponse.class);

        // Send invalid/garbage signature
        VerifyChallengeRequest verifyReq = new VerifyChallengeRequest(chal.getNonce(), "0x123456");
        mockMvc.perform(post("/auth/wallet/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
    }

    private String signPrefixedMessage(String message, ECKeyPair keyPair) {
        Sign.SignatureData sigData = Sign.signPrefixedMessage(
                message.getBytes(StandardCharsets.UTF_8),
                keyPair
        );
        byte[] sigBytes = new byte[65];
        System.arraycopy(sigData.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(sigData.getS(), 0, sigBytes, 32, 32);
        sigBytes[64] = sigData.getV()[0];
        return Numeric.toHexString(sigBytes);
    }
}
