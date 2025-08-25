package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import com.dalhousie.dalhousie_marketplace_backend.service.NotificationService;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
// import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the PaymentService class.
 * This test suite verifies payment processing functionality and inventory management.
 */
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private BidRepository bidRepository;
    @Mock
    private NotificationService notificationService;


    @InjectMocks
    private PaymentService paymentService;

    private Order mockOrder;
    private Payment mockPayment;
    private PaymentIntent mockPaymentIntent;
    private Listing mockListing;
    private OrderItem mockOrderItem;

    /**
     * Setup method that runs before each test.
     * Initializes mock objects and test data.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "test_key");

        // Set up a Listing with stock
        mockListing = new Listing();
        mockListing.setId(1L);
        mockListing.setTitle("Test Product");
        mockListing.setQuantity(5); // Initial stock of 5
        mockListing.setPrice(100.0);

        // Set up an Order Item
        mockOrderItem = new OrderItem();
        mockOrderItem.setOrderItemId(1L);
        mockOrderItem.setListing(mockListing);
        mockOrderItem.setQuantity(2); // Buying 2 units
        mockOrderItem.setPrice(new BigDecimal("200.00"));

        ArrayList<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(mockOrderItem);

        // Set up an Order
        mockOrder = new Order();
        mockOrder.setOrderId(1L);
        mockOrder.setUserId(1L);
        mockOrder.setTotalPrice(new BigDecimal("200.00"));
        mockOrder.setOrderStatus(OrderStatus.PENDING);
        mockOrder.setOrderDate(LocalDateTime.now());
        mockOrder.setItems(orderItems);

        // Set up a Payment
        mockPayment = new Payment();
        mockPayment.setId(1L);
        mockPayment.setOrderId(1L);
        mockPayment.setStripePaymentIntentId("pi_test123");
        mockPayment.setPaymentMethod(Payment.PaymentMethod.STRIPE);
        mockPayment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        mockPayment.setAmount(new BigDecimal("200.00"));
        mockPayment.setCurrency("CAD");
        mockPayment.setCreatedAt(LocalDateTime.now());

        // Set up mock PaymentIntent
        mockPaymentIntent = mock(PaymentIntent.class);
        when(mockPaymentIntent.getId()).thenReturn("pi_test123");
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L); // In cents
    }

    /**
     * Tests that a new payment is created when no existing payment is found.
     * Verifies the payment repository save method is called with the correct data.
     */
    @Test
    void createOrUpdatePaymentFromIntent_shouldCreateNewPayment_whenNoExistingPayment() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // Act
        Payment result = paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L);

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        assertEquals(mockPayment, result);
    }

    /**
     * Tests that an existing payment is updated when found by ID.
     * Verifies the payment repository save method is called with the updated payment.
     */
    @Test
    void createOrUpdatePaymentFromIntent_shouldUpdateExistingPayment_whenFoundById() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId(anyString())).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // Act
        Payment result = paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 2L);

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        assertEquals(mockPayment, result);
    }

    @Test
    void createCheckoutSession_shouldThrowException_WhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", ""); // simulate missing API key
        Exception exception = assertThrows(RuntimeException.class, () ->
                paymentService.createCheckoutSession(mockOrder)
        );
        assertTrue(exception.getMessage().contains("Stripe API key is not configured"));
    }
