package com.terraledger.controller;

import com.terraledger.domain.RecordStatus;
import com.terraledger.dto.AuditEventResponse;
import com.terraledger.dto.FieldUpdateRequest;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.security.UserPrincipal;
import com.terraledger.service.AuditService;
import com.terraledger.service.LandRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final LandRecordService landRecordService;
    private final AuditService auditService;

    public RecordController(LandRecordService landRecordService, AuditService auditService) {
        this.landRecordService = landRecordService;
        this.auditService = auditService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordResponse> uploadRecord(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RecordResponse response = landRecordService.createRecord(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordResponse> getRecord(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RecordResponse response = landRecordService.getRecord(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecordResponse>> listRecords(
            @RequestParam(value = "status", required = false) RecordStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<RecordResponse> response = landRecordService.listRecords(status, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/fields")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordResponse> updateFields(
            @PathVariable("id") String id,
            @RequestBody FieldUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RecordResponse response = landRecordService.updateRecordFields(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<RecordResponse> approveRecord(
            @PathVariable("id") String id,
            @RequestBody(required = false) ReviewDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RecordResponse response = landRecordService.approveRecord(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<RecordResponse> rejectRecord(
            @PathVariable("id") String id,
            @RequestBody(required = false) ReviewDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RecordResponse response = landRecordService.rejectRecord(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditEventResponse>> getRecordAuditLog(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        // Enforce that only authorized users (owner or officer/admin) can view audit log
        landRecordService.getRecord(id, currentUser);
        List<AuditEventResponse> events = auditService.getEventsForEntity(id);
        return ResponseEntity.ok(events);
    }
}
