package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Order;
import com.dalhousie.dalhousie_marketplace_backend.model.Payment;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long orderId) {
        try {
            logger.info("Initiating payment process for order: {}", orderId);

            // Validate order exists
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                logger.warn("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found: " + orderId));
            }

            // Validate order status
            if (order.getOrderStatus() == null) {
                logger.error("Invalid order status for order: {}", orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid order status"));
            }

            // Create Stripe checkout session
            String checkoutUrl = paymentService.createCheckoutSession(order);

            if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                logger.error("Failed to generate checkout URL for order: {}", orderId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to generate checkout URL"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", checkoutUrl);

            logger.info("Successfully created checkout session for order: {}", orderId);
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            logger.error("Stripe error processing order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment processing error: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long orderId) {
        try {
            logger.info("Checking payment status for order: {}", orderId);

            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                logger.warn("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }

            // Get payment details including receipt URL
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            Map<String, Object> response = new HashMap<>();

            response.put("orderStatus", order.getOrderStatus().toString());

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                response.put("paymentStatus", payment.getPaymentStatus().toString());

                // Include receipt URL if available
                if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isEmpty()) {
                    response.put("receiptUrl", payment.getReceiptUrl());
                }
            }

            logger.info("Successfully retrieved payment status for order: {}", orderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking payment status for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking payment status: " + e.getMessage()));
        }
    }

    @GetMapping("/payments/{paymentIntentId}/status")
    public ResponseEntity<?> getPaymentIntentStatus(@PathVariable String paymentIntentId) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("paymentStatus", payment.getPaymentStatus());
                response.put("orderId", payment.getOrderId());
                response.put("amount", payment.getAmount());
                response.put("updatedAt", payment.getUpdatedAt());

                // Include receipt URL if available
                if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isEmpty()) {
                    response.put("receiptUrl", payment.getReceiptUrl());
                }

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payment not found for payment intent: " + paymentIntentId));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/payments")
    public ResponseEntity<?> getUserPayments(@PathVariable Long userId,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Retrieving payment history for user: {}", userId);

            // Extract token and verify user access
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            if (!jwtUtil.validateUserAccess(token, userId)) {
                logger.warn("Unauthorized access attempt to payment history for user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to access this user's payment history"));
            }

            // Get all orders for this user
            List<Order> userOrders = orderRepository.findByUserId(userId);

            if (userOrders.isEmpty()) {
                logger.info("No orders found for user: {}", userId);
                return ResponseEntity.ok(List.of());
            }

            // Get order IDs
            List<Long> orderIds = userOrders.stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());

            // Fetch all payments for these orders
            List<Payment> payments = paymentRepository.findByOrderIdIn(orderIds);

            // Map to a simplified response object
            List<Map<String, Object>> paymentHistory = payments.stream()
                    .map(payment -> {
                        Map<String, Object> paymentData = new HashMap<>();
                        Order order = userOrders.stream()
                                .filter(o -> o.getOrderId().equals(payment.getOrderId()))
                                .findFirst()
                                .orElse(null);

                        paymentData.put("paymentId", payment.getId());
                        paymentData.put("orderId", payment.getOrderId());
                        paymentData.put("amount", payment.getAmount());
                        paymentData.put("currency", payment.getCurrency());
                        paymentData.put("status", payment.getPaymentStatus());
                        paymentData.put("paymentMethod", payment.getPaymentMethod());
                        paymentData.put("createdAt", payment.getCreatedAt());
                        paymentData.put("updatedAt", payment.getUpdatedAt());

                        // Include receipt URL if available
                        if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isEmpty()) {
                            paymentData.put("receiptUrl", payment.getReceiptUrl());
                        }

                        // Add order status if order is available
                        if (order != null) {
                            paymentData.put("orderStatus", order.getOrderStatus());
                            paymentData.put("orderDate", order.getOrderDate());
                        }

                        // Add failure reason if payment failed
                        if (payment.getPaymentStatus() == Payment.PaymentStatus.FAILED &&
                                payment.getFailureReason() != null) {
                            paymentData.put("failureReason", payment.getFailureReason());
                        }

                        return paymentData;
                    })
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved {} payment records for user {}",
                    paymentHistory.size(), userId);
            return ResponseEntity.ok(paymentHistory);

        } catch (Exception e) {
            logger.error("Error retrieving payment history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve payment history: " + e.getMessage()));
        }
    }
}
