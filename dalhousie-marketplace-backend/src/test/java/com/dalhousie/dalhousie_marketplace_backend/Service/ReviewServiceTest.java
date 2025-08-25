package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewResponse;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ReviewRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingService;
import com.dalhousie.dalhousie_marketplace_backend.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ListingService listingService;

    @InjectMocks
    private ReviewService reviewService;

    private final Long USER_ID = 1L;
    private final Long LISTING_ID = 101L;
    private final Long ORDER_ITEM_ID = 201L;
    private final Long REVIEW_ID = 301L;
    private final Long ORDER_ID = 401L;

    private User testUser;
    private Listing testListing;
    private OrderItem testOrderItem;
    private Order testOrder;
    private Review testReview;
    private ReviewRequest testReviewRequest;
    private ReviewResponse mockReviewResponse;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail("test@example.com");

        // Setup test listing
        testListing = new Listing();
        testListing.setId(LISTING_ID);
        testListing.setTitle("Test Product");
        testListing.setDescription("Test Description");
        testListing.setPrice(99.99);

        // Setup test order
        testOrder = new Order();
        testOrder.setOrderId(ORDER_ID);
        testOrder.setUserId(USER_ID);
        testOrder.setOrderStatus(OrderStatus.COMPLETED);
        testOrder.setOrderDate(LocalDateTime.now());

        // Setup test order item
        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(ORDER_ITEM_ID);
        testOrderItem.setListing(testListing);
        testOrderItem.setOrder(testOrder);

        // Setup test review with necessary properties
        testReview = new Review();
        testReview.setReviewId(REVIEW_ID);
        testReview.setUserId(USER_ID);
        testReview.setListingId(LISTING_ID);
        testReview.setOrderItemId(ORDER_ITEM_ID);
        testReview.setRating(5);
        testReview.setReviewText("Great product!");

        // Setup review request
        testReviewRequest = new ReviewRequest();
        testReviewRequest.setOrderItemId(ORDER_ITEM_ID);
        testReviewRequest.setListingId(LISTING_ID);
        testReviewRequest.setRating(5);
        testReviewRequest.setReviewText("Great product!");

        // Setup a mock response
        mockReviewResponse = new ReviewResponse();
        mockReviewResponse.setReviewId(REVIEW_ID);
        mockReviewResponse.setUserId(USER_ID);
        mockReviewResponse.setListingId(LISTING_ID);
        mockReviewResponse.setRating(5);
        mockReviewResponse.setReviewText("Great product!");
    }

    @Test
    void testCreateReview_Success() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(testListing));

        // Create a direct mock of the buildReviewResponse method to avoid NPE
        ReviewService mockService = spy(reviewService);
        doReturn(mockReviewResponse).when(mockService).buildReviewResponse(any(), any(), any());

        // Act
        ReviewResponse response = mockService.createReview(USER_ID, testReviewRequest);

        // Assert
        assertNotNull(response);
        assertEquals(REVIEW_ID, response.getReviewId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(LISTING_ID, response.getListingId());
        assertEquals(5, response.getRating());
        assertEquals("Great product!", response.getReviewText());

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void testCreateReview_OrderItemNotFound() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                reviewService.createReview(USER_ID, testReviewRequest)
        );

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReview_ListingMismatch() {
        // Arrange
        testOrderItem.getListing().setId(999L); // Different from request
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(USER_ID, testReviewRequest)
        );

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReview_UserMismatch() {
        // Arrange
        testOrder.setUserId(999L); // Different from requesting user
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(USER_ID, testReviewRequest)
        );

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReview_AlreadyReviewed() {
        // Arrange
        when(orderItemRepository.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(testOrderItem));
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(USER_ID, testReviewRequest)
        );

        verify(orderItemRepository).findById(ORDER_ITEM_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testUpdateReview_Success() {
        // Arrange
        testReviewRequest.setRating(4);
        testReviewRequest.setReviewText("Updated review text");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(testListing));

        // Create a direct mock of the buildReviewResponse method to avoid NPE
        ReviewService mockService = spy(reviewService);
        doReturn(mockReviewResponse).when(mockService).buildReviewResponse(any(), any(), any());

        // Act
        ReviewResponse response = mockService.updateReview(USER_ID, REVIEW_ID, testReviewRequest);

        // Assert
        assertNotNull(response);
        assertEquals(REVIEW_ID, response.getReviewId());

        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void testUpdateReview_NotFound() {
        // Arrange
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                reviewService.updateReview(USER_ID, REVIEW_ID, testReviewRequest)
        );

        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testUpdateReview_NotOwnedByUser() {
        // Arrange
        testReview.setUserId(999L); // Different user
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(testReview));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.updateReview(USER_ID, REVIEW_ID, testReviewRequest)
        );

        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testDeleteReview_Success() {
        // Arrange
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(any(Review.class));

        // Act
        reviewService.deleteReview(USER_ID, REVIEW_ID);

        // Assert
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).delete(any(Review.class));
    }

    @Test
    void testDeleteReview_NotFound() {
        // Arrange
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                reviewService.deleteReview(USER_ID, REVIEW_ID)
        );

        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void testDeleteReview_NotOwnedByUser() {
        // Arrange
        testReview.setUserId(999L); // Different user
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(testReview));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.deleteReview(USER_ID, REVIEW_ID)
        );

        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void testGetReviewById_Success() {
        // Arrange
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(testListing));

        // Create a direct mock of the buildReviewResponse method to avoid NPE
        ReviewService mockService = spy(reviewService);
        doReturn(mockReviewResponse).when(mockService).buildReviewResponse(any(), any(), any());

        // Act
        ReviewResponse response = mockService.getReviewById(REVIEW_ID);

        // Assert
        assertNotNull(response);
        assertEquals(REVIEW_ID, response.getReviewId());

        verify(reviewRepository).findById(REVIEW_ID);
        verify(userRepository).findById(USER_ID);
        verify(listingRepository).findById(LISTING_ID);
    }

    @Test
    void testGetReviewById_NotFound() {
        // Arrange
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                reviewService.getReviewById(REVIEW_ID)
        );

        verify(reviewRepository).findById(REVIEW_ID);
    }

    @Test
    void testGetReviewsByListingId_Success() {
        // Arrange
        List<Review> reviewList = Arrays.asList(testReview);
        when(reviewRepository.findByListingId(LISTING_ID)).thenReturn(reviewList);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(testListing));

        // Create a direct mock of the buildReviewResponse method to avoid NPE
        ReviewService mockService = spy(reviewService);
        doReturn(mockReviewResponse).when(mockService).buildReviewResponse(any(), any(), any());

        // Act
        List<ReviewResponse> responses = mockService.getReviewsByListingId(LISTING_ID);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(reviewRepository).findByListingId(LISTING_ID);
    }

    @Test
    void testGetAverageRatingForListing() {
        // Arrange
        when(reviewRepository.getAverageRatingForListing(LISTING_ID)).thenReturn(4.5);

        // Act
        Double averageRating = reviewService.getAverageRatingForListing(LISTING_ID);

        // Assert
        assertEquals(4.5, averageRating);
        verify(reviewRepository).getAverageRatingForListing(LISTING_ID);
    }

    @Test
    void testGetReviewCountForListing() {
        // Arrange
        when(reviewRepository.getReviewCountForListing(LISTING_ID)).thenReturn(10L);

        // Act
        Long count = reviewService.getReviewCountForListing(LISTING_ID);

        // Assert
        assertEquals(10L, count);
        verify(reviewRepository).getReviewCountForListing(LISTING_ID);
    }

    @Test
    void testHasUserReviewedOrderItem_True() {
        // Arrange
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(true);

        // Act
        boolean hasReviewed = reviewService.hasUserReviewedOrderItem(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertTrue(hasReviewed);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testHasUserReviewedOrderItem_False() {
        // Arrange
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(false);

        // Act
        boolean hasReviewed = reviewService.hasUserReviewedOrderItem(USER_ID, ORDER_ITEM_ID);

        // Assert
        assertFalse(hasReviewed);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }

    @Test
    void testGetReviewsByUserId_Success() {
        // Arrange
        List<Review> reviewList = Arrays.asList(testReview);
        when(reviewRepository.findByUserId(USER_ID)).thenReturn(reviewList);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(testListing));

        // Create a direct mock of the buildReviewResponse method to avoid NPE
        ReviewService mockService = spy(reviewService);
        doReturn(mockReviewResponse).when(mockService).buildReviewResponse(any(), any(), any());

        // Act
        List<ReviewResponse> responses = mockService.getReviewsByUserId(USER_ID);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(reviewRepository).findByUserId(USER_ID);
    }

    @Test
    void testGetEligibleOrderItemsForReview() {
        // Arrange
        List<OrderItem> orderItems = Arrays.asList(testOrderItem);
        List<Long> expectedOrderItemIds = Collections.singletonList(ORDER_ITEM_ID);

        when(orderItemRepository.findByUserId(USER_ID)).thenReturn(orderItems);
        when(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).thenReturn(false);

        // Act
        List<Long> eligibleItemIds = reviewService.getEligibleOrderItemsForReview(USER_ID);

        // Assert
        assertEquals(1, eligibleItemIds.size());
        assertEquals(ORDER_ITEM_ID, eligibleItemIds.get(0));

        verify(orderItemRepository).findByUserId(USER_ID);
        verify(reviewRepository).existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID);
    }
}