package com.terraledger.controller;

import com.terraledger.dto.*;
import com.terraledger.security.UserPrincipal;
import com.terraledger.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/wallet/challenge")
    public ResponseEntity<ChallengeResponse> getChallenge(
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal != null ? principal.getId() : null;
        ChallengeResponse res = authService.createChallenge(userId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/wallet/verify")
    public ResponseEntity<AuthResponse> verifyWallet(
            @Valid @RequestBody VerifyChallengeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal != null ? principal.getId() : null;
        AuthResponse res = authService.verifyChallenge(req, userId);
        return ResponseEntity.ok(res);
    }
}
