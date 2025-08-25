package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    /**
     * Find listings by status
     * @param status The listing status to filter by
     * @return List of listings with the specified status
     */
    List<Listing> findByStatus(Listing.ListingStatus status);

    /**
     * Find listings by seller ID
     * @param sellerId The ID of the seller
     * @return List of listings created by the specified seller
     */
    @Query("SELECT l FROM Listing l WHERE l.seller.userId = :sellerId")
    List<Listing> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT l FROM Listing l WHERE l.status = com.dalhousie.dalhousie_marketplace_backend.model.Listing$ListingStatus.ACTIVE AND (LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Listing> searchByKeyword(@Param("keyword") String keyword);

    List<Listing> findByBiddingAllowedAndStatus(Boolean biddingAllowed, Listing.ListingStatus status);
}