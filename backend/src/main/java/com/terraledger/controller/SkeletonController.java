package com.terraledger.controller;

import com.terraledger.service.SkeletonChainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * M0 skeleton endpoint.  A hardcoded record travels from the UI through this
 * controller to the chain service and back.  This controller is removed /
 * replaced in M1.
 */
@RestController
@RequestMapping("/api/skeleton")
public class SkeletonController {

    private final SkeletonChainService chainService;

    public SkeletonController(SkeletonChainService chainService) {
        this.chainService = chainService;
    }

    /**
     * POST /api/skeleton/record
     * Writes a hardcoded record to the chain and returns the result.
     */
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> createSkeletonRecord() {
        var result = chainService.commitHardcodedRecord();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/skeleton/record/{recordId}
     * Reads a record back from the chain.
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<Map<String, Object>> getSkeletonRecord(
            @PathVariable String recordId) {
        var result = chainService.readRecord(recordId);
        return ResponseEntity.ok(result);
    }
}