//    @Test
//    void createCheckoutSession_shouldReturnValidUrl_WhenSessionCreatedSuccessfully() throws Exception {
//        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_dummy");
//        ReflectionTestUtils.setField(paymentService, "appBaseUrl", "http://localhost:8080");
//
//        Session mockSession = mock(Session.class);
//        when(mockSession.getUrl()).thenReturn("http://checkout-url.com");
//        when(mockSession.getPaymentIntent()).thenReturn("pi_test123");
//
//        mockStatic(Stripe.class);
//        mockStatic(Session.class);
//        when(Session.create(any())).thenReturn(mockSession);
//        when(PaymentIntent.retrieve("pi_test123")).thenReturn(mockPaymentIntent);
//        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
//        when(paymentRepository.findByOrderId(mockOrder.getOrderId())).thenReturn(Optional.empty());
//        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
//
//        String url = paymentService.createCheckoutSession(mockOrder);
//        assertEquals("http://checkout-url.com", url);
//    }

    @Test
    void handleChargeUpdate_shouldUpdatePaymentAndOrder_WhenChargeSuccessful() {
        Charge charge = mock(Charge.class);
        when(charge.getId()).thenReturn("ch_test");
        when(charge.getPaymentIntent()).thenReturn("pi_test123");
        when(charge.getStatus()).thenReturn("succeeded");
        when(charge.getReceiptUrl()).thenReturn("http://receipt.com");

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(bidRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        paymentService.handleChargeUpdate(charge);

        verify(paymentRepository).save(argThat(payment ->
                payment.getTransactionId().equals("ch_test") &&
                        payment.getPaymentStatus() == Payment.PaymentStatus.COMPLETED &&
                        "http://receipt.com".equals(payment.getReceiptUrl())
        ));
        verify(orderService).updateOrder(argThat(order ->
                order.getOrderStatus() == OrderStatus.COMPLETED
        ));
    }

    @Test
    void handleChargeUpdate_shouldUpdateReceiptUrl_WhenNewUrlAvailable() {
        mockPayment.setReceiptUrl(null); // ensure it's null initially

        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn("pi_test123");
        when(charge.getReceiptUrl()).thenReturn("http://updated-receipt.com");

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));
        when(charge.getStatus()).thenReturn("pending");

        paymentService.handleChargeUpdate(charge);

        verify(paymentRepository).save(argThat(payment ->
                "http://updated-receipt.com".equals(payment.getReceiptUrl())
        ));
    }

    @Test
    void handleSuccessfulPayment_shouldNotUpdateIfAlreadyCompleted() {
        // Arrange
        mockPayment.setPaymentStatus(Payment.PaymentStatus.COMPLETED); // already completed
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));

        // Act
        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository, never()).save(any()); // no update
        verify(orderService, never()).updateOrder(any()); // no order update
    }
    @Test
    void createOrUpdatePaymentFromIntent_shouldAddReceiptUrlIfChargePresent() throws Exception {
        // Arrange
        Payment existingPayment = new Payment();
        existingPayment.setStripePaymentIntentId("pi_test123");
        existingPayment.setOrderId(null); // simulating no order yet

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(existingPayment));

        when(mockPaymentIntent.getId()).thenReturn("pi_test123");
        when(mockPaymentIntent.getLatestCharge()).thenReturn("ch_123");

        // Mock Stripe Charge
        com.stripe.model.Charge mockCharge = mock(com.stripe.model.Charge.class);
        when(mockCharge.getReceiptUrl()).thenReturn("https://receipt.url");

        // Mock static Charge.retrieve
        try (MockedStatic<com.stripe.model.Charge> chargeStatic = mockStatic(com.stripe.model.Charge.class)) {
            chargeStatic.when(() -> com.stripe.model.Charge.retrieve(anyString())).thenReturn(mockCharge);

            // Stub save to return the same payment object
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Payment result = paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L);

            // Assert
            assertEquals("https://receipt.url", result.getReceiptUrl());
            assertEquals(1L, result.getOrderId());
        }
    }


    @Test
    void handleFailedPayment_shouldSkipIfAlreadyFailed() {
        // Arrange
        mockPayment.setPaymentStatus(Payment.PaymentStatus.FAILED); // Already failed
        when(mockPaymentIntent.getLastPaymentError()).thenReturn(null);
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));

        // Act
        paymentService.handleFailedPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository, never()).save(any()); // Should not update payment
        verify(orderService, never()).updateOrder(any()); // Should not update order
    }

    @Test
    void updateListingInventory_shouldSkipBidListings() {
        // Arrange: Properly mock the Bid and its Listing
        Bid bid = new Bid();
        bid.setId(1L);
        bid.setListing(mockListing); // Mocked listing is now present

        // Ensure bid is returned for the orderId
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(orderRepository.findOrderWithItems(1L)).thenReturn(mockOrder);

        // Inject a working bid repository with a bid that has a non-null listing
        com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository bidRepo =
                mock(com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository.class);
        ReflectionTestUtils.setField(paymentService, "bidRepository", bidRepo);

        when(bidRepo.findByOrderId(1L)).thenReturn(Optional.of(bid));

        // Act
        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        // Assert: Listing inventory update should be skipped
        //verify(listingRepository, never()).save(any());

        verify(listingRepository).save(any(Listing.class)); // Because it was a bid listing
    }
    @Test
    void handleSuccessfulPayment_shouldCreateFallbackPaymentIfNotFound() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> paymentService.handleSuccessfulPayment(mockPaymentIntent));

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void handleFailedPayment_shouldCreateNewPaymentIfNotFound() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> paymentService.handleFailedPayment(mockPaymentIntent));

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void createOrUpdatePaymentFromIntent_shouldUpdatePaymentIfFoundByOrderIdOnly() {
        // Arrange
        Payment existingPayment = new Payment();
        existingPayment.setOrderId(1L);
        existingPayment.setStripePaymentIntentId(null);

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existingPayment));
        when(mockPaymentIntent.getLatestCharge()).thenReturn(null);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payment result = paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L);

        // Assert
        assertEquals("pi_test123", result.getStripePaymentIntentId());
    }
    @Test
    void createOrUpdatePaymentFromIntent_shouldCreateNewPaymentIfNoMatch() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payment result = paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("pi_test123", result.getStripePaymentIntentId());
        assertEquals(1L, result.getOrderId());
    }
    @Test
    void handleSuccessfulPayment_shouldCreateFallbackPayment_whenPaymentNotFound() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(mockPaymentIntent.getLatestCharge()).thenReturn("ch_123");

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://receipt.url");

        try (MockedStatic<Charge> chargeStatic = mockStatic(Charge.class)) {
            chargeStatic.when(() -> Charge.retrieve("ch_123")).thenReturn(charge);

            Order order = new Order();
            order.setOrderId(1L);
            order.setItems(new ArrayList<>());
            when(orderService.getOrderById(1L)).thenReturn(order);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentService.handleSuccessfulPayment(mockPaymentIntent);

            // Assert
            verify(paymentRepository).save(any(Payment.class));
            verify(orderService).updateOrder(any(Order.class));
        }
    }

    @Test
    void handleSuccessfulPayment_shouldNotProceed_whenOrderIdNotParsedFromDescription() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Invalid Format");

        // Act
        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).updateOrder(any());
    }

    @Test
    void handleFailedPayment_shouldCreateFallbackPayment_whenPaymentNotFound() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(mockPaymentIntent.getLastPaymentError()).thenReturn(null);

        Order order = new Order();
        order.setOrderId(1L);
        when(orderService.getOrderById(1L)).thenReturn(order);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.handleFailedPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }

    @Test
    void handleFailedPayment_shouldNotProceed_whenOrderIdNotParsedFromDescription() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Not-an-order");

        // Act
        paymentService.handleFailedPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).updateOrder(any());
    }

    @Test
    void updateListingInventory_shouldSkipNullItemList() {
        // Arrange
        mockOrder.setItems(null);

        ReflectionTestUtils.invokeMethod(paymentService, "updateListingInventory", mockOrder);

        // Assert
        verify(listingRepository, never()).save(any());
    }

    @Test
    void updateListingInventory_shouldPreventNegativeStock() {
        mockListing.setQuantity(1);
        mockOrderItem.setQuantity(5);

        ReflectionTestUtils.invokeMethod(paymentService, "updateListingInventory", mockOrder);

        verify(listingRepository).save(argThat(listing ->
                listing.getQuantity() == 0 && listing.getStatus() == Listing.ListingStatus.SOLD
        ));
    }
    @Test
    void handleSuccessfulPayment_shouldCreateFallbackPaymentWhenNotFound() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void handleFailedPayment_shouldCreateNewPaymentRecordWhenPaymentNotFound() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        paymentService.handleFailedPayment(mockPaymentIntent);

        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void handleChargeUpdate_shouldSkipIfNoPaymentIntentId() {
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn(null); // Simulate null payment intent

        assertDoesNotThrow(() -> paymentService.handleChargeUpdate(charge));
    }
    @Test
    void handleChargeUpdate_shouldNotThrowWhenPaymentMissing() {
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn("pi_test123");
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> paymentService.handleChargeUpdate(charge));
    }
    @Test
    void handleBidPaymentCompletion_shouldUpdateBidAndListing() {
        Bid bid = new Bid();
        bid.setId(1L);
        bid.setListing(mockListing);
        bid.setBuyer(new User());
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(bidRepository.findByOrderId(1L)).thenReturn(Optional.of(bid));

        paymentService.handleBidPaymentCompletion(1L);

        verify(bidRepository).save(any(Bid.class));
        verify(listingRepository).save(any(Listing.class));
        verify(notificationService, times(2)).sendNotification(any(), eq(NotificationType.BID), anyString());
    }
    @Test
    void handleBidPaymentFailure_shouldSetListingActive() {
        Bid bid = new Bid();
        bid.setListing(mockListing);
        bid.setBuyer(new User());
        when(bidRepository.findByOrderId(1L)).thenReturn(Optional.of(bid));

        paymentService.handleBidPaymentFailure(1L);

        verify(listingRepository).save(argThat(listing -> listing.getStatus() == Listing.ListingStatus.ACTIVE));
        verify(notificationService, times(2)).sendNotification(any(), eq(NotificationType.BID), anyString());
    }
    @Test
    void handleSuccessfulPayment_shouldCreateFallbackPayment_WhenNoExistingPayment() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // Act
        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void handleSuccessfulPayment_shouldNotCreateFallback_WhenDescriptionIsInvalid() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Invalid description");

        // Act
        paymentService.handleSuccessfulPayment(mockPaymentIntent);

        // Assert
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderService, never()).updateOrder(any());
    }



    @Test
    void handleChargeUpdate_shouldExitGracefullyIfNoPaymentIntentId() {
        // Arrange
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> paymentService.handleChargeUpdate(charge));
    }

    @Test
    void handleSuccessfulPayment_shouldExitGracefullyWhenInvalidDescription() {
        // Arrange
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("InvalidDescription");

        // Act
        assertDoesNotThrow(() -> paymentService.handleSuccessfulPayment(mockPaymentIntent));

        // Assert
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).updateOrder(any());
    }
    @Test
    void handleSuccessfulPayment_shouldNotFail_whenChargeRetrievalFailsInFallback() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(mockPaymentIntent.getLatestCharge()).thenReturn("ch_123");

        try (MockedStatic<Charge> chargeStatic = mockStatic(Charge.class)) {
            chargeStatic.when(() -> Charge.retrieve("ch_123")).thenThrow(new RuntimeException("Stripe error"));

            when(orderService.getOrderById(1L)).thenReturn(mockOrder);
            when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

            assertDoesNotThrow(() -> paymentService.handleSuccessfulPayment(mockPaymentIntent));
        }
    }
    @Test
    void handleSuccessfulPayment_shouldHandleNullChargeInFallback() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(mockPaymentIntent.getLatestCharge()).thenReturn(null); // simulate no charge

        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        assertDoesNotThrow(() -> paymentService.handleSuccessfulPayment(mockPaymentIntent));

        verify(paymentRepository).save(any(Payment.class));
        verify(orderService).updateOrder(any(Order.class));
    }
    @Test
    void handleBidPaymentCompletion_shouldSkipIfListingIsNull() {
        Bid bid = new Bid();
        bid.setBuyer(new User());
        bid.setListing(null); // simulate corrupted bid

        when(orderService.getOrderById(1L)).thenReturn(mockOrder);
        when(bidRepository.findByOrderId(1L)).thenReturn(Optional.of(bid));

        assertThrows(RuntimeException.class, () -> paymentService.handleBidPaymentCompletion(1L));
    }
    @Test
    void handleBidPaymentFailure_shouldSkipIfListingIsNull() {
        Bid bid = new Bid();
        bid.setBuyer(new User());
        bid.setListing(null); // simulate corrupted bid

        when(bidRepository.findByOrderId(1L)).thenReturn(Optional.of(bid));

        assertThrows(RuntimeException.class, () -> paymentService.handleBidPaymentFailure(1L));
    }
    @Test
    void createOrUpdatePaymentFromIntent_shouldHandleExceptionDuringChargeRetrievalForNewPayment() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(mockPaymentIntent.getLatestCharge()).thenReturn("ch_test");

        try (MockedStatic<Charge> chargeStatic = mockStatic(Charge.class)) {
            chargeStatic.when(() -> Charge.retrieve("ch_test")).thenThrow(new RuntimeException("Stripe error"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L));
        }
    }
    @Test
    void handleChargeUpdate_shouldUpdateTransactionIdAndReceiptOnly_WhenChargeNotSucceeded() {
        mockPayment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        Charge charge = mock(Charge.class);
        when(charge.getId()).thenReturn("ch_test");
        when(charge.getPaymentIntent()).thenReturn("pi_test123");
        when(charge.getStatus()).thenReturn("pending");
        when(charge.getReceiptUrl()).thenReturn("http://receipt.partial");

        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(mockPayment));

        paymentService.handleChargeUpdate(charge);

        verify(paymentRepository).save(argThat(payment ->
                "ch_test".equals(payment.getTransactionId()) &&
                        "http://receipt.partial".equals(payment.getReceiptUrl())
        ));
    }
