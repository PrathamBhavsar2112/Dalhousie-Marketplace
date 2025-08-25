package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.BidRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotificationService notificationService;

    private static final BigDecimal STRIPE_AMOUNT_DIVISOR = BigDecimal.valueOf(100);

    public String createCheckoutSession(Order order) throws StripeException {
        logger.info("Creating checkout session for order ID: {} with total price: {}",
                order.getOrderId(), order.getTotalPrice());
        validateStripeConfig();
        Stripe.apiKey = stripeApiKey;

        Session session = createStripeSession(order);
        handleInitialPaymentRecord(session, order);
        logger.info("Successfully created checkout session for order: {}", order.getOrderId());
        return session.getUrl();
    }

    private void validateStripeConfig() {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new RuntimeException("Stripe API key is not configured");
        }
    }

    private Session createStripeSession(Order order) throws StripeException {
        long amountInCents = order.getTotalPrice().multiply(new BigDecimal("100")).longValue();
        SessionCreateParams params = buildSessionParams(order, amountInCents);
        return Session.create(params);
    }

    private SessionCreateParams buildSessionParams(Order order, long amountInCents) {
        SessionCreateParams.Mode mode = SessionCreateParams.Mode.PAYMENT;
        String successUrl = appBaseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = appBaseUrl + "/payment/cancel";
        String clientRefId = order.getOrderId().toString();
        SessionCreateParams.PaymentIntentData paymentIntentData = buildPaymentIntentData(order);
        SessionCreateParams.LineItem lineItem = buildLineItem(order, amountInCents);

        SessionCreateParams.Builder builder = SessionCreateParams.builder();
        builder.setMode(mode);
        builder.setSuccessUrl(successUrl);
        builder.setCancelUrl(cancelUrl);
        builder.setClientReferenceId(clientRefId);
        builder.setPaymentIntentData(paymentIntentData);
        builder.addLineItem(lineItem);

        return builder.build();
    }

    private SessionCreateParams.PaymentIntentData buildPaymentIntentData(Order order) {
        Long orderId = order.getOrderId();
        String label = "Order #";
        String description = label + orderId;
        String orderIdStr = orderId.toString();

        SessionCreateParams.PaymentIntentData.SetupFutureUsage usage =
                SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION;

        SessionCreateParams.PaymentIntentData.Builder builder =
                SessionCreateParams.PaymentIntentData.builder();

        builder.setDescription(description);
        builder.putMetadata("orderId", orderIdStr);
        builder.setSetupFutureUsage(usage);

        return builder.build();
    }

    private SessionCreateParams.LineItem buildLineItem(Order order, long amountInCents) {
        String currency = "cad";
        Long quantity = 1L;

        Long orderId = order.getOrderId();
        String productLabel = "Order #";
        String productName = productLabel + orderId;

        SessionCreateParams.LineItem.PriceData.ProductData.Builder productBuilder =
                SessionCreateParams.LineItem.PriceData.ProductData.builder();
        productBuilder.setName(productName);
        SessionCreateParams.LineItem.PriceData.ProductData productData = productBuilder.build();

        SessionCreateParams.LineItem.PriceData.Builder priceBuilder =
                SessionCreateParams.LineItem.PriceData.builder();
        priceBuilder.setCurrency(currency);
        priceBuilder.setUnitAmount(amountInCents);
        priceBuilder.setProductData(productData);
        SessionCreateParams.LineItem.PriceData priceData = priceBuilder.build();

        SessionCreateParams.LineItem.Builder lineItemBuilder = SessionCreateParams.LineItem.builder();
        lineItemBuilder.setPriceData(priceData);
        lineItemBuilder.setQuantity(quantity);

        return lineItemBuilder.build();
    }


    private void handleInitialPaymentRecord(Session session, Order order) {
        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId != null) {
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                createOrUpdatePaymentFromIntent(paymentIntent, order.getOrderId());
                logger.info("Payment record created/updated for order {} and payment intent {}",
                        order.getOrderId(), paymentIntentId);
            } catch (Exception e) {
                logger.error("Error creating/updating payment record: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("No payment intent ID returned from session creation");
        }
    }

    @Transactional
    public void handleChargeUpdate(Charge charge) {
        logger.info("Processing charge update with ID: {}", charge.getId());
        String paymentIntentId = charge.getPaymentIntent();
        if (paymentIntentId == null) {
            logger.warn("Charge has no associated payment intent ID: {}", charge.getId());
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (paymentOpt.isPresent()) {
            processExistingChargeUpdate(paymentOpt.get(), charge);
        } else {
            logger.warn("No payment found for payment intent: {}", paymentIntentId);
        }
    }

    private void processExistingChargeUpdate(Payment payment, Charge charge) {
        boolean updated = updatePaymentDetails(payment, charge);
        if ("succeeded".equals(charge.getStatus()) && payment.getPaymentStatus() != Payment.PaymentStatus.COMPLETED) {
            handleChargeSuccess(payment, charge);
            updated = true;
        }
        if (updated) {
            paymentRepository.save(payment);
            logger.info("Saved updated payment: {}", payment.getId());
        }
    }

    private boolean updatePaymentDetails(Payment payment, Charge charge) {
        boolean updated = false;

        String newReceiptUrl = charge.getReceiptUrl();
        String existingReceiptUrl = payment.getReceiptUrl();

        if (shouldUpdateReceiptUrl(existingReceiptUrl, newReceiptUrl)) {
            logger.info("Updating receipt URL for payment: {}", payment.getId());
            payment.setReceiptUrl(newReceiptUrl);
            updated = true;
        }

        if (shouldUpdateTransactionId(payment.getTransactionId(), charge.getId())) {
            payment.setTransactionId(charge.getId());
            updated = true;
        }

        return updated;
    }

    private boolean shouldUpdateReceiptUrl(String existing, String latest) {
        return latest != null && (existing == null || !existing.equals(latest));
    }

    private boolean shouldUpdateTransactionId(String transactionId, String chargeId) {
        return transactionId == null && chargeId != null;
    }


    private void handleChargeSuccess(Payment payment, Charge charge) {
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        updateOrderStatus(payment.getOrderId(), OrderStatus.COMPLETED);
        updateListingInventory(orderService.getOrderById(payment.getOrderId()));
        bidRepository.findByOrderId(payment.getOrderId())
                .ifPresent(bid -> handleBidPaymentCompletion(payment.getOrderId()));
        logger.info("Updated payment status to COMPLETED for payment: {}", payment.getId());
    }

    @Transactional
    public void handleSuccessfulPayment(PaymentIntent paymentIntent) {
        logger.info("Processing successful payment with intent ID: {}", paymentIntent.getId());
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

        if (paymentOpt.isPresent()) {
            processExistingSuccessfulPayment(paymentOpt.get(), paymentIntent);
        } else {
            processNewSuccessfulPayment(paymentIntent);
        }
    }

    private void processExistingSuccessfulPayment(Payment payment, PaymentIntent paymentIntent) {
        if (payment.getPaymentStatus() != Payment.PaymentStatus.COMPLETED) {
            updatePaymentOnSuccess(payment, paymentIntent);
            paymentRepository.save(payment);
            logger.info("Saved updated payment status: {}", payment.getPaymentStatus());
            handleOrderAndInventoryUpdate(payment.getOrderId());
        } else {
            logger.info("Payment already marked as completed. Skipping update.");
        }
    }

    private void updatePaymentOnSuccess(Payment payment, PaymentIntent paymentIntent) {
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        updatePaymentWithChargeDetails(payment, paymentIntent);
    }

    private void handleOrderAndInventoryUpdate(Long orderId) {
        Order order = updateOrderStatus(orderId, OrderStatus.COMPLETED);
        updateListingInventory(order);
        bidRepository.findByOrderId(orderId)
                .ifPresent(bid -> handleBidPaymentCompletion(orderId));
        logger.info("Updated order status to COMPLETED for order: {}", orderId);
    }

    private void processNewSuccessfulPayment(PaymentIntent paymentIntent) {
        logPaymentNotFound(paymentIntent);

        Long orderId = extractOrderIdFromDescription(paymentIntent.getDescription());
        if (orderId == null) {
            logFallbackPaymentFailure(paymentIntent);
            return;
        }

        Payment payment = createFallbackPayment(paymentIntent, orderId);
        paymentRepository.save(payment);

        handleOrderAndInventoryUpdate(orderId);

        logger.info("Created fallback payment record for order ID: {}", orderId);
    }

    private void logPaymentNotFound(PaymentIntent paymentIntent) {
        logger.error("Payment not found for PaymentIntent: {}", paymentIntent.getId());
    }

    private void logFallbackPaymentFailure(PaymentIntent paymentIntent) {
        String errorMsg = "Cannot create fallback payment: Unable to determine order ID from description: {}";
        logger.error(errorMsg, paymentIntent.getDescription());
    }

    private Long extractOrderIdFromDescription(String description) {
        if (description != null && description.startsWith("Order #")) {
            try {
                return Long.parseLong(description.substring("Order #".length()));
            } catch (NumberFormatException e) {
                logger.error("Failed to parse order ID from description: {}", description);
            }
        }
        return null;
    }

    private Payment createFallbackPayment(PaymentIntent paymentIntent, Long orderId) {
        Payment payment = new Payment();
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setOrderId(orderId);
        payment.setPaymentMethod(Payment.PaymentMethod.STRIPE);
        payment.setCurrency("CAD");
        payment.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()).divide(STRIPE_AMOUNT_DIVISOR));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        updatePaymentWithChargeDetails(payment, paymentIntent);
        return payment;
    }

    private void updatePaymentWithChargeDetails(Payment payment, PaymentIntent paymentIntent) {
        String chargeId = paymentIntent.getLatestCharge();
        if (chargeId != null) {
            payment.setTransactionId(chargeId);
            logger.info("Updated transaction ID: {}", chargeId);
            updateReceiptUrl(payment, chargeId);
        }
    }

    private void updateReceiptUrl(Payment payment, String chargeId) {
        try {
            Stripe.apiKey = stripeApiKey;
            Charge charge = Charge.retrieve(chargeId);
            if (charge.getReceiptUrl() != null) {
                payment.setReceiptUrl(charge.getReceiptUrl());
                logger.info("Updated receipt URL: {}", charge.getReceiptUrl());
            }
        } catch (Exception e) {
            logger.warn("Could not retrieve charge for receipt URL: {}", e.getMessage());
        }
    }

    @Transactional
    public void handleFailedPayment(PaymentIntent paymentIntent) {
        logger.info("Processing failed payment with intent ID: {}", paymentIntent.getId());
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

        if (paymentOpt.isPresent()) {
            processExistingFailedPayment(paymentOpt.get(), paymentIntent);
        } else {
            processNewFailedPayment(paymentIntent);
        }
    }

    private void processExistingFailedPayment(Payment payment, PaymentIntent paymentIntent) {
        if (payment.getPaymentStatus() != Payment.PaymentStatus.FAILED) {
            updatePaymentOnFailure(payment, paymentIntent);
            paymentRepository.save(payment);
            handleFailedOrderUpdate(payment.getOrderId(), paymentIntent);
            logger.info("Processed failed payment for order: {}", payment.getOrderId());
        } else {
            logger.info("Payment already marked as failed. Skipping update.");
        }
    }

    private void updatePaymentOnFailure(Payment payment, PaymentIntent paymentIntent) {
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        String failureReason = "Unknown error";
        if (paymentIntent.getLastPaymentError() != null) {
            failureReason = paymentIntent.getLastPaymentError().getMessage();
        }

        payment.setFailureReason(failureReason);
    }


    private void handleFailedOrderUpdate(Long orderId, PaymentIntent paymentIntent) {
        Order order = updateOrderStatus(orderId, OrderStatus.CANCELLED);
        bidRepository.findByOrderId(orderId)
                .ifPresent(bid -> handleBidPaymentFailure(orderId));
    }

    private void processNewFailedPayment(PaymentIntent paymentIntent) {
        String paymentIntentId = paymentIntent.getId();
        String description = paymentIntent.getDescription();

        logger.error("Payment not found for PaymentIntent: {}", paymentIntentId);

        Long orderId = extractOrderIdFromDescription(description);
        if (orderId == null) {
            String errorMsg = "Cannot create failed payment record: "
                    + "Unable to determine order ID from description: {}";
            logger.error(errorMsg, description);
            return;
        }

        Payment payment = createFailedPayment(paymentIntent, orderId);
        paymentRepository.save(payment);

        handleFailedOrderUpdate(orderId, paymentIntent);
        logger.info(
                "Created new payment record with failed status for order ID: {}",
                orderId
        );
    }

    private static final BigDecimal CENTS_IN_DOLLAR = BigDecimal.valueOf(100);
    private Payment createFailedPayment(PaymentIntent paymentIntent, Long orderId) {
        Payment payment = new Payment();
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setOrderId(orderId);
        payment.setPaymentMethod(Payment.PaymentMethod.STRIPE);
        payment.setCurrency("CAD");

        BigDecimal rawAmount = BigDecimal.valueOf(paymentIntent.getAmount());
        BigDecimal amount = rawAmount.divide(CENTS_IN_DOLLAR);
        payment.setAmount(amount);

        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        String failureReason = getFailureReason(paymentIntent);
        payment.setFailureReason(failureReason);

        return payment;
    }

    private String getFailureReason(PaymentIntent paymentIntent) {
        if (paymentIntent.getLastPaymentError() != null) {
            return paymentIntent.getLastPaymentError().getMessage();
        }
        return "Unknown error";
    }

    @Transactional
    public Payment createOrUpdatePaymentFromIntent(PaymentIntent paymentIntent, Long orderId) {
        logger.info("Creating or updating payment record for payment intent ID: {}", paymentIntent.getId());
        Optional<Payment> existingByIntentId = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());
        Optional<Payment> existingByOrderId = paymentRepository.findByOrderId(orderId);

        if (existingByIntentId.isPresent()) {
            return updateExistingPaymentByIntent(existingByIntentId.get(), paymentIntent, orderId);
        } else if (existingByOrderId.isPresent()) {
            return updateExistingPaymentByOrder(existingByOrderId.get(), paymentIntent);
        } else {
            return createNewPayment(paymentIntent, orderId);
        }
    }

    private Payment updateExistingPaymentByIntent(Payment payment, PaymentIntent paymentIntent, Long orderId) {
        boolean updated = payment.getOrderId() == null;
        if (updated) {
            payment.setOrderId(orderId);
            logger.info("Updated order ID for existing payment record with payment intent ID: {}",
                    paymentIntent.getId());
        }
        updateReceiptUrlIfNeeded(payment, paymentIntent);
        payment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private Payment updateExistingPaymentByOrder(Payment payment, PaymentIntent paymentIntent) {
        Long orderId = payment.getOrderId();
        String oldIntentId = payment.getStripePaymentIntentId();
        String newIntentId = paymentIntent.getId();

        logger.info("Updating payment record for order ID: {}", orderId);
        logger.info("Changing payment intent ID from {} to {}", oldIntentId, newIntentId);

        payment.setStripePaymentIntentId(newIntentId);
        updateReceiptUrlIfNeeded(payment, paymentIntent);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    private void updateReceiptUrlIfNeeded(Payment payment, PaymentIntent paymentIntent) {
        String latestCharge = paymentIntent.getLatestCharge();
        boolean isReceiptUrlMissing = payment.getReceiptUrl() == null || payment.getReceiptUrl().isEmpty();

        if (latestCharge != null && isReceiptUrlMissing) {
            updateReceiptUrl(payment, latestCharge);
        }
    }

    private static final BigDecimal STRIPE_CENTS_DIVISOR = BigDecimal.valueOf(100);
    private Payment createNewPayment(PaymentIntent paymentIntent, Long orderId) {
        Payment payment = new Payment();
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setOrderId(orderId);
        payment.setPaymentMethod(Payment.PaymentMethod.STRIPE);
        payment.setCurrency("CAD");
        payment.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()).divide(STRIPE_CENTS_DIVISOR));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        updateReceiptUrlIfNeeded(payment, paymentIntent);

        logger.info(
                "Created new payment record for order ID: {} with payment intent ID: {}",
                orderId,
                paymentIntent.getId()
        );

        return paymentRepository.save(payment);
    }

    private void updateListingInventory(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            logger.info("No items to update inventory for order ID: {}", order.getOrderId());
            return;
        }
        logger.info("Updating inventory for order ID: {}", order.getOrderId());
        processOrderItemsInventory(order);
    }

    private void processOrderItemsInventory(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            Listing listing = orderItem.getListing();
            if (listing != null && !isBidPurchase(order.getOrderId())) {
                updateListingQuantity(listing, orderItem.getQuantity());
                listingRepository.save(listing);
                logger.info("Updated inventory for listing ID: {}, new quantity: {}",
                        listing.getId(), listing.getQuantity());
            }
        }
    }

    private boolean isBidPurchase(Long orderId) {
        return bidRepository.findByOrderId(orderId).isPresent();
    }

    private void updateListingQuantity(Listing listing, int purchasedQuantity) {
        int newQuantity = Math.max(listing.getQuantity() - purchasedQuantity, 0);
        listing.setQuantity(newQuantity);
        if (newQuantity <= 0) {
            listing.setStatus(Listing.ListingStatus.SOLD);
            logger.info("Listing ID: {} is now SOLD (out of stock)", listing.getId());
        }
    }

    @Transactional
    public void handleBidPaymentCompletion(Long orderId) {
        Order order = orderService.getOrderById(orderId);
        Optional<Bid> bidOpt = bidRepository.findByOrderId(orderId);
        if (bidOpt.isPresent()) {
            processBidCompletion(bidOpt.get(), order);
        }
    }

    private void processBidCompletion(Bid bid, Order order) {
        Listing listing = bid.getListing();

        bid.setStatus(BidStatus.PAID);
        bidRepository.save(bid);

        listing.setStatus(Listing.ListingStatus.SOLD);
        listingRepository.save(listing);

        sendBidCompletionNotifications(bid, listing);

        Long bidId = bid.getId();
        Long orderId = order.getOrderId();
        Long listingId = listing.getId();
        logger.info("Bid payment completed:");
        logger.info("→ Bid ID: {}", bidId);
        logger.info("→ Order ID: {}", orderId);
        logger.info("→ Listing ID: {}", listingId);
    }

    private void sendBidCompletionNotifications(Bid bid, Listing listing) {
        String title = listing.getTitle();

        String buyerMessage = "Your payment for " + title + " has been completed.";
        notificationService.sendNotification(bid.getBuyer(), NotificationType.BID, buyerMessage);

        String sellerMessage = "Payment received for your listing: " + title;
        notificationService.sendNotification(listing.getSeller(), NotificationType.BID, sellerMessage);
    }

    @Transactional
    public void handleBidPaymentFailure(Long orderId) {
        Optional<Bid> bidOpt = bidRepository.findByOrderId(orderId);
        if (bidOpt.isPresent()) {
            processBidFailure(bidOpt.get());
        }
    }

    private void processBidFailure(Bid bid) {
        Listing listing = bid.getListing();
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listingRepository.save(listing);
        sendBidFailureNotifications(bid, listing);

        Long bidId = bid.getId();
        Long orderId = bid.getOrderId();
        Long listingId = listing.getId();

        logger.info(
                "Bid payment failed: Bid ID: {}, Order ID: {}, Listing ID: {}",
                bidId, orderId, listingId
        );
    }

    private void sendBidFailureNotifications(Bid bid, Listing listing) {
        String title = listing.getTitle();

        String buyerMessage = "Your payment for " + title + " has failed. Please try again.";
        notificationService.sendNotification(bid.getBuyer(), NotificationType.BID, buyerMessage);

        String sellerMessage = "Payment failed for your listing: " + title + ". The item is now available again.";
        notificationService.sendNotification(listing.getSeller(), NotificationType.BID, sellerMessage);
    }

    private Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderService.getOrderById(orderId);
        order.setOrderStatus(status);
        orderService.updateOrder(order);
        return order;
    }
}