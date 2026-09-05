package com.project.service;

import com.project.entity.*;
import com.project.repository.AccountRepository;
import com.project.repository.TransactionRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.service.TransferExecutor;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransferExecutor transferExecutor;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_RETRIES = 3;

    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("50000");

    public TransactionService(TransferExecutor transferExecutor, TransferExecutor transferExecutor1, TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transferExecutor = transferExecutor;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Transactions deposit(UUID accountId, BigDecimal amount, UserEntity user) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Transactions txn = new Transactions(Type.DEPOSIT, null, account, amount, user, null);
        txn = transactionRepository.save(txn);

        try {
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);

            txn.setStatus(Status.SUCCESS);
            txn.setCompletedAt(java.time.Instant.now());

        } catch (Exception e) {
            txn.setStatus(Status.FAILED);
            txn.setFailureReason(e.getMessage());
            throw e;
        }

        return transactionRepository.save(txn);
    }

    @Transactional
    public Transactions withdraw(UUID accountId, BigDecimal amount, UserEntity user){
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        Transactions txn = new Transactions(Type.WITHDRAWAL,null ,account , amount, user, null);
        transactionRepository.save(txn);

        if(account.getBalance().compareTo(amount) < 0){
            txn.setStatus(Status.FAILED);
            txn.setFailureReason("Insufficient balance in account " + account.getAccountNumber());
            txn.setCompletedAt(java.time.Instant.now());
            return transactionRepository.save(txn);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        txn.setStatus(Status.SUCCESS);
        txn.setCompletedAt(java.time.Instant.now());

        return transactionRepository.save(txn);
    }

    public Transactions transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, UserEntity user, String idempotencyKey){
        if(idempotencyKey != null){
            var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if(existing.isPresent()){
                return existing.get();
            }
        }

        if (amount.compareTo(APPROVAL_THRESHOLD) >= 0) {
            return transferExecutor.createApprovalRequest(fromAccountId, toAccountId, amount, user, idempotencyKey);
        }

        int attempt=0;
        while(true){
            try{
                return transferExecutor.executeTransfer(fromAccountId, toAccountId, amount, user, idempotencyKey);
            }
            catch(ObjectOptimisticLockingFailureException e){
                attempt++;
                if(attempt >= MAX_RETRIES){
                    throw new RuntimeException("Transfer failed after "+ MAX_RETRIES + "attempts due to concurrent updates ", e);
                }

                try{
                    Thread.sleep(100L * attempt);
                }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transfer retry interrupted", interrupted);
                }
            }
        }
    }

}
