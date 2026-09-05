package com.project.dto;

import com.project.entity.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        UUID transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        java.math.BigDecimal amount,
        ApprovalStatus status,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt
) {
    public static ApprovalResponse fromEntity(com.project.entity.ApprovalRequest ar) {
        return new ApprovalResponse(
                ar.getId(),
                ar.getTransaction().getId(),
                ar.getTransaction().getFromAccount().getAccountNumber(),
                ar.getTransaction().getToAccount().getAccountNumber(),
                ar.getTransaction().getAmount(),
                ar.getStatus(),
                ar.getReviewNote(),
                ar.getCreatedAt(),
                ar.getReviewedAt()
        );
    }
}