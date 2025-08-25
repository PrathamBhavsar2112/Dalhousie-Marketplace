package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_OrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.userId = :userId")
    List<OrderItem> findByUserId(@Param("userId") Long userId);

    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o JOIN oi.listing l " +
            "WHERE o.userId = :userId AND o.orderStatus = 'COMPLETED' AND l.id = :listingId")
    List<OrderItem> findCompletedOrderItemsByUserIdAndListingId(
            @Param("userId") Long userId,
            @Param("listingId") Long listingId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.listing.id = :listingId")
    List<OrderItem> findByListingId(@Param("listingId") Long listingId);

    /**
     * Find order items for a list of listings
     * @param listingIds List of listing IDs to search for
     * @return List of order items for the specified listings
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.listing.id IN :listingIds")
    List<OrderItem> findByListingIdIn(@Param("listingIds") List<Long> listingIds);
}