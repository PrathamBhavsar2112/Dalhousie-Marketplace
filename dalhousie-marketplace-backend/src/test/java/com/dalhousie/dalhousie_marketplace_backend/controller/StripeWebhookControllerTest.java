package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
import com.dalhousie.dalhousie_marketplace_backend.repository.PaymentRepository;
import com.dalhousie.dalhousie_marketplace_backend.model.Payment;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class StripeWebhookControllerTest {

    @InjectMocks
    private StripeWebhookController stripeWebhookController;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    private MockMvc mockMvc;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private String validJwtToken = "valid_jwt_token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(stripeWebhookController).build();
    }




    @Test
    void testHandleStripeWebhook_invalidSignature() throws Exception {
        String payload = "{\"id\":\"evt_test_invalid_signature\",\"type\":\"payment_intent.succeeded\"}";
        String sigHeader = "t=12345,v1=invalid_signature";

        // Perform POST request to /webhook with an invalid signature
        mockMvc.perform(post("/webhook")
                        .content(payload)
                        .header("Stripe-Signature", sigHeader))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("Invalid signature"));
    }

    @Test
    void testHandleStripeWebhook_invalidPayload() throws Exception {
        String payload = "{\"id\":\"evt_test_invalid_payload\",\"type\":\"payment_intent.succeeded\"}";
        String sigHeader = "t=12345,v1=1234567890";

        // Perform POST request to /webhook with invalid payload (mocking a failed payload)
        mockMvc.perform(post("/webhook")
                        .content(payload)
                        .header("Stripe-Signature", sigHeader))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("Failed to deserialize Stripe event"));
    }


}
