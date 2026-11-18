package com.terraledger.controller;

import com.terraledger.security.UserPrincipal;
import com.terraledger.dto.ApproveTransferRequest;
import com.terraledger.dto.CreateTransferRequest;
import com.terraledger.dto.TransferResponse;
import com.terraledger.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/api/transfers")
    public ResponseEntity<?> initiateTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            TransferResponse response = transferService.createTransfer(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/transfers/{id}")
    public ResponseEntity<?> getTransfer(@PathVariable("id") String id) {
        try {
            TransferResponse response = transferService.getTransfer(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/records/{recordId}/transfers")
    public ResponseEntity<List<TransferResponse>> getTransfersForRecord(@PathVariable("recordId") String recordId) {
        List<TransferResponse> transfers = transferService.getTransfersForRecord(recordId);
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/api/registrar/transfers/pending")
    public ResponseEntity<List<TransferResponse>> getPendingRegistrarTransfers() {
        List<TransferResponse> pending = transferService.getPendingRegistrarTransfers();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/api/registrar/transfers/{id}/approve")
    public ResponseEntity<?> approveTransfer(
            @PathVariable("id") String id,
            @RequestBody(required = false) ApproveTransferRequest request,
            @AuthenticationPrincipal UserPrincipal registrarUser) {
        try {
            ApproveTransferRequest dto = (request != null) ? request : new ApproveTransferRequest("Approved by Registrar");
            TransferResponse response = transferService.approveTransfer(id, dto, registrarUser);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Transfer approval failed: " + e.getMessage()));
        }
    }
}
