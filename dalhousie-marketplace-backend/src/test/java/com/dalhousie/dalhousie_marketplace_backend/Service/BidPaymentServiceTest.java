package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderItemRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.OrderRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.BidPaymentService;
import com.dalhousie.dalhousie_marketplace_backend.service.NotificationService;
import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for BidPaymentService functionality.
 * Verifies payment processing for bids.
 */
@ExtendWith(MockitoExtension.class)
public class BidPaymentServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BidPaymentService bidPaymentService;

    private User buyer;
    private User seller;
    private Listing listing;
    private Bid acceptedBid;
    private Bid pendingBid;
    private Order order;

    /**
     * Sets up test data before each test.
     * Creates test user, listing, bid, and order objects.
     */
    @BeforeEach
    void setUp() {
        // Setup buyer
        buyer = new User();
        buyer.setUserId(1L);
        buyer.setEmail("buyer@example.com");
        buyer.setusername("Buyer User");

        // Setup seller
        seller = new User();
        seller.setUserId(2L);
        seller.setEmail("seller@example.com");
        seller.setusername("Seller User");

        // Setup listing
        listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Test Listing");
        listing.setDescription("A listing for testing");
        listing.setPrice(100.0);
        listing.setBiddingAllowed(true);
        listing.setStartingBid(50.0);
        listing.setSeller(seller);

        // Setup accepted bid
        acceptedBid = new Bid();
        acceptedBid.setId(1L);
        acceptedBid.setListing(listing);
        acceptedBid.setBuyer(buyer);
        acceptedBid.setProposedPrice(80.0);
        acceptedBid.setAdditionalTerms("I can pick it up today");
        acceptedBid.setStatus(BidStatus.ACCEPTED);
        acceptedBid.setCreatedAt(new Date());
        acceptedBid.setUpdatedAt(new Date());

        // Setup pending bid (not accepted)
        pendingBid = new Bid();
        pendingBid.setId(2L);
        pendingBid.setListing(listing);
        pendingBid.setBuyer(buyer);
        pendingBid.setProposedPrice(75.0);
        pendingBid.setAdditionalTerms("My offer");
        pendingBid.setStatus(BidStatus.PENDING);
        pendingBid.setCreatedAt(new Date());
        pendingBid.setUpdatedAt(new Date());

        // Setup order
        order = new Order();
        order.setOrderId(1L);
        order.setUserId(buyer.getUserId());
        order.setTotalPrice(BigDecimal.valueOf(80.0));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        // Mock BidRepository in PaymentService to prevent NullPointerException
        Mockito.mock(BidRepository.class);
    }

    @Test
    void testNewCheckoutSession_Success() throws Exception {
        // Ensure seller is linked properly
        listing.setSeller(seller);
        acceptedBid.setListing(listing);
        acceptedBid.setBuyer(buyer);

        // Mock repository returns
        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(paymentService.createCheckoutSession(any(Order.class))).thenReturn("https://checkout.com/test");

        // Act
        String result = bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());

        // Assert
        assertEquals("https://checkout.com/test", result);

        // Verify interactions
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(bidRepository).save(any(Bid.class));
        verify(notificationService).sendNotification(eq(seller), eq(NotificationType.BID), contains("Payment initiated"));
    }


    @Test
    void testCheckoutWithExistingOrder_PendingStatus() throws Exception {
        acceptedBid.setOrderId(order.getOrderId());
        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
        when(paymentService.createCheckoutSession(order)).thenReturn("https://checkout.com/existing");

        String result = bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());

        assertEquals("https://checkout.com/existing", result);
        verify(orderRepository, never()).save(any(Order.class));
    }

//    @Test
//    void testBidNotFound() {
//        when(bidRepository.findById(99L)).thenReturn(Optional.empty());
//
//        Exception e = assertThrows(RuntimeException.class, () -> {
//            bidPaymentService.createBidCheckoutSession(99L, buyer.getUserId());
//        });
//
//        assertEquals("Bid not found", e.getMessage());
//    }

    @Test
    void testOnlyAcceptedBidsAllowed() {
        when(bidRepository.findById(pendingBid.getId())).thenReturn(Optional.of(pendingBid));

        Exception e = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(pendingBid.getId(), buyer.getUserId());
        });

        assertEquals("Only accepted bids can be processed for payment", e.getMessage());
    }

    @Test
    void testWrongBuyer_NotAllowed() {
        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));

        Exception e = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), 999L); // wrong user
        });

        assertEquals("You can only pay for your own bids", e.getMessage());
    }

