package com.project.service;

import com.project.entity.Account;
import com.project.entity.AccountStatus;
import com.project.entity.AccountType;
import com.project.entity.UserEntity;
import com.project.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(UserEntity user, AccountType type) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(user, type, accountNumber);
        return accountRepository.save(account);
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<Account> getAccountsForUser(UUID userId) {
        return accountRepository.findByUserId(userId);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(1000000000L + new Random().nextInt(900000000));
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());
        return accountNumber;
    }
}