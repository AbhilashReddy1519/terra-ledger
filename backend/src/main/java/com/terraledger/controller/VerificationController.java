package com.terraledger.controller;

import com.terraledger.dto.VerificationResponse;
import com.terraledger.service.VerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping(value = {"/api/verify", "/verify"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "recordId", required = false) String recordId) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"File must not be empty\"}");
        }

        try {
            VerificationResponse response = verificationService.verifyFile(file, recordId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\":\"Verification failed: " + e.getMessage() + "\"}");
        }
    }
}
