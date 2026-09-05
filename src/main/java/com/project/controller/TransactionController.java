package com.project.controller;


import com.project.dto.TransactionRequest;
import com.project.dto.TransactionResponse;
import com.project.dto.TransferRequest;
import com.project.entity.Transactions;
import com.project.entity.UserEntity;
import com.project.repository.UserRepository;
import com.project.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/{accountId}")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity user = resolveUser(currentUser);
        Transactions txn = transactionService.deposit(accountId, request.amount(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.fromEntity(txn));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity user = resolveUser(currentUser);
        Transactions txn = transactionService.withdraw(accountId, request.amount(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.fromEntity(txn));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity user = resolveUser(currentUser);
        Transactions txn = transactionService.transfer(
                accountId, request.toAccountId(), request.amount(), user, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.fromEntity(txn));
    }

    private UserEntity resolveUser(UserDetails currentUser) {
        return userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}