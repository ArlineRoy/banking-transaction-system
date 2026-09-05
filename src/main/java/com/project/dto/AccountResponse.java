package com.project.dto;

import com.project.entity.Account;
import com.project.entity.AccountStatus;
import com.project.entity.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// AccountResponse.java
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private Instant createdAt;

    // constructor that maps from Account entity, getters

    public AccountResponse(){
    }

    public static AccountResponse fromEntity(Account account) {
        AccountResponse accRes = new AccountResponse();
        accRes.id = account.getId();
        accRes.accountNumber = account.getAccountNumber();
        accRes.accountType = account.getAccountType();
        accRes.balance = account.getBalance();
        accRes.createdAt = account.getCreatedAt();
        accRes.status = account.getStatus();
        return accRes;
    }

    public AccountResponse(Account account){
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.accountType = account.getAccountType();
        this.balance = account.getBalance();
        this.createdAt = account.getCreatedAt();
        this.status = account.getStatus();
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}