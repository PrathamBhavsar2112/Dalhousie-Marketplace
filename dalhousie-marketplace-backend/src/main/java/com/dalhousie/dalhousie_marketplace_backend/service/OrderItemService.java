package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.Order;
import com.dalhousie.dalhousie_marketplace_backend.model.OrderItem;
import com.dalhousie.dalhousie_marketplace_backend.model.OrderStatus;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * Get all order items for a specific user
     */
    public List<OrderItem> getOrderItemsByUserId(Long userId) {
        // Use the repository method for efficiency
        return orderItemRepository.findByUserId(userId);
    }

    /**
     * Get all order items that are eligible for review by a user
     */
    public List<OrderItem> getEligibleOrderItemsForReview(Long userId) {
        List<OrderItem> userOrderItems = getOrderItemsByUserId(userId);

        return userOrderItems.stream()
                .filter(item -> isCompletedOrder(item))
                .filter(item -> isNotReviewed(userId, item))
                .collect(Collectors.toList());
    }

    private boolean isCompletedOrder(OrderItem item) {
        return item.getOrder().getOrderStatus() == OrderStatus.COMPLETED;
    }

    private boolean isNotReviewed(Long userId, OrderItem item) {
        return !reviewRepository.existsByUserIdAndOrderItemId(userId, item.getOrderItemId());
    }

    /**
     * Check if a user is eligible to review a specific order item
     */
    public boolean isEligibleToReview(Long userId, Long orderItemId) {
        Optional<OrderItem> orderItemOpt = orderItemRepository.findById(orderItemId);

        if (orderItemOpt.isEmpty()) {
            return false;
        }

        OrderItem orderItem = orderItemOpt.get();
        Long orderUserId = orderItem.getOrder().getUserId();
        OrderStatus orderStatus = orderItem.getOrder().getOrderStatus();

        boolean isUserMatch = orderUserId.equals(userId);
        boolean isCompleted = orderStatus == OrderStatus.COMPLETED;

        if (!isUserMatch || !isCompleted) {
            return false;
        }

        boolean alreadyReviewed = reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId);
        return !alreadyReviewed;
    }

    /**
     * Get an order item by ID
     */
    public Optional<OrderItem> getOrderItemById(Long orderItemId) {
        return orderItemRepository.findById(orderItemId);
    }
}