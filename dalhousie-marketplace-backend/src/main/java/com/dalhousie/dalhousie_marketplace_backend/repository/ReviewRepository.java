package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a given listing
     */
    List<Review> findByListingId(Long listingId);

    /**
     * Find all reviews by a given user
     */
    List<Review> findByUserId(Long userId);

    /**
     * Find a review by user and order item
     */
    Optional<Review> findByUserIdAndOrderItemId(Long userId, Long orderItemId);

    /**
     * Get the average rating for a listing
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listingId = :listingId")
    Double getAverageRatingForListing(@Param("listingId") Long listingId);

    /**
     * Get the number of reviews for a listing
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.listingId = :listingId")
    Long getReviewCountForListing(@Param("listingId") Long listingId);

    /**
     * Check if a user has already reviewed an order item
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.userId = :userId AND r.orderItemId = :orderItemId")
    boolean existsByUserIdAndOrderItemId(@Param("userId") Long userId, @Param("orderItemId") Long orderItemId);

    /**
     * Find reviews by listing ID and fetch the listing eagerly
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.listing WHERE r.listing.id = :listingId")
    List<Review> findByListingIdWithListing(@Param("listingId") Long listingId);
}