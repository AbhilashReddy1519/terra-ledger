package com.terraledger.controller;

import com.terraledger.dto.CreateSigningRequest;
import com.terraledger.dto.SigningRequestResponse;
import com.terraledger.dto.SubmitSignatureRequest;
import com.terraledger.service.SigningRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signing/requests")
public class SigningRequestController {

    private final SigningRequestService signingRequestService;

    public SigningRequestController(SigningRequestService signingRequestService) {
        this.signingRequestService = signingRequestService;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@Valid @RequestBody CreateSigningRequest request) {
        try {
            SigningRequestResponse response = signingRequestService.createRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequest(@PathVariable("id") String id) {
        try {
            SigningRequestResponse response = signingRequestService.getRequest(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitSignature(
            @PathVariable("id") String id,
            @Valid @RequestBody SubmitSignatureRequest request) {
        try {
            SigningRequestResponse response = signingRequestService.submitSignature(id, request.getSignature());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Replay attack attempt
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Expired or wrong signer
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process signature: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SigningRequestResponse>> getPendingRequests(
            @RequestParam("address") String address) {
        List<SigningRequestResponse> pending = signingRequestService.getPendingRequestsForSigner(address);
        return ResponseEntity.ok(pending);
    }
}