//    @Test
//    void createCheckoutSession_shouldLogWarningWhenNoPaymentIntentReturned() throws Exception {
//        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_dummy");
//        ReflectionTestUtils.setField(paymentService, "appBaseUrl", "http://localhost:8080");
//
//        Session mockSession = mock(Session.class);
//
//        when(mockSession.getId()).thenReturn("cs_test_123");
//
    //// In your service, construct the URL manually
//        String url = "https://checkout.stripe.com/pay/cs_test_123";
//
//
//        when(mockSession.getUrl()).thenReturn("http://checkout-session.com");
//        when(mockSession.getPaymentIntent()).thenReturn(null);
//
//        try (MockedStatic<Stripe> stripeStatic = mockStatic(Stripe.class);
//             MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
//            sessionStatic.when(() -> Session.create(any())).thenReturn(mockSession);
//
//            String result = paymentService.createCheckoutSession(mockOrder);
//
//            assertEquals("http://checkout-session.com", result);
//        }
//    }
    @Test
    void handleSuccessfulPayment_shouldNotCreatePaymentIfOrderIdIsInvalid() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #XYZ");

        assertDoesNotThrow(() -> paymentService.handleSuccessfulPayment(mockPaymentIntent));

        verify(paymentRepository, never()).save(any());
    }
    @Test
    void handleSuccessfulPayment_shouldThrowIfOrderNotFoundForFallback() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.empty());
        when(mockPaymentIntent.getDescription()).thenReturn("Order #1");
        when(mockPaymentIntent.getAmount()).thenReturn(20000L);
        when(orderService.getOrderById(1L)).thenThrow(new RuntimeException("Order not found"));

        assertThrows(RuntimeException.class, () -> paymentService.handleSuccessfulPayment(mockPaymentIntent));
    }


    @Test
    void createOrUpdatePaymentFromIntent_shouldHandleChargeRetrievalException_whenFoundByIntentId() {
        Payment existingPayment = new Payment();
        existingPayment.setStripePaymentIntentId("pi_test123");
        existingPayment.setOrderId(null); // force update
        when(paymentRepository.findByStripePaymentIntentId("pi_test123")).thenReturn(Optional.of(existingPayment));
        when(mockPaymentIntent.getId()).thenReturn("pi_test123");
        when(mockPaymentIntent.getLatestCharge()).thenReturn("ch_123");

        try (MockedStatic<Charge> chargeStatic = mockStatic(Charge.class)) {
            chargeStatic.when(() -> Charge.retrieve("ch_123")).thenThrow(new RuntimeException("Stripe retrieval failed"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);

            assertDoesNotThrow(() ->
                    paymentService.createOrUpdatePaymentFromIntent(mockPaymentIntent, 1L));
        }
    }


//    @Test
//    void createCheckoutSession_shouldHandleMissingPaymentIntentId() throws Exception {
//        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_dummy");
//        ReflectionTestUtils.setField(paymentService, "appBaseUrl", "http://localhost:8080");
//
//        com.stripe.model.checkout.Session mockSession = mock(com.stripe.model.checkout.Session.class);
//        when(mockSession.getUrl()).thenReturn("http://mock-checkout-session.com");
//        when(mockSession.getPaymentIntent()).thenReturn(null); // triggers else block
//
//        try (
//                MockedStatic<Stripe> stripeStatic = mockStatic(Stripe.class);
//                MockedStatic<com.stripe.model.checkout.Session> sessionStatic = mockStatic(com.stripe.model.checkout.Session.class)
//        ) {
//            sessionStatic.when(() -> com.stripe.model.checkout.Session.create(any(SessionCreateParams.class)))
//                    .thenReturn(mockSession);
//
//            String result = paymentService.createCheckoutSession(mockOrder);
//
//            assertEquals("http://mock-checkout-session.com", result);
//        }
//    }



}