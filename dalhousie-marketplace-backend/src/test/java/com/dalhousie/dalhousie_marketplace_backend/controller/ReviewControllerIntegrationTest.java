package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewEligibilityResponse;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewResponse;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.Order;
import com.dalhousie.dalhousie_marketplace_backend.model.OrderItem;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderItemService;
import com.dalhousie.dalhousie_marketplace_backend.service.ReviewService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReviewControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private ReviewController reviewController;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();

        // Mock a valid JWT token
        validJwtToken = "valid.jwt.token.here";  // Replace with actual valid JWT token logic
    }

    @Test
    void testCreateReview_Success() throws Exception {
        // Create a new instance of ReviewRequest using setters
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setOrderItemId(1L);
        reviewRequest.setRating(5);
        reviewRequest.setReviewText("Great product!");
        reviewRequest.setListingId(1L);  // Make sure the listingId is set

        // Create a new instance of ReviewResponse using setters (since no constructor with arguments)
        ReviewResponse reviewResponse = new ReviewResponse();
        reviewResponse.setReviewId(1L);  // Assuming there's a setter for reviewId
        reviewResponse.setListingId(1L);  // Assuming there's a setter for listingId
        reviewResponse.setRating(5);
        reviewResponse.setReviewText("Great product!");

        when(reviewService.createReview(anyLong(), any(ReviewRequest.class))).thenReturn(reviewResponse);

        // Simulate API call
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType("application/json")
                        .content("{\"orderItemId\":1, \"rating\":5, \"reviewText\":\"Great product!\", \"listingId\": 1}")) // Include listingId in the request
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Great product!")); // Adjusted field name
    }


    @Test
    void testCreateReview_Unauthorized() throws Exception {
        // Simulate a POST request without the Authorization header
        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content("{\"orderItemId\":1, \"rating\":5, \"reviewText\":\"Great product!\", \"listingId\":1}"))
                .andExpect(status().isBadRequest())  // Expecting 400 Bad Request instead of 401 Unauthorized
                .andExpect(jsonPath("$").doesNotExist());  // Expect no value in the response body
    }
    @Test
    void testCreateReview_Conflict() throws Exception {
        // Create a new instance of ReviewRequest using setters
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setOrderItemId(1L);
        reviewRequest.setRating(5);
        reviewRequest.setReviewText("Great product!");
        reviewRequest.setListingId(1L);  // Add listingId to the request

        // Simulate that the user has already reviewed the order item
        when(reviewService.hasUserReviewedOrderItem(anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType("application/json")
                        .content("{\"orderItemId\":1, \"rating\":5, \"reviewText\":\"Great product!\", \"listingId\":1}"))  // Include listingId
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("You have already reviewed this purchase"));
    }


    @Test
    void testUpdateReview_Success() throws Exception {
        // Create a new instance of ReviewRequest using setters
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setOrderItemId(1L);
        reviewRequest.setRating(5);
        reviewRequest.setReviewText("Great product!");
        reviewRequest.setListingId(1L); // Add listingId to the request

        // Mock updated review response
        // Create a new instance of ReviewResponse using setters (since no constructor with arguments)
        ReviewResponse reviewResponse = new ReviewResponse();
        reviewResponse.setReviewId(1L); // Assuming there's a setter for reviewId
        reviewResponse.setListingId(1L); // Assuming there's a setter for listingId
        reviewResponse.setRating(5);
        reviewResponse.setReviewText("Updated review");

        // Simulate the update review service response
        when(reviewService.updateReview(anyLong(), anyLong(), any(ReviewRequest.class))).thenReturn(reviewResponse);

        // Perform the test with the updated request body including listingId
        mockMvc.perform(put("/api/reviews/{reviewId}", 1L)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType("application/json")
                        .content("{\"orderItemId\":1, \"rating\":5, \"reviewText\":\"Updated review\", \"listingId\":1}"))  // Include listingId
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Updated review"));
    }


    @Test
    void testDeleteReview_Success() throws Exception {
        mockMvc.perform(delete("/api/reviews/{reviewId}", 1L)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isNoContent());

        verify(reviewService, times(1)).deleteReview(anyLong(), anyLong());
    }

    @Test
    void testGetReviewById_Success() throws Exception {
        // Mock review response
        ReviewResponse reviewResponse = new ReviewResponse();
        reviewResponse.setReviewId(1L); // Assuming there's a setter for reviewId
        reviewResponse.setListingId(1L); // Assuming there's a setter for listingId
        reviewResponse.setRating(5);
        reviewResponse.setReviewText("Great product!"); // Adjusted field name to match ReviewResponse

        when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);

        mockMvc.perform(get("/api/reviews/{reviewId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Great product!")); // Adjusted field name
    }


    @Test
    void testGetReviewsByListingId_Success() throws Exception {
        // Mock reviews list and response
        // Create a new instance of ReviewResponse using setters (since no constructor with arguments)
        ReviewResponse reviewResponse1 = new ReviewResponse();
        reviewResponse1.setReviewId(1L); // Assuming there's a setter for reviewId
        reviewResponse1.setListingId(1L); // Assuming there's a setter for orderItemId
        reviewResponse1.setRating(5);
        reviewResponse1.setReviewText("Great product!");
        // Create a new instance of ReviewResponse using setters (since no constructor with arguments)
        ReviewResponse reviewResponse2 = new ReviewResponse();
        reviewResponse2.setReviewId(2L); // Assuming there's a setter for reviewId
        reviewResponse2.setListingId(1L); // Assuming there's a setter for orderItemId
        reviewResponse2.setRating(5);
        reviewResponse2.setReviewText("Great product!");
        List<ReviewResponse> reviews = List.of(reviewResponse1, reviewResponse2);

        when(reviewService.getReviewsByListingId(1L)).thenReturn(reviews);
        when(reviewService.getAverageRatingForListing(1L)).thenReturn(4.5);
        when(reviewService.getReviewCountForListing(1L)).thenReturn(2L);

        mockMvc.perform(get("/api/reviews/listing/{listingId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews.size()").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));
    }

    @Test
    void testGetUserReviews_Success() throws Exception {
        // Mock user reviews
        ReviewResponse reviewResponse1 = new ReviewResponse();
        reviewResponse1.setReviewId(1L); // Assuming there's a setter for reviewId
        reviewResponse1.setListingId(1L); // Assuming there's a setter for orderItemId
        reviewResponse1.setRating(5);
        reviewResponse1.setReviewText("Great product!");  // Correct the field to reviewText
        List<ReviewResponse> reviews = List.of(reviewResponse1);

        when(reviewService.getReviewsByUserId(anyLong())).thenReturn(reviews);

        mockMvc.perform(get("/api/reviews/user")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].reviewText").value("Great product!"));  // Correct field name here
    }


    @Test
    void testGetEligibleItemsForReview_Success() throws Exception {
        // Mock eligible items for review
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(1L);

        // Set the Listing
        Listing listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Item 1");
        listing.setPrice(100.0);
        orderItem.setListing(listing);

        // Set the Order with a non-null orderDate
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderDate(LocalDateTime.now());  // Ensure the orderDate is not null
        orderItem.setOrder(order);

        List<OrderItem> eligibleItems = List.of(orderItem);

        when(orderItemService.getEligibleOrderItemsForReview(anyLong())).thenReturn(eligibleItems);

        mockMvc.perform(get("/api/reviews/eligible-items")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleItems.size()").value(1))  // Adjusted path
                .andExpect(jsonPath("$.eligibleItems[0].listingTitle").value("Item 1"))  // Adjusted path to listingTitle
                .andExpect(jsonPath("$.eligibleItems[0].price").value(100.0));  // Adjusted path to price
    }

    @Test
    void testCheckReviewEligibilityForListing_Success() throws Exception {
        // Mock the OrderItem with a non-null Listing and Order
        OrderItem orderItem = new OrderItem();

        Listing listing = new Listing();
        listing.setId(1L);  // Set the ID for Listing
        orderItem.setListing(listing);  // Set the Listing in OrderItem

        Order order = new Order();
        order.setOrderId(1L);  // Set the ID for Order
        orderItem.setOrder(order);  // Set the Order in OrderItem

        // Mock the eligibility check
        when(orderItemService.getEligibleOrderItemsForReview(anyLong())).thenReturn(List.of(orderItem));

        mockMvc.perform(get("/api/reviews/eligibility/listing/{listingId}", 1L)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));  // Assuming response contains { "eligible": true }
    }


}
