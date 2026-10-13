package com.example.SmartPark.repository;

import com.example.SmartPark.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySessionId(Long sessionId);
    List<Payment> findByStatus(Payment.Status status);
}