package com.project.service;

import com.project.entity.*;
import com.project.repository.AccountRepository;
import com.project.repository.ApprovalRequestRepository;
import com.project.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferExecutor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApprovalRequestRepository approvalRequestRepository;

    public TransferExecutor(AccountRepository accountRepository,
                            TransactionRepository transactionRepository, ApprovalRequestRepository approvalRequestRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.approvalRequestRepository = approvalRequestRepository;
    }

    @Transactional
    public Transactions executeTransfer(UUID fromAccountId, UUID toAccountId,
                                        BigDecimal amount, UserEntity user, String idempotencyKey) {

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        AccountPair pair = fetchAccountsInLockOrder(fromAccountId, toAccountId);
        Transactions txn = new Transactions(Type.TRANSFER, pair.fromAccount(), pair.toAccount(), amount, user, idempotencyKey);

        return applyTransfer(txn, pair, amount);
    }

    // ===== PATH 2: above-threshold, park for approval instead of executing =====
    @Transactional
    public Transactions createApprovalRequest(UUID fromAccountId, UUID toAccountId,
                                              BigDecimal amount, UserEntity user, String idempotencyKey) {

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        Transactions txn = new Transactions(Type.TRANSFER, fromAccount, toAccount, amount, user, idempotencyKey);
        txn.setStatus(Status.AWAITING_APPROVAL);
        txn = transactionRepository.save(txn);

        ApprovalRequest approvalRequest = new ApprovalRequest(txn);
        approvalRequestRepository.save(approvalRequest);

        return txn;
    }

    // ===== PATH 3: employee/admin approved it — NOW actually execute =====
    @Transactional
    public Transactions executeApprovedTransfer(UUID transactionId) {

        Transactions txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        UUID fromAccountId = txn.getFromAccount().getId();
        UUID toAccountId = txn.getToAccount().getId();

        AccountPair pair = fetchAccountsInLockOrder(fromAccountId, toAccountId);

        return applyTransfer(txn, pair, txn.getAmount());
    }

    // ===== Shared internals — lock ordering =====
    private AccountPair fetchAccountsInLockOrder(UUID fromAccountId, UUID toAccountId) {

        UUID firstId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        UUID secondId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        Account firstAccount = accountRepository.findById(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account secondAccount = accountRepository.findById(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account fromAccount = fromAccountId.equals(firstId) ? firstAccount : secondAccount;
        Account toAccount = toAccountId.equals(firstId) ? firstAccount : secondAccount;

        return new AccountPair(firstAccount, secondAccount, fromAccount, toAccount);
    }

    // ===== Shared internals — the actual debit/credit + status transition =====
    private Transactions applyTransfer(Transactions txn, AccountPair pair, BigDecimal amount) {

        if (pair.fromAccount().getBalance().compareTo(amount) < 0) {
            txn.setStatus(Status.FAILED);
            txn.setFailureReason("Insufficient balance in account " + pair.fromAccount().getAccountNumber());
            txn.setCompletedAt(Instant.now());
            return transactionRepository.save(txn);
        }

        pair.fromAccount().setBalance(pair.fromAccount().getBalance().subtract(amount));
        pair.toAccount().setBalance(pair.toAccount().getBalance().add(amount));

        // Save in FIXED order (first, then second) — same deadlock prevention as before,
        // regardless of which path (immediate or post-approval) got us here.
        accountRepository.save(pair.firstAccount());
        accountRepository.save(pair.secondAccount());

        txn.setStatus(Status.SUCCESS);
        txn.setCompletedAt(Instant.now());
        return transactionRepository.save(txn);
    }

    private record AccountPair(Account firstAccount, Account secondAccount,
                               Account fromAccount, Account toAccount) {}

}