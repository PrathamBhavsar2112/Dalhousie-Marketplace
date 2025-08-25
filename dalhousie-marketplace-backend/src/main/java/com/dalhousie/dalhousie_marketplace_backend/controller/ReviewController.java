package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewEligibilityResponse;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewResponse;
import com.dalhousie.dalhousie_marketplace_backend.model.OrderItem;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderItemService;
import com.dalhousie.dalhousie_marketplace_backend.service.ReviewService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private JwtUtil jwtUtil;


    @PostMapping
    public ResponseEntity<?> createReview(
            @Valid @RequestBody ReviewRequest reviewRequest,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            if (reviewService.hasUserReviewedOrderItem(userId, reviewRequest.getOrderItemId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("You have already reviewed this purchase");
            }

            ReviewResponse createdReview = reviewService.createReview(userId, reviewRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating review: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating review: " + e.getMessage());
        }
    }


    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest reviewRequest,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            ReviewResponse updatedReview = reviewService.updateReview(userId, reviewId, reviewRequest);
            return ResponseEntity.ok(updatedReview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating review: " + e.getMessage());
        }
    }


    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            reviewService.deleteReview(userId, reviewId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting review: " + e.getMessage());
        }
    }


    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(@PathVariable Long reviewId) {
        try {
            ReviewResponse review = reviewService.getReviewById(reviewId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching review: " + e.getMessage());
        }
    }


    @GetMapping("/listing/{listingId}")
    public ResponseEntity<?> getReviewsByListingId(@PathVariable Long listingId) {
        try {
            List<ReviewResponse> reviews = reviewService.getReviewsByListingId(listingId);

            Double averageRating = reviewService.getAverageRatingForListing(listingId);
            Long reviewCount = reviewService.getReviewCountForListing(listingId);

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviews);
            response.put("averageRating", averageRating);
            response.put("reviewCount", reviewCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching reviews: " + e.getMessage());
        }
    }


    @GetMapping("/user")
    public ResponseEntity<?> getUserReviews(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            List<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user reviews: " + e.getMessage());
        }
    }


    @GetMapping("/eligible-items")
    public ResponseEntity<?> getEligibleItemsForReview(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            List<OrderItem> eligibleItems = orderItemService.getEligibleOrderItemsForReview(userId);

            // Convert to DTOs with more information
            List<ReviewEligibilityResponse.ReviewableItemDTO> reviewableDTOs = eligibleItems.stream()
                    .map(item -> {
                        String imageUrl = null;
                        // If there are listing images, get the primary one
                        if (item.getListing() != null && item.getListing().getId() != null) {
                            imageUrl = "/api/listings/" + item.getListing().getId() + "/images";
                        }

                        return new ReviewEligibilityResponse.ReviewableItemDTO(
                                item.getOrderItemId(),
                                item.getListing().getId(),
                                item.getListing().getTitle(),
                                imageUrl,
                                item.getListing().getPrice(),
                                item.getOrder().getOrderDate().toString()
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ReviewEligibilityResponse(reviewableDTOs));
        } catch (Exception e) {
            logger.error("Error fetching eligible items: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching eligible items: " + e.getMessage());
        }
    }


    @GetMapping("/eligibility/listing/{listingId}")
    public ResponseEntity<?> checkReviewEligibilityForListing(
            @PathVariable Long listingId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = validateAndExtractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            // Check if the user has purchased this item and not reviewed it yet
            List<OrderItem> completedItems = orderItemService.getEligibleOrderItemsForReview(userId).stream()
                    .filter(item -> item.getListing().getId().equals(listingId))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("eligible", !completedItems.isEmpty());

            if (!completedItems.isEmpty()) {
                OrderItem item = completedItems.get(0);
                response.put("orderItemId", item.getOrderItemId());
                response.put("orderId", item.getOrder().getOrderId());
                response.put("purchaseDate", item.getOrder().getOrderDate());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking review eligibility: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking review eligibility: " + e.getMessage());
        }
    }


    private Long validateAndExtractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            logger.error("Token extraction error: {}", e.getMessage());
            return null;
        }
    }
}

