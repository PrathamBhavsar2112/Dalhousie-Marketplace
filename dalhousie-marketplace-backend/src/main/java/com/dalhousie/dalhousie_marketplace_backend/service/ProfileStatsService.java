package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileStatsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BidRepository bidRepository;

    public Map<String, Object> getProfileStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.putAll(getBuyerStats(userId));
        stats.putAll(getSellerStats(userId));
        return stats;
    }

    public Map<String, Object> getBuyerStats(Long userId) {
        Map<String, Object> buyerStats = new HashMap<>();

        List<Order> userOrders = orderRepository.findByUserId(userId);
        BigDecimal totalSpent = userOrders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        buyerStats.put("buyingActivity", Map.of(
                "totalOrders", userOrders.size(),
                "totalSpent", totalSpent,
                "pendingOrders", countOrdersByStatus(userOrders, OrderStatus.PENDING),
                "completedOrders", countOrdersByStatus(userOrders, OrderStatus.COMPLETED),
                "cancelledOrders", countOrdersByStatus(userOrders, OrderStatus.CANCELLED)
        ));

        List<Bid> userBids = bidRepository.findByBuyerId(userId);
        buyerStats.put("biddingActivity", Map.of(
                "totalBids", userBids.size(),
                "activeBids", countBidsByStatuses(userBids, BidStatus.PENDING, BidStatus.COUNTERED),
                "acceptedBids", countBidsByStatus(userBids, BidStatus.ACCEPTED),
                "successfulBids", countBidsByStatus(userBids, BidStatus.PAID)
        ));

        return buyerStats;
    }

    public Map<String, Object> getSellerStats(Long userId) {
        Map<String, Object> sellerStats = new HashMap<>();

        List<Listing> listings = listingRepository.findBySellerId(userId);
        List<Long> listingIds = listings.stream().map(Listing::getId).toList();

        Map<String, Object> listingActivity = new HashMap<>();
        listingActivity.put("totalListings", listings.size());
        listingActivity.put("activeListings", countListingsByStatus(listings, Listing.ListingStatus.ACTIVE));
        listingActivity.put("inactiveListings", countListingsByStatus(listings, Listing.ListingStatus.INACTIVE));
        listingActivity.put("soldListings", countListingsByStatus(listings, Listing.ListingStatus.SOLD));
        sellerStats.put("listingActivity", listingActivity);

        List<OrderItem> soldItems = orderItemRepository.findByListingIdIn(listingIds);
        List<Bid> paidBids = bidRepository.findAcceptedAndPaidBidsBySellerId(userId);

        BigDecimal regularSales = calculateTotalSalesFromItems(soldItems);
        BigDecimal bidSales = calculateTotalSalesFromBids(paidBids);
        BigDecimal totalSales = regularSales.add(bidSales);
        int itemsSold = soldItems.size() + paidBids.size();

        Map<String, Object> salesActivity = new HashMap<>();
        salesActivity.put("totalSales", totalSales);
        salesActivity.put("regularSales", regularSales);
        salesActivity.put("bidSales", bidSales);
        salesActivity.put("itemsSold", itemsSold);
        sellerStats.put("salesActivity", salesActivity);

        List<Bid> receivedBids = bidRepository.findBySellerListings(listingIds);

        Map<String, Object> bidActivity = new HashMap<>();
        bidActivity.put("totalBidsReceived", receivedBids.size());
        bidActivity.put("activeBidsReceived", countBidsByStatuses(
                receivedBids, BidStatus.PENDING, BidStatus.COUNTERED));
        sellerStats.put("bidActivity", bidActivity);

        return sellerStats;
    }

    private long countOrdersByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(o -> o.getOrderStatus() == status).count();
    }

    private long countBidsByStatus(List<Bid> bids, BidStatus status) {
        return bids.stream().filter(b -> b.getStatus() == status).count();
    }

    private long countBidsByStatuses(List<Bid> bids, BidStatus... statuses) {
        Set<BidStatus> statusSet = Set.of(statuses);
        return bids.stream().filter(b -> statusSet.contains(b.getStatus())).count();
    }

    private long countListingsByStatus(List<Listing> listings, Listing.ListingStatus status) {
        return listings.stream().filter(l -> l.getStatus() == status).count();
    }

    private BigDecimal calculateTotalSalesFromItems(List<OrderItem> items) {
        return items.stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateItemTotal(OrderItem item) {
        BigDecimal price = item.getPrice();
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
        return price.multiply(quantity);
    }

    private BigDecimal calculateTotalSalesFromBids(List<Bid> bids) {
        return bids.stream()
                .map(b -> BigDecimal.valueOf(b.getProposedPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
