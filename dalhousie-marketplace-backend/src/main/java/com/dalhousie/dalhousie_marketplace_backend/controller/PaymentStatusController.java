package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Payment;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentStatusController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusController.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/{paymentIntentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentIntentId) {
        logger.info("Checking payment status for payment intent: {}", paymentIntentId);

        try {
            // Using the autowired instance (this.paymentRepository), not the class name
            Optional<Payment> paymentOpt = this.paymentRepository.findByStripePaymentIntentId(paymentIntentId);

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();

                Map<String, Object> response = new HashMap<>();
                response.put("paymentStatus", payment.getPaymentStatus());
                response.put("orderId", payment.getOrderId());
                response.put("amount", payment.getAmount());
                response.put("updatedAt", payment.getUpdatedAt());

                logger.info("Found payment status: {} for order: {}", payment.getPaymentStatus(), payment.getOrderId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Payment not found for payment intent: {}", paymentIntentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payment not found for intent: " + paymentIntentId));
            }
        } catch (Exception e) {
            logger.error("Error retrieving payment status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}