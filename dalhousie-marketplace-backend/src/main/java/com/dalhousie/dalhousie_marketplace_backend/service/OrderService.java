package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order createOrderFromCart(Long userId) {
        Cart cart = fetchUserCart(userId);

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        Order order = createBaseOrder(userId, cart.getTotalPrice());
        List<OrderItem> orderItems = mapCartToOrderItems(cart, order);

        orderItemRepository.saveAll(orderItems);
        cartRepository.delete(cart);

        order.setItems(orderItems);
        return order;
    }

    @Transactional
    public Order updateOrder(Order order) {
        Order existing = getOrderById(order.getOrderId());
        existing.setOrderStatus(order.getOrderStatus());
        return orderRepository.save(existing);
    }

    private Cart fetchUserCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user ID: " + userId));
    }

    private Order createBaseOrder(Long userId, java.math.BigDecimal totalPrice) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice(totalPrice);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private List<OrderItem> mapCartToOrderItems(Cart cart, Order order) {
        return cart.getCartItems().stream().map(cartItem -> {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setListing(cartItem.getListing());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getPrice());
            return item;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
    Order order = getOrderById(orderId);
    order.setOrderStatus(status);
    updateOrder(order);  
    return order;
}
}
