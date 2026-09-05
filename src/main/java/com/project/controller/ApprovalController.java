package com.project.controller;

import com.project.dto.ApprovalResponse;
import com.project.dto.RejectRequest;
import com.project.entity.ApprovalRequest;
import com.project.entity.UserEntity;
import com.project.repository.UserRepository;
import com.project.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserRepository userRepository;

    public ApprovalController(ApprovalService approvalService, UserRepository userRepository) {
        this.approvalService = approvalService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        List<ApprovalResponse> responses = approvalService.getPendingApprovals()
                .stream()
                .map(ApprovalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity reviewer = resolveUser(currentUser);
        ApprovalRequest result = approvalService.approve(id, reviewer);
        return ResponseEntity.ok(ApprovalResponse.fromEntity(result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity reviewer = resolveUser(currentUser);
        String reason = request != null ? request.reason() : null;
        ApprovalRequest result = approvalService.reject(id, reviewer, reason);
        return ResponseEntity.ok(ApprovalResponse.fromEntity(result));
    }

    private UserEntity resolveUser(UserDetails currentUser) {
        return userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}