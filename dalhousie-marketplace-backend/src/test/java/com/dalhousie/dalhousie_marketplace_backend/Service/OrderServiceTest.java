package com.dalhousie.dalhousie_marketplace_backend.Service;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.CartRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for OrderService with single assert per method and ~85%+ coverage
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    private Cart mockCart;
    private Order mockOrder;
    private CartItem cartItem;
    private Listing mockListing;

    @BeforeEach
    void setUp() {
        mockListing = new Listing();
        mockListing.setId(1L);
        mockListing.setPrice(10.0);

        cartItem = new CartItem();
        cartItem.setListing(mockListing);
        cartItem.setQuantity(2);
        cartItem.setPrice(BigDecimal.valueOf(20.0));

        mockCart = new Cart();
        mockCart.setUserId(1L);
        mockCart.setCartItems(Collections.singletonList(cartItem));
        mockCart.setTotalPrice(BigDecimal.valueOf(20.0));

        mockOrder = new Order();
        mockOrder.setOrderId(1L);
        mockOrder.setUserId(1L);
        mockOrder.setTotalPrice(BigDecimal.valueOf(20.0));
        mockOrder.setOrderStatus(OrderStatus.PENDING);
        mockOrder.setOrderDate(LocalDateTime.now());
    }

    // getOrderById
    @Test
    void getOrderById_ReturnsOrderWhenFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        Order result = orderService.getOrderById(1L);

        assertNotNull(result);
    }

    @Test
    void getOrderById_ThrowsExceptionWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(999L);
        });

        assertEquals("Order not found", exception.getMessage());
    }

    // createOrderFromCart
    @Test
    void createOrderFromCart_ReturnsOrderWhenCartExists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(new OrderItem()));

        Order result = orderService.createOrderFromCart(1L);

        assertNotNull(result);
    }

    @Test
    void createOrderFromCart_SetsPendingStatus() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(new OrderItem()));

        Order result = orderService.createOrderFromCart(1L);

        assertEquals(OrderStatus.PENDING, result.getOrderStatus());
    }

    @Test
    void createOrderFromCart_ThrowsExceptionWhenCartNotFound() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrderFromCart(1L);
        });

        assertEquals("Cart not found for user ID: 1", exception.getMessage());
    }

    @Test
    void createOrderFromCart_ThrowsExceptionWhenCartEmpty() {
        Cart emptyCart = new Cart();
        emptyCart.setUserId(1L);
        emptyCart.setCartItems(Collections.emptyList());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrderFromCart(1L);
        });

        assertEquals("Cart is empty. Cannot place order.", exception.getMessage());
    }

    @Test
    void createOrderFromCart_DeletesCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(new OrderItem()));

        orderService.createOrderFromCart(1L);

        verify(cartRepository, times(1)).delete(mockCart);
    }

    @Test
    void createOrderFromCart_SavesOrderItems() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(new OrderItem()));

        orderService.createOrderFromCart(1L);

        verify(orderItemRepository, times(1)).saveAll(anyList());
    }

    // updateOrder
    @Test
    void updateOrder_ReturnsUpdatedOrder() {
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(1L);
        updatedOrder.setOrderStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        Order result = orderService.updateOrder(updatedOrder);

        assertEquals(OrderStatus.COMPLETED, result.getOrderStatus());
    }

    @Test
    void updateOrder_ThrowsExceptionWhenOrderNotFound() {
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(999L);
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrder(updatedOrder);
        });

        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void updateOrder_SavesChanges() {
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(1L);
        updatedOrder.setOrderStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        orderService.updateOrder(updatedOrder);

        verify(orderRepository, times(1)).save(any(Order.class));
    }
}