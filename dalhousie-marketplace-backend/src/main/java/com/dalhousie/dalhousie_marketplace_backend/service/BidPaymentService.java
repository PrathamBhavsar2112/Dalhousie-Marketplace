package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling bid-related payment operations.
 */
@Service
public class BidPaymentService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Converts an accepted bid to an order and creates a Stripe checkout session.
     *
     * @param bidId The ID of the accepted bid
     * @param buyerId The ID of the buyer
     * @return The Stripe checkout URL
     * @throws Exception If there's an error processing the bid payment
     */
    @Transactional
    public String createBidCheckoutSession(Long bidId, Long buyerId) throws Exception {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        if (bid.getStatus() != BidStatus.ACCEPTED) {
            throw new RuntimeException("Only accepted bids can be processed for payment");
        }

        if (!bid.getBuyer().getUserId().equals(buyerId)) {
            throw new RuntimeException("You can only pay for your own bids");
        }

        if (bid.getOrderId() != null) {
            Order existingOrder = orderRepository.findById(bid.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Associated order not found"));

            boolean isAlreadyPaid = existingOrder.getOrderStatus() == OrderStatus.COMPLETED;
            if (isAlreadyPaid) {
                throw new RuntimeException("This bid has already been paid for");
            }

            return paymentService.createCheckoutSession(existingOrder);
        }

        Order order = new Order();
        order.setUserId(buyerId);
        order.setTotalPrice(BigDecimal.valueOf(bid.getProposedPrice()));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setListing(bid.getListing());
        orderItem.setQuantity(1);
        orderItem.setPrice(BigDecimal.valueOf(bid.getProposedPrice()));
        orderItemRepository.save(orderItem);

        savedOrder.setItems(List.of(orderItem));
        orderRepository.save(savedOrder);

        Long orderId = savedOrder.getOrderId();
        bid.setOrderId(orderId);
        bidRepository.save(bid);

        String listingTitle = bid.getListing().getTitle();
        User seller = bid.getListing().getSeller();
        String message = "Payment initiated for your listing: " + listingTitle;

        notificationService.sendNotification(seller, NotificationType.BID, message);

        try {
            return paymentService.createCheckoutSession(savedOrder);
        } catch (StripeException e) {
            String errorMessage = "Error creating payment session: " + e.getMessage();
            throw new RuntimeException(errorMessage);
        }
    }
}
