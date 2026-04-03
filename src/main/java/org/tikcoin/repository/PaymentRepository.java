package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tikcoin.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaystackReference(String reference);
    List<Payment> findAllByOrderByCreatedAtDesc();
}