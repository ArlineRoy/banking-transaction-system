package com.project.service;

import com.project.entity.*;
import com.project.repository.ApprovalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final TransferExecutor transferExecutor;

    public ApprovalService(ApprovalRequestRepository approvalRequestRepository,
                           TransferExecutor transferExecutor) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.transferExecutor = transferExecutor;
    }

    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING);
    }

    @Transactional
    public ApprovalRequest approve(UUID approvalRequestId, UserEntity reviewer) {

        ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + approvalRequestId));

        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    "Approval request is already " + approvalRequest.getStatus() + " — cannot approve again");
        }

        UUID transactionId = approvalRequest.getTransaction().getId();

        // This actually moves the money — reusing Day 4-5's locking/debit/credit logic
        transferExecutor.executeApprovedTransfer(transactionId);

        approvalRequest.setStatus(ApprovalStatus.APPROVED);
        approvalRequest.setReviewedBy(reviewer);
        approvalRequest.setReviewedAt(Instant.now());

        return approvalRequestRepository.save(approvalRequest);
    }

    @Transactional
    public ApprovalRequest reject(UUID approvalRequestId, UserEntity reviewer, String reason) {

        ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + approvalRequestId));

        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    "Approval request is already " + approvalRequest.getStatus() + " — cannot reject again");
        }

        Transactions txn = approvalRequest.getTransaction();
        txn.setStatus(Status.FAILED);
        txn.setFailureReason("Rejected by " + reviewer.getEmail() +
                (reason != null && !reason.isBlank() ? ": " + reason : ""));
        txn.setCompletedAt(Instant.now());

        approvalRequest.setStatus(ApprovalStatus.REJECTED);
        approvalRequest.setReviewedBy(reviewer);
        approvalRequest.setReviewNote(reason);
        approvalRequest.setReviewedAt(Instant.now());

        return approvalRequestRepository.save(approvalRequest);
    }
}