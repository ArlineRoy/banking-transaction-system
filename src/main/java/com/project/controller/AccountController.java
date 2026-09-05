package com.project.controller;

import com.project.dto.AccountResponse;
import com.project.dto.CreateAccountRequest;
import com.project.entity.Account;
import com.project.entity.UserEntity;
import com.project.repository.UserRepository;
import com.project.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    public AccountController(AccountService accountService, UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity user = resolveUser(currentUser);
        Account account = accountService.createAccount(user, request.getAccountType());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.fromEntity(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(AccountResponse.fromEntity(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listMyAccounts(
            @AuthenticationPrincipal UserDetails currentUser) {

        UserEntity user = resolveUser(currentUser);
        List<AccountResponse> accounts = accountService.getAccountsForUser(user.getId())
                .stream()
                .map(AccountResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    private UserEntity resolveUser(UserDetails currentUser) {
        return userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}