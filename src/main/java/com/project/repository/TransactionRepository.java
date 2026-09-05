package com.project.repository;

import com.project.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transactions, UUID> {

    Optional<Transactions> findByIdempotencyKey(String idempotencyKey);

}