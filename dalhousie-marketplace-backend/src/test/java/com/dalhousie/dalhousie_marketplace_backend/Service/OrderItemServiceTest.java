package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ReviewRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private final Long USER_ID = 1L;
    private final Long LISTING_ID = 101L;
    private final Long ORDER_ITEM_ID = 201L;
    private final Long ORDER_ID = 301L;

    private OrderItem completedOrderItem;
    private OrderItem pendingOrderItem;
    private Order completedOrder;
    private Order pendingOrder;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        // Setup test listing
        testListing = new Listing();
        testListing.setId(LISTING_ID);
        testListing.setTitle("Test Product");
        testListing.setPrice(99.99);

        // Setup completed order
        completedOrder = new Order();
        completedOrder.setOrderId(ORDER_ID);
        completedOrder.setUserId(USER_ID);
        completedOrder.setOrderStatus(OrderStatus.COMPLETED);
        completedOrder.setOrderDate(LocalDateTime.now().minusDays(7)); // 1 week old order

        // Setup pending order
        pendingOrder = new Order();
        pendingOrder.setOrderId(ORDER_ID + 1);
        pendingOrder.setUserId(USER_ID);
        pendingOrder.setOrderStatus(OrderStatus.PENDING);
        pendingOrder.setOrderDate(LocalDateTime.now());

        // Setup completed order item
        completedOrderItem = new OrderItem();
        completedOrderItem.setOrderItemId(ORDER_ITEM_ID);
        completedOrderItem.setListing(testListing);
        completedOrderItem.setOrder(completedOrder);

        // Setup pending order item
        pendingOrderItem = new OrderItem();
        pendingOrderItem.setOrderItemId(ORDER_ITEM_ID + 1);
        pendingOrderItem.setListing(testListing);
        pendingOrderItem.setOrder(pendingOrder);
    }

    @Test
    void testGetOrderItemsByUserId() {
        // Arrange
        List<OrderItem> expectedOrderItems = Arrays.asList(completedOrderItem, pendingOrderItem);
        when(orderItemRepository.findByUserId(USER_ID)).thenReturn(expectedOrderItems);

        // Act
        List<OrderItem> result = orderItemService.getOrderItemsByUserId(USER_ID);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(completedOrderItem));
        assertTrue(result.contains(pendingOrderItem));
        verify(orderItemRepository).findByUserId(USER_ID);
    }

    @Test
    void testGetEligibleOrderItemsForReview() {
        // Arrange
        List<OrderItem> allUserOrderItems = Arrays.asList(completedOrderItem, pendingOrderItem);
        when(orderItemRepository.findByUserId(USER_ID)).thenReturn(allUserOrderItems);

        // Not yet reviewed
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(false);

        // Act
        List<OrderItem> eligibleItems = orderItemService.getEligibleOrderItemsForReview(USER_ID);

        // Assert
        assertEquals(1, eligibleItems.size());
        assertEquals(ORDER_ITEM_ID, eligibleItems.get(0).getOrderItemId());
        assertEquals(LISTING_ID, eligibleItems.get(0).getListing().getId());

        verify(orderItemRepository).findByUserId(USER_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testGetEligibleOrderItemsForReview_EmptyWhenAllAlreadyReviewed() {
        // Arrange
        List<OrderItem> allUserOrderItems = Collections.singletonList(completedOrderItem);
        when(orderItemRepository.findByUserId(USER_ID)).thenReturn(allUserOrderItems);

        // Already reviewed
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(true);

        // Act
        List<OrderItem> eligibleItems = orderItemService.getEligibleOrderItemsForReview(USER_ID);

        // Assert
        assertTrue(eligibleItems.isEmpty());

        verify(orderItemRepository).findByUserId(USER_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testIsEligibleToReview_Success() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(completedOrderItem));
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(false);

        // Act
        boolean isEligible = orderItemService.isEligibleToReview(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertTrue(isEligible);

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testIsEligibleToReview_NotFoundOrderItem() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.empty());

        // Act
        boolean isEligible = orderItemService.isEligibleToReview(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertFalse(isEligible);

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository, never()).existsByUserIdAndOrderItemId(any(), any());
    }

    @Test
    void testIsEligibleToReview_DifferentUser() {
        // Arrange
        completedOrder.setUserId(999L); // Different user
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(completedOrderItem));

        // Act
        boolean isEligible = orderItemService.isEligibleToReview(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertFalse(isEligible);

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository, never()).existsByUserIdAndOrderItemId(any(), any());
    }

    @Test
    void testIsEligibleToReview_PendingOrder() {
        // Arrange
        when(orderItemRepository.findById(pendingOrderItem.getOrderItemId())).thenReturn(Optional.of(pendingOrderItem));

        // Act
        boolean isEligible = orderItemService.isEligibleToReview(USER_ID, pendingOrderItem.getOrderItemId());

        // Assert
        assertFalse(isEligible);

        verify(orderItemRepository).findById(pendingOrderItem.getOrderItemId());
        verify(reviewRepository, never()).existsByUserIdAndOrderItemId(any(), any());
    }

    @Test
    void testIsEligibleToReview_AlreadyReviewed() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(completedOrderItem));
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(true);

        // Act
        boolean isEligible = orderItemService.isEligibleToReview(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertFalse(isEligible);

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testGetOrderItemById() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(completedOrderItem));

        // Act
        Optional<OrderItem> result = orderItemService.getOrderItemById(ORDER_ITEM_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(ORDER_ITEM_ID, result.get().getOrderItemId());

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
    }
}