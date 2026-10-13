package com.bankevent.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** Used to make retries of the same HTTP request return the same payment. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
