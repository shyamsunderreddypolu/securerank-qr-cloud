package com.securerank.controller;

import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.DashboardStatsResponse;
import com.securerank.dto.response.KeyRequestResponse;
import com.securerank.entity.User;
import com.securerank.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_PKG')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/pending-users")
    public ResponseEntity<List<User>> getPendingUsers() {
        return ResponseEntity.ok(adminService.getPendingUsers());
    }

    @PostMapping("/approve-user/{userId}")
    public ResponseEntity<ApiResponse> approveUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(adminService.approveUser(userId));
    }

    @GetMapping("/pending-keys")
    public ResponseEntity<List<KeyRequestResponse>> getPendingKeyRequests() {
        return ResponseEntity.ok(adminService.getPendingKeyRequests());
    }

    @PostMapping("/approve-key/{requestId}")
    public ResponseEntity<ApiResponse> approveKeyRequest(@PathVariable("requestId") Long requestId) {
        return ResponseEntity.ok(adminService.approveKeyRequest(requestId));
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }
}
