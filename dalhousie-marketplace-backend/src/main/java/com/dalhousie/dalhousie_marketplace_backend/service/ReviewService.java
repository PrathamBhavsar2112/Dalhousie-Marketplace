package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ReviewResponse;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ListingService listingService;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        OrderItem orderItem = getOrderItemOrThrow(request.getOrderItemId());

        validateReviewCreation(userId, request, orderItem);

        Review review = new Review();
        review.setUserId(userId);
        review.setOrderItemId(request.getOrderItemId());
        review.setListingId(request.getListingId());
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());

        Review saved = reviewRepository.save(review);
        updateListingRatingStats(review.getListingId());

        return buildReviewResponse(saved, getUser(userId), getListing(request.getListingId()));
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        Review review = getReviewOrThrow(reviewId);

        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());

        Review updated = reviewRepository.save(review);
        updateListingRatingStats(review.getListingId());

        return buildReviewResponse(updated, getUser(userId), getListing(review.getListingId()));
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = getReviewOrThrow(reviewId);

        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        Long listingId = review.getListingId();
        reviewRepository.delete(review);
        updateListingRatingStats(listingId);
    }

    public ReviewResponse getReviewById(Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        return buildReviewResponse(review, getUser(review.getUserId()), getListing(review.getListingId()));
    }

    public List<ReviewResponse> getReviewsByListingId(Long listingId) {
        List<Review> reviews = reviewRepository.findByListingId(listingId);

        return reviews.stream()
                .map(this::buildSafeReviewResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        List<Review> userReviews = reviewRepository.findByUserId(userId);

        return userReviews.stream()
                .map(this::buildSafeReviewResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Double getAverageRatingForListing(Long listingId) {
        Double average = reviewRepository.getAverageRatingForListing(listingId);
        return average != null ? average : 0.0;
    }

    public Long getReviewCountForListing(Long listingId) {
        Long count = reviewRepository.getReviewCountForListing(listingId);
        return count != null ? count : 0L;
    }

    public boolean hasUserReviewedOrderItem(Long userId, Long orderItemId) {
        return reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId);
    }

    public List<Long> getEligibleOrderItemsForReview(Long userId) {
        List<OrderItem> allItems = orderItemRepository.findByUserId(userId);
        return filterEligibleItems(userId, allItems);
    }

    private List<Long> filterEligibleItems(Long userId, List<OrderItem> items) {
        return items.stream()
                .filter(this::isCompletedOrder)
                .filter(item -> isNotYetReviewed(userId, item))
                .map(OrderItem::getOrderItemId)
                .collect(Collectors.toList());
    }

    private boolean isCompletedOrder(OrderItem item) {
        return item.getOrder().getOrderStatus() == OrderStatus.COMPLETED;
    }

    private boolean isNotYetReviewed(Long userId, OrderItem item) {
        return !hasUserReviewedOrderItem(userId, item.getOrderItemId());
    }

    // -------------------- Private Helpers --------------------

    private void validateReviewCreation(Long userId, ReviewRequest req, OrderItem item) {
        if (!item.getListing().getId().equals(req.getListingId())) {
            throw new IllegalArgumentException("The order item does not match the listing");
        }
        if (!item.getOrder().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only review items you have purchased");
        }
        if (hasUserReviewedOrderItem(userId, req.getOrderItemId())) {
            throw new IllegalArgumentException("You have already reviewed this purchase");
        }
    }

    private void updateListingRatingStats(Long listingId) {
        Double average = getAverageRatingForListing(listingId);
        Long count = getReviewCountForListing(listingId);
        listingService.updateListingRatingStats(listingId, average, count.intValue());
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));
    }

    private OrderItem getOrderItemOrThrow(Long itemId) {
        return orderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Order item not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found"));
    }

    private ReviewResponse buildSafeReviewResponse(Review review) {
        try {
            User user = getUser(review.getUserId());
            Listing listing = getListing(review.getListingId());
            return buildReviewResponse(review, user, listing);
        } catch (Exception e) {
            System.err.println("Error building response for review " + review.getReviewId() + ": " + e.getMessage());
            return null;
        }
    }

    public ReviewResponse buildReviewResponse(Review review, User user, Listing listing) {
        ReviewResponse response = new ReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setListingId(listing.getId());
        response.setListingTitle(listing.getTitle());
        response.setRating(review.getRating());
        response.setReviewText(review.getReviewText());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
