package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Bid;
import com.dalhousie.dalhousie_marketplace_backend.model.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Bid entity operations.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Finds all bids for a specific listing.
     *
     * @param listingId The ID of the listing
     * @return List of bids for the specified listing
     */
    List<Bid> findByListingId(Long listingId);

    /**
     * Finds all bids made by a specific buyer.
     *
     * @param buyerId The ID of the buyer
     * @return List of bids made by the specified buyer
     */
    @Query("SELECT b FROM Bid b WHERE b.buyer.userId = :buyerId")
    List<Bid> findByBuyerId(@Param("buyerId") Long buyerId);

    /**
     * Finds all bids for a listing with a specific status.
     *
     * @param listingId The ID of the listing
     * @param status The status to filter by
     * @return List of bids matching the listing ID and status
     */
    @Query("SELECT b FROM Bid b WHERE b.listing.id = :listingId AND b.status = :status")
    List<Bid> findByListingIdAndStatus(@Param("listingId") Long listingId, @Param("status") BidStatus status);

    /**
     * Finds all bids made by a buyer with a specific status.
     *
     * @param buyerId The ID of the buyer
     * @param status The status to filter by
     * @return List of bids matching the buyer ID and status
     */
    @Query("SELECT b FROM Bid b WHERE b.buyer.userId = :buyerId AND b.status = :status")
    List<Bid> findByBuyerIdAndStatus(@Param("buyerId") Long buyerId, @Param("status") BidStatus status);

    /**
     * Counts active bids for a listing with any of the specified statuses.
     *
     * @param listingId The ID of the listing
     * @param statuses List of statuses to include in the count
     * @return Count of bids matching the listing ID and any of the specified statuses
     */
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.listing.id = :listingId AND b.status IN :statuses")
    int countByListingIdAndStatusIn(@Param("listingId") Long listingId, @Param("statuses") List<BidStatus> statuses);

    /**
     * Finds bids for a listing with a specific status, ordered by price (descending).
     *
     * @param listingId The ID of the listing
     * @param status The status to filter by
     * @return List of bids ordered by price (highest first)
     */
    @Query("SELECT b FROM Bid b WHERE b.listing.id = :listingId AND b.status = :status ORDER BY b.proposedPrice DESC")
    List<Bid> findByListingIdAndStatusOrderByProposedPriceDesc(@Param("listingId") Long listingId, @Param("status") BidStatus status);

    /**
     * Finds the latest bid in a conversation between a buyer and a listing.
     *
     * @param listingId The ID of the listing
     * @param buyerId The ID of the buyer
     * @return The most recent bid in the conversation
     */
    @Query("SELECT b FROM Bid b WHERE b.listing.id = :listingId AND b.buyer.userId = :buyerId ORDER BY b.createdAt DESC")
    List<Bid> findLatestBidByListingIdAndBuyerId(@Param("listingId") Long listingId, @Param("buyerId") Long buyerId);

    /**
     * Finds a bid associated with a specific order.
     *
     * @param orderId The ID of the order
     * @return The bid associated with the order, if any
     */
    Optional<Bid> findByOrderId(Long orderId);

    /**
     * Finds all accepted and paid bids for listings created by a seller.
     *
     * @param sellerId The ID of the seller
     * @return List of accepted and paid bids for the seller's listings
     */
    @Query("SELECT b FROM Bid b WHERE b.listing.seller.userId = :sellerId AND b.status IN ('ACCEPTED', 'PAID')")
    List<Bid> findAcceptedAndPaidBidsBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Finds all bids for a list of listings (useful for getting all bids a seller has received).
     *
     * @param listingIds List of listing IDs
     * @return List of bids for the specified listings
     */
    @Query("SELECT b FROM Bid b WHERE b.listing.id IN :listingIds")
    List<Bid> findBySellerListings(@Param("listingIds") List<Long> listingIds);
}