//    @Test
//    void testAlreadyPaidBid() {
//        acceptedBid.setOrderId(order.getOrderId());
//        order.setOrderStatus(OrderStatus.COMPLETED);
//
//        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
//        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
//
//        Exception e = assertThrows(RuntimeException.class, () -> {
//            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());
//        });
//
//        assertEquals("This bid has already been paid for", e.getMessage());
//    }

    @Test
    void testStripeExceptionHandled() throws Exception {
        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(paymentService.createCheckoutSession(any(Order.class))).thenThrow(new StripeException("Stripe failed", null, null, 500) {});

        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());
        });

        assertTrue(e.getMessage().contains("Error creating payment session"));
    }

    /**
     * Tests creating a checkout session for an accepted bid with an existing order.
     */
    @Test
    void testExistingOrderCheckout() throws Exception {
        // Arrange
        String expectedCheckoutUrl = "https://stripe.com/checkout/test";
        acceptedBid.setOrderId(order.getOrderId());

        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
        when(paymentService.createCheckoutSession(order)).thenReturn(expectedCheckoutUrl);

        // Act
        String resultUrl = bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());

        // Assert
        assertEquals(expectedCheckoutUrl, resultUrl);

        // Verify the existing order was used
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).save(any(OrderItem.class));

        // Verify checkout session was created
        verify(paymentService).createCheckoutSession(eq(order));
    }

    /**
     * Tests that a non-accepted bid cannot be processed for payment.
     */
    @Test
    void testNonAcceptedBid() throws Exception {
        // Arrange
        when(bidRepository.findById(pendingBid.getId())).thenReturn(Optional.of(pendingBid));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(pendingBid.getId(), buyer.getUserId());
        });

        assertEquals("Only accepted bids can be processed for payment", exception.getMessage());

        // Verify no further processing occurred
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).createCheckoutSession(any(Order.class));
    }

    /**
     * Tests that a user can only pay for their own bids.
     */
    @Test
    void testWrongUserBid() throws Exception {
        // Arrange
        Long wrongUserId = 99L;

        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), wrongUserId);
        });

        assertEquals("You can only pay for your own bids", exception.getMessage());

        // Verify no further processing occurred
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).createCheckoutSession(any(Order.class));
    }

    /**
     * Tests handling of a bid that has been paid for already.
     */
    @Test
    void testAlreadyPaidBid() throws Exception {
        // Arrange
        acceptedBid.setOrderId(order.getOrderId());
        order.setOrderStatus(OrderStatus.COMPLETED);

        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());
        });

        assertEquals("This bid has already been paid for", exception.getMessage());

        // Verify no further processing occurred
        verify(paymentService, never()).createCheckoutSession(any(Order.class));
    }

    /**
     * Tests handling of Stripe exceptions during checkout session creation.
     */
//    @Test
//    void testStripeError() throws Exception {
//        // Arrange
//        when(bidRepository.findById(acceptedBid.getId())).thenReturn(Optional.of(acceptedBid));
//        when(orderRepository.save(any(Order.class))).thenReturn(order);
//        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
//
//        // Mock Stripe exception
//        when(paymentService.createCheckoutSession(any(Order.class)))
//                .thenThrow(new StripeException("Stripe API error", null, null, 0) {});
//
//        // Act & Assert
//        Exception exception = assertThrows(RuntimeException.class, () -> {
//            bidPaymentService.createBidCheckoutSession(acceptedBid.getId(), buyer.getUserId());
//        });
//
//        assertTrue(exception.getMessage().contains("Error creating payment session"));
//
//        // Verify order creation happened but checkout failed
//        verify(orderRepository).save(any(Order.class));
//        verify(orderItemRepository).save(any(OrderItem.class));
//        verify(bidRepository).save(any(Bid.class));
//        verify(paymentService).createCheckoutSession(any(Order.class));
//    }

    /**
     * Tests that a bid not found scenario is handled properly.
     */
    @Test
    void testBidNotFound() throws Exception {
        // Arrange
        when(bidRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidPaymentService.createBidCheckoutSession(99L, buyer.getUserId());
        });

        assertEquals("Bid not found", exception.getMessage());

        // Verify no further processing
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).createCheckoutSession(any(Order.class));
    }
}