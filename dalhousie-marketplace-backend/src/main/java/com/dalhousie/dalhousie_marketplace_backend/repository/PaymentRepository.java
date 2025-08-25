package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);
    List<Payment> findByOrderIdIn(List<Long> orderIds);
}