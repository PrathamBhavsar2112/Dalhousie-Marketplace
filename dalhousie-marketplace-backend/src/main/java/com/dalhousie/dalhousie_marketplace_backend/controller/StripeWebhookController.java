package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.net.Webhook;
import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import com.dalhousie.dalhousie_marketplace_backend.model.Payment;

import java.util.Map;
import java.util.Optional;

@RestController
public class StripeWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        logger.info("Received Stripe webhook");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Webhook event type: {}", event.getType());
        } catch (SignatureVerificationException e) {
            logger.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (!dataObjectDeserializer.getObject().isPresent()) {
            logger.error("Failed to deserialize Stripe event object");
            return ResponseEntity.badRequest().body("Failed to deserialize Stripe event");
        }

        StripeObject stripeObject = dataObjectDeserializer.getObject().get();
        logger.info("Processing Stripe object type: {}", stripeObject.getClass().getSimpleName());

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    logger.info("Processing payment_intent.succeeded event");
                    PaymentIntent successfulPaymentIntent = (PaymentIntent) stripeObject;
                    logger.info("Payment Intent ID: {}", successfulPaymentIntent.getId());
                    logger.info("Payment Intent status: {}", successfulPaymentIntent.getStatus());
                    logger.info("Payment Intent amount: {}", successfulPaymentIntent.getAmount());

                    try {
                        // First ensure the payment record exists
                        String successDescription = successfulPaymentIntent.getDescription();
                        if (successDescription != null && successDescription.startsWith("Order #")) {
                            try {
                                Long orderId = Long.parseLong(successDescription.substring("Order #".length()));
                                logger.info("Extracted order ID from description: {}", orderId);

                                // Ensure payment record exists before processing success
                                paymentService.createOrUpdatePaymentFromIntent(successfulPaymentIntent, orderId);
                            } catch (NumberFormatException e) {
                                logger.error("Failed to parse order ID from description: {}", successDescription);
                            }
                        }

                        paymentService.handleSuccessfulPayment(successfulPaymentIntent);
                        logger.info("Successfully processed payment_intent.succeeded event");
                    } catch (Exception e) {
                        logger.error("Error in handleSuccessfulPayment: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error processing payment success: " + e.getMessage());
                    }
                    break;

                case "payment_intent.payment_failed":
                    logger.info("Processing payment_intent.payment_failed event");
                    PaymentIntent failedPaymentIntent = (PaymentIntent) stripeObject;
                    logger.info("Failed Payment Intent ID: {}", failedPaymentIntent.getId());

                    // Ensure the payment record exists before processing failure
                    String failedDescription = failedPaymentIntent.getDescription();
                    if (failedDescription != null && failedDescription.startsWith("Order #")) {
                        try {
                            Long orderId = Long.parseLong(failedDescription.substring("Order #".length()));
                            logger.info("Extracted order ID from description: {}", orderId);

                            // Ensure payment record exists before processing failure
                            paymentService.createOrUpdatePaymentFromIntent(failedPaymentIntent, orderId);
                        } catch (NumberFormatException e) {
                            logger.error("Failed to parse order ID from description: {}", failedDescription);
                        }
                    }

                    paymentService.handleFailedPayment(failedPaymentIntent);
                    logger.info("Successfully processed payment_intent.payment_failed event");
                    break;

                case "payment_intent.created":
                    logger.info("Processing payment_intent.created event");
                    PaymentIntent createdPaymentIntent = (PaymentIntent) stripeObject;
                    logger.info("Created Payment Intent ID: {}", createdPaymentIntent.getId());

                    // Create a new payment record in the database using the service method
                    try {
                        // Extract order ID from the description which has format "Order #X"
                        Long orderId = null;
                        String description = createdPaymentIntent.getDescription();
                        logger.info("Payment intent description: {}", description);

                        if (description != null && description.startsWith("Order #")) {
                            try {
                                orderId = Long.parseLong(description.substring("Order #".length()));
                                logger.info("Extracted order ID from description: {}", orderId);

                                // Use the payment service to create or update the payment record
                                paymentService.createOrUpdatePaymentFromIntent(createdPaymentIntent, orderId);
                                logger.info("Successfully processed payment record for intent ID: {}", createdPaymentIntent.getId());
                            } catch (NumberFormatException e) {
                                logger.error("Failed to parse order ID from description: {}", description);
                            }
                        } else {
                            logger.warn("No order ID found in payment intent description. Cannot create payment record.");
                        }
                    } catch (Exception e) {
                        logger.error("Error creating payment record for payment intent: {}", e.getMessage(), e);
                    }
                    break;

                case "charge.succeeded":
                    logger.info("Processing charge.succeeded event");
                    Charge successfulCharge = (Charge) stripeObject;
                    logger.info("Charge ID: {}, Amount: {}, Status: {}",
                            successfulCharge.getId(),
                            successfulCharge.getAmount(),
                            successfulCharge.getStatus());

                    // If the charge is linked to a payment intent, process it
                    if (successfulCharge.getPaymentIntent() != null) {
                        try {
                            Stripe.apiKey = stripeApiKey;
                            PaymentIntent paymentIntent = PaymentIntent.retrieve(successfulCharge.getPaymentIntent());

                            // First ensure the payment record exists
                            String description = paymentIntent.getDescription();
                            if (description != null && description.startsWith("Order #")) {
                                try {
                                    Long orderId = Long.parseLong(description.substring("Order #".length()));
                                    logger.info("Extracted order ID from description: {}", orderId);

                                    // Ensure payment record exists before processing success
                                    paymentService.createOrUpdatePaymentFromIntent(paymentIntent, orderId);
                                } catch (NumberFormatException e) {
                                    logger.error("Failed to parse order ID from description: {}", description);
                                }
                            }

                            paymentService.handleSuccessfulPayment(paymentIntent);
                            logger.info("Successfully processed payment through charge.succeeded event");
                        } catch (StripeException e) {
                            logger.error("Error retrieving payment intent from charge: {}", e.getMessage(), e);
                        }
                    }
                    break;

                case "checkout.session.completed":
                    logger.info("Processing checkout.session.completed event");
                    Session completedSession = (Session) stripeObject;
                    logger.info("Completed Checkout Session ID: {}", completedSession.getId());

                    // Extract payment intent from the session and process it
                    if (completedSession.getPaymentIntent() != null) {
                        try {
                            Stripe.apiKey = stripeApiKey;
                            PaymentIntent paymentIntent = PaymentIntent.retrieve(completedSession.getPaymentIntent());

                            // First ensure the payment record exists
                            String description = paymentIntent.getDescription();
                            if (description != null && description.startsWith("Order #")) {
                                try {
                                    Long orderId = Long.parseLong(description.substring("Order #".length()));
                                    logger.info("Extracted order ID from description: {}", orderId);

                                    // Ensure payment record exists before processing success
                                    paymentService.createOrUpdatePaymentFromIntent(paymentIntent, orderId);
                                } catch (NumberFormatException e) {
                                    logger.error("Failed to parse order ID from description: {}", description);
                                }
                            }

                            // Now handle successful payment
                            paymentService.handleSuccessfulPayment(paymentIntent);
                            logger.info("Successfully processed payment through checkout.session.completed event");
                        } catch (StripeException e) {
                            logger.error("Error retrieving payment intent from session: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.warn("No payment intent found in completed checkout session");
                    }
                    break;

                ////
                case "charge.updated":
                    logger.info("Processing charge.updated event");
                    Charge updatedCharge = (Charge) stripeObject;
                    logger.info("Charge updated for ID: {}", updatedCharge.getId());

                    // Check if receipt_url is in the previous_attributes
                    if (event.getData() != null && event.getData().getPreviousAttributes() != null) {
                        Map<String, Object> previousAttributes = event.getData().getPreviousAttributes();
                        if (previousAttributes.containsKey("receipt_url")) {
                            logger.info("Receipt URL has been updated");

                            // Get the new receipt URL
                            String receiptUrl = updatedCharge.getReceiptUrl();
                            String paymentIntentId = updatedCharge.getPaymentIntent();

                            if (paymentIntentId != null && receiptUrl != null) {
                                Optional<Payment> payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId);

                                if (payment.isPresent()) {
                                    logger.info("Updating receipt URL for payment intent: {}", paymentIntentId);
                                    Payment paymentEntity = payment.get();
                                    paymentEntity.setReceiptUrl(receiptUrl);
                                    paymentRepository.save(paymentEntity);
                                    logger.info("Successfully updated receipt URL for payment ID: {}", paymentEntity.getId());
                                } else {
                                    logger.warn("No payment found for payment intent: {}", paymentIntentId);
                                }
                            }
                        } else {
                            // Forward to service method for general charge updates
                            paymentService.handleChargeUpdate(updatedCharge);
                        }
                    } else {
                        // Forward to service method for general charge updates
                        paymentService.handleChargeUpdate(updatedCharge);
                    }

                    // Rest of the charge.updated handling remains the same
                    if (updatedCharge.getPaymentIntent() != null) {
                        logger.info("Charge updated for Payment Intent ID: {}", updatedCharge.getPaymentIntent());
                        // Get the full payment intent to process it
                        try {
                            Stripe.apiKey = stripeApiKey;
                            PaymentIntent paymentIntent = PaymentIntent.retrieve(updatedCharge.getPaymentIntent());
                            if ("succeeded".equals(paymentIntent.getStatus())) {
                                paymentService.handleSuccessfulPayment(paymentIntent);
                                logger.info("Successfully processed payment through charge.updated event");
                            } else if ("canceled".equals(paymentIntent.getStatus()) ||
                                    "requires_payment_method".equals(paymentIntent.getStatus())) {
                                paymentService.handleFailedPayment(paymentIntent);
                                logger.info("Successfully processed failed payment through charge.updated event");
                            }
                        } catch (Exception e) {
                            logger.error("Error processing charge.updated event: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.info("Charge updated but no payment intent found");
                    }
                    break;

                case "charge.failed":
                    logger.info("Processing charge.failed event");
                    Charge failedCharge = (Charge) stripeObject;
                    logger.info("Failed Charge ID: {}, Reason: {}",
                            failedCharge.getId(),
                            failedCharge.getFailureMessage());

                    // If the charge is linked to a payment intent, process it as a failed payment
                    if (failedCharge.getPaymentIntent() != null) {
                        try {
                            Stripe.apiKey = stripeApiKey;
                            PaymentIntent paymentIntent = PaymentIntent.retrieve(failedCharge.getPaymentIntent());
                            paymentService.handleFailedPayment(paymentIntent);
                            logger.info("Successfully processed failed payment through charge.failed event");
                        } catch (StripeException e) {
                            logger.error("Error retrieving payment intent from failed charge: {}", e.getMessage(), e);
                        }
                    }
                    break;

                case "payment_intent.canceled":
                    logger.info("Processing payment_intent.canceled event");
                    PaymentIntent canceledPaymentIntent = (PaymentIntent) stripeObject;
                    logger.info("Canceled Payment Intent ID: {}", canceledPaymentIntent.getId());
                    paymentService.handleFailedPayment(canceledPaymentIntent);
                    break;

                case "payment_method.attached":
                    logger.info("Processing payment_method.attached event, no action needed");
                    // Just log it, no further action needed
                    break;

                case "payment_method.detached":
                    logger.info("Processing payment_method.detached event, no action needed");
                    // Just log it, no further action needed
                    break;

                default:
                    logger.info("Unhandled event type: {}", event.getType());
                    return ResponseEntity.ok().body("Unhandled event type: " + event.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing webhook: {} for event type: {}", e.getMessage(), event.getType(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }

        return ResponseEntity.ok().body("Webhook processed successfully");
    }
}