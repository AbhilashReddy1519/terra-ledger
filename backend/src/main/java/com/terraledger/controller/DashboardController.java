package com.terraledger.controller;

import com.terraledger.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/citizen/dashboard")
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> citizenDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "view", "citizen",
                "email", principal.getEmail(),
                "role", principal.getRole(),
                "message", "Welcome Citizen. You can upload records and view submitted land records."
        ));
    }

    @GetMapping("/officer/dashboard")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> officerDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "view", "officer",
                "email", principal.getEmail(),
                "role", principal.getRole(),
                "message", "Welcome Revenue Officer. Review queue and approval dashboard active."
        ));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "view", "admin",
                "email", principal.getEmail(),
                "role", principal.getRole(),
                "message", "Welcome Administrator. User role management and system settings."
        ));
    }
}
