package com.terraledger.controller;

import com.terraledger.dto.FieldUpdateRequest;
import com.terraledger.dto.RecordResponse;
import com.terraledger.security.UserPrincipal;
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

    public RecordController(LandRecordService landRecordService) {
        this.landRecordService = landRecordService;
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
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<RecordResponse> response = landRecordService.listRecords(currentUser);
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
}
