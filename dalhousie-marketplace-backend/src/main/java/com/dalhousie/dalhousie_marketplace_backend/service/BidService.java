package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Service for managing bids on listings.
 */
@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a new bid on a listing.
     *
     * @param listingId The ID of the listing to bid on
     * @param buyerId The ID of the buyer making the bid
     * @param proposedPrice The price proposed by the buyer
     * @param additionalTerms Any additional terms for the bid (optional)
     * @return The created bid
     * @throws RuntimeException if validation fails
     */
    @Transactional
    public Bid createBid(Long listingId, Long buyerId, Double proposedPrice, String additionalTerms) {
        // Validate listing exists and allows bidding
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (!listing.getBiddingAllowed()) {
            throw new RuntimeException("This listing does not allow bidding");
        }

        // Validate buyer exists and is not the seller
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (buyer.getUserId().equals(listing.getSeller().getUserId())) {
            throw new RuntimeException("You cannot bid on your own listing");
        }

        // Validate bid price meets minimum requirements
        if (listing.getStartingBid() != null && proposedPrice < listing.getStartingBid()) {
            throw new IllegalArgumentException("Bid must be at least the starting price of $" + listing.getStartingBid());
        }

        // Create and save the bid
        Bid bid = new Bid();
        bid.setListing(listing);
        bid.setBuyer(buyer);
        bid.setProposedPrice(proposedPrice);
        bid.setAdditionalTerms(additionalTerms);
        bid.setStatus(BidStatus.PENDING);

        Bid savedBid = bidRepository.save(bid);

        // Notify the seller of the new bid
        notificationService.sendNotification(
                listing.getSeller(),
                NotificationType.BID,
                "New bid of $" + proposedPrice + " received for your listing: " + listing.getTitle()
        );

        // Send real-time update via WebSocket
        messagingTemplate.convertAndSend("/topic/bids/" + listingId, savedBid);

        return savedBid;
    }

    /**
     * Updates the status of a bid (accept, reject, etc.).
     *
     * @param bidId The ID of the bid to update
     * @param userId The ID of the user trying to update the bid
     * @param newStatus The new status to set
     * @return The updated bid
     * @throws RuntimeException if validation fails
     */
    @Transactional
    public Bid updateBidStatus(Long bidId, Long userId, BidStatus newStatus) {
        // Find the bid
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        // Verify that only the seller can update bid status
        if (!bid.getListing().getSeller().getUserId().equals(userId)) {
            throw new RuntimeException("Only the seller can update bid status");
        }

        // Update the bid status
        bid.setStatus(newStatus);

        Bid updatedBid = bidRepository.save(bid);

        // Prepare notification message based on the status
        String message;
        if (newStatus == BidStatus.ACCEPTED) {
            message = "Your bid of $" + bid.getProposedPrice() + " for " + bid.getListing().getTitle() + " was accepted!";
        } else if (newStatus == BidStatus.REJECTED) {
            message = "Your bid of $" + bid.getProposedPrice() + " for " + bid.getListing().getTitle() + " was rejected.";
        } else {
            message = "The status of your bid for " + bid.getListing().getTitle() + " has been updated to " + newStatus;
        }

        // Notify the buyer
        notificationService.sendNotification(
                bid.getBuyer(),
                NotificationType.BID,
                message
        );

        // Send real-time update via WebSocket
        messagingTemplate.convertAndSend("/topic/bids/" + bid.getListing().getId(), updatedBid);

        return updatedBid;
    }

    /**
     * Accepts a single bid and automatically rejects all other bids for the same listing.
     * Also creates an order for the accepted bid.
     *
     * @param bidId The ID of the bid to accept
     * @param sellerId The ID of the seller accepting the bid
     * @return The accepted bid
     * @throws RuntimeException if validation fails
     */
    @Transactional
    public Bid acceptSingleBid(Long bidId, Long sellerId) {
        // Find the bid
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        // Verify that only the seller can accept bids
        if (!bid.getListing().getSeller().getUserId().equals(sellerId)) {
            throw new RuntimeException("Only the seller can accept bids");
        }

        // Verify the bid is in a state that can be accepted
        if (bid.getStatus() != BidStatus.PENDING && bid.getStatus() != BidStatus.COUNTERED) {
            throw new RuntimeException("Only pending or countered bids can be accepted");
        }

        // Get the listing ID to find other bids
        Long listingId = bid.getListing().getId();

        // Accept this bid and update its status
        bid.setStatus(BidStatus.ACCEPTED);
        Bid acceptedBid = bidRepository.save(bid);

        // Get all other pending bids for this listing
        List<Bid> otherPendingBids = bidRepository.findByListingIdAndStatus(listingId, BidStatus.PENDING);

        // Get all other countered bids for this listing
        List<Bid> otherCounteredBids = bidRepository.findByListingIdAndStatus(listingId, BidStatus.COUNTERED);

        // Combine the lists
        List<Bid> allOtherActiveBids = new ArrayList<>();
        allOtherActiveBids.addAll(otherPendingBids);
        allOtherActiveBids.addAll(otherCounteredBids);

        // Remove the accepted bid from the list if it's somehow present
        allOtherActiveBids.removeIf(b -> b.getId().equals(bidId));

        // Reject all other bids
        for (Bid otherBid : allOtherActiveBids) {
            otherBid.setStatus(BidStatus.REJECTED);
            bidRepository.save(otherBid);

            // Notify each rejected bidder
            notificationService.sendNotification(
                    otherBid.getBuyer(),
                    NotificationType.BID,
                    "Your bid of $" + otherBid.getProposedPrice() + " for " +
                            bid.getListing().getTitle() + " was not selected."
            );

            // Send real-time update via WebSocket for each rejected bid
            messagingTemplate.convertAndSend("/topic/bids/" + listingId, otherBid);
        }

        // Update listing status to INACTIVE to prevent further bids
        Listing listing = bid.getListing();
        listing.setStatus(Listing.ListingStatus.INACTIVE);
        listingRepository.save(listing);

        // Create order for the accepted bid
        Order order = new Order();
        order.setUserId(bid.getBuyer().getUserId());
        order.setTotalPrice(BigDecimal.valueOf(bid.getProposedPrice()));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // Create and save order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setListing(listing);
        orderItem.setQuantity(1);
        orderItem.setPrice(BigDecimal.valueOf(bid.getProposedPrice()));
        orderItemRepository.save(orderItem);

        // Update the list of items in the order
        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        savedOrder.setItems(items);
        orderRepository.save(savedOrder);

        // Store order ID in bid for reference
        acceptedBid.setOrderId(savedOrder.getOrderId());
        bidRepository.save(acceptedBid);

        // Notify the buyer about winning bid
        notificationService.sendNotification(
                bid.getBuyer(),
                NotificationType.BID,
                "Your bid has been accepted for " +
                        listing.getTitle() + ". Please proceed to payment."
        );

        // Notify the seller about accepting a bid
        notificationService.sendNotification(
                listing.getSeller(),
                NotificationType.BID,
                "You've accepted a bid of $" + bid.getProposedPrice() + " for your listing: " + listing.getTitle()
        );

        // Send real-time update via WebSocket for the accepted bid
        messagingTemplate.convertAndSend("/topic/bids/" + listingId, acceptedBid);

        return acceptedBid;
    }

    /**
     * Gets all bids for a specific listing.
     *
     * @param listingId The ID of the listing
     * @return List of bids for the listing
     */
    public List<Bid> getBidsByListing(Long listingId) {
        return bidRepository.findByListingId(listingId);
    }

    /**
     * Gets all bids made by a specific user.
     *
     * @param userId The ID of the user
     * @return List of bids made by the user
     */
    public List<Bid> getBidsByUser(Long userId) {
        return bidRepository.findByBuyerId(userId);
    }

    /**
     * Gets bids for a listing with a specific status.
     *
     * @param listingId The ID of the listing
     * @param status The status to filter by
     * @return List of bids matching criteria
     */
    public List<Bid> getBidsByListingAndStatus(Long listingId, BidStatus status) {
        return bidRepository.findByListingIdAndStatus(listingId, status);
    }

    /**
     * Creates a counter offer to an existing bid.
     *
     * @param originalBidId The ID of the original bid being countered
     * @param sellerId The ID of the seller making the counter offer
     * @param counterPrice The counter offer price
     * @param counterTerms Additional terms for the counter offer
     * @return The new counter bid
     * @throws RuntimeException if validation fails
     */
    @Transactional
    public Bid counterBid(Long originalBidId, Long sellerId, Double counterPrice, String counterTerms) {
        // Find the original bid
        Bid originalBid = bidRepository.findById(originalBidId)
                .orElseThrow(() -> new RuntimeException("Original bid not found"));

        // Verify that only the seller can create counter offers
        if (!originalBid.getListing().getSeller().getUserId().equals(sellerId)) {
            throw new RuntimeException("Only the seller can create counter offers");
        }

        // Mark the original bid as countered
        originalBid.setStatus(BidStatus.COUNTERED);
        bidRepository.save(originalBid);

        // Create new counter bid with the same relationship to listing and buyer
        Bid counterBid = new Bid();
        counterBid.setListing(originalBid.getListing());
        counterBid.setBuyer(originalBid.getBuyer());
        counterBid.setProposedPrice(counterPrice);
        counterBid.setAdditionalTerms(counterTerms);
        counterBid.setStatus(BidStatus.COUNTERED);

        Bid savedCounterBid = bidRepository.save(counterBid);

        // Notify the buyer
        notificationService.sendNotification(
                originalBid.getBuyer(),
                NotificationType.BID,
                originalBid.getListing().getSeller().getUsername() + " has countered your bid with $" +
                        counterPrice + " for " + originalBid.getListing().getTitle()
        );

        // Send real-time update via WebSocket
        messagingTemplate.convertAndSend("/topic/bids/" + originalBid.getListing().getId(), savedCounterBid);

        return savedCounterBid;
    }

    /**
     * Gets the count of active bids for a listing.
     *
     * @param listingId The ID of the listing
     * @return Count of active bids
     */
    public int getActiveBidCount(Long listingId) {
        List<BidStatus> activeStatuses = Arrays.asList(BidStatus.PENDING, BidStatus.COUNTERED);
        return bidRepository.countByListingIdAndStatusIn(listingId, activeStatuses);
    }

    /**
     * Gets a bid by its ID.
     *
     * @param bidId The ID of the bid
     * @return The bid if found, null otherwise
     */
    public Bid getBidById(Long bidId) {
        return bidRepository.findById(bidId).orElse(null);
    }

    /**
     * Finalizes bidding on a listing by selecting the highest bid as the winner
     * and automatically creating an order for the winning bid.
     *
     * @param listingId The ID of the listing
     * @param sellerId The ID of the seller
     * @return The winning bid
     * @throws RuntimeException if no bids or unauthorized access
     */
    @Transactional
    public Bid finalizeBidding(Long listingId, Long sellerId) {
        // Get the listing
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Verify seller owns the listing
        if (!listing.getSeller().getUserId().equals(sellerId)) {
            throw new RuntimeException("Only the seller can finalize bidding");
        }

        // Get all pending bids sorted by price (highest first)
        List<Bid> pendingBids = bidRepository.findByListingIdAndStatusOrderByProposedPriceDesc(listingId, BidStatus.PENDING);

        if (pendingBids.isEmpty()) {
            throw new RuntimeException("No pending bids to finalize");
        }

        // Select the highest bid as winner
        Bid winningBid = pendingBids.get(0);
        winningBid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(winningBid);

        // Reject all other bids - make sure we process and save EACH bid individually
        for (int i = 1; i < pendingBids.size(); i++) {
            Bid otherBid = pendingBids.get(i);
            otherBid.setStatus(BidStatus.REJECTED);
            bidRepository.save(otherBid);
            // Notify losing bidders
            notificationService.sendNotification(
                    otherBid.getBuyer(),
                    NotificationType.BID,
                    "Your bid of $" + otherBid.getProposedPrice() + " for " +
                            listing.getTitle() + " was not selected as the winning bid."
            );
        }

        listing.setStatus(Listing.ListingStatus.INACTIVE);
        listingRepository.save(listing);

        // Create order automatically after bid acceptance
        Order order = new Order();
        order.setUserId(winningBid.getBuyer().getUserId());
        order.setTotalPrice(BigDecimal.valueOf(winningBid.getProposedPrice()));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order = orderRepository.save(order);

        // Create and save order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setListing(listing);
        orderItem.setQuantity(1);
        orderItem.setPrice(BigDecimal.valueOf(winningBid.getProposedPrice()));
        orderItemRepository.save(orderItem);

        // Update the list of items in the order
        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);
        orderRepository.save(order);

        // Store order ID in bid for reference
        winningBid.setOrderId(order.getOrderId());
        bidRepository.save(winningBid);

        // Notify winner
        notificationService.sendNotification(
                winningBid.getBuyer(),
                NotificationType.BID,
                "Your bid has been accepted as the winner for " +
                        listing.getTitle() + ". Please proceed to payment."
        );

        return winningBid;
    }
}