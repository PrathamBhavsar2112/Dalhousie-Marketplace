package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Order;
import com.dalhousie.dalhousie_marketplace_backend.model.OrderItem;
import com.dalhousie.dalhousie_marketplace_backend.model.Payment;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Order not found for ID: " + orderId);
        }
    }

    @PostMapping("/cart/{userId}")
    public ResponseEntity<Order> convertCartToOrder(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.createOrderFromCart(userId));
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItemsByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Get the order with items - using the autowired instance
            Order order = orderRepository.findOrderWithItems(orderId);

            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }

            // Extract token and validate user access
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Long tokenUserId = jwtUtil.getUserIdFromUsername(jwtUtil.extractUsername(token));

            // Check if the user making the request is the owner of the order
            if (!order.getUserId().equals(tokenUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to access this order"));
            }

            // If we reach here, the user is authorized to see the order items
            return ResponseEntity.ok(order.getItems());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving order items: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Retrieving order history for user: {}", userId);

            // Extract token and verify user access
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Long tokenUserId = jwtUtil.extractUserId(token);

            // Check if the user making the request is authorized
            if (!userId.equals(tokenUserId)) {
                logger.warn("Unauthorized access attempt to order history for user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to access this user's order history"));
            }

            // Get all orders for this user
            List<Order> userOrders = orderRepository.findByUserId(userId);

            if (userOrders.isEmpty()) {
                logger.info("No orders found for user: {}", userId);
                return ResponseEntity.ok(List.of());
            }

            // Get all order IDs
            List<Long> orderIds = userOrders.stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());

            // Fetch all payments for these orders
            List<Payment> payments = paymentRepository.findByOrderIdIn(orderIds);

            // Create a map of orderId to payment for quick lookup
            Map<Long, Payment> paymentMap = payments.stream()
                    .collect(Collectors.toMap(
                            Payment::getOrderId,
                            payment -> payment,
                            (existing, replacement) -> existing
                    ));

            // Map to a combined response object
            List<Map<String, Object>> orderHistory = userOrders.stream()
                    .map(order -> {
                        Map<String, Object> orderData = new HashMap<>();

                        // Order details
                        orderData.put("orderId", order.getOrderId());
                        orderData.put("amount", order.getTotalPrice());
                        orderData.put("orderStatus", order.getOrderStatus());
                        orderData.put("orderDate", order.getOrderDate());

                        // Payment details (if available)
                        Payment payment = paymentMap.get(order.getOrderId());
                        if (payment != null) {
                            orderData.put("paymentId", payment.getId());
                            orderData.put("paymentMethod", payment.getPaymentMethod());
                            orderData.put("currency", payment.getCurrency());

                            // Only add receipt URL if available
                            if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isEmpty()) {
                                orderData.put("receiptUrl", payment.getReceiptUrl());
                            }

                            orderData.put("updatedAt", payment.getUpdatedAt());
                        } else {
                            // Default values if no payment found
                            orderData.put("paymentMethod", "PENDING");
                            orderData.put("currency", "CAD");
                            orderData.put("updatedAt", order.getOrderDate());
                        }

                        return orderData;
                    })
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved {} order records for user {}",
                    orderHistory.size(), userId);
            return ResponseEntity.ok(orderHistory);

        } catch (Exception e) {
            logger.error("Error retrieving order history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve order history: " + e.getMessage()));
        }
    }
}