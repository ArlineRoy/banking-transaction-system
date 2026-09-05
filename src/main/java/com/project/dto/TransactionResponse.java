package com.project.dto;

import com.project.entity.Status;
import com.project.entity.Transactions;
import com.project.entity.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        Type type,
        BigDecimal amount,
        Status status,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
    public static TransactionResponse fromEntity(Transactions txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getType(),
                txn.getAmount(),
                txn.getStatus(),
                txn.getFailureReason(),
                txn.getCreatedAt(),
                txn.getCompletedAt()
        );
    }
}