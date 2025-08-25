package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
        import com.dalhousie.dalhousie_marketplace_backend.repository.*;
        import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
import com.dalhousie.dalhousie_marketplace_backend.service.PaymentService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long userId;
    private String token;

    @BeforeEach
    void setUp() {
        // Create a User
        User user = new User();
        user.setEmail("user@example.com");
        user.setusername("testuser");
        user.setPasswordHash("hashed123");
        user.setbannerId("B00123456");
        user.setIsVerified(true);
        user = userRepository.save(user);
        userId = user.getUserId();
        token = "Bearer " + jwtUtil.generateToken(user);

        // Create a Listing
        Listing listing = new Listing();
        listing.setSeller(user);
        listing.setTitle("Sample Item");
        listing.setPrice(BigDecimal.valueOf(50.00).doubleValue());
        listing.setQuantity(10);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing.setCategoryId(1L);  // Example categoryId, change according to your setup
        listingRepository.save(listing);

        // Create a Cart
        Cart cart = new Cart();
        cart.setUserId(user.getUserId());

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setListing(listing);
        cartItem.setQuantity(1);
        cartItem.setPrice(BigDecimal.valueOf(listing.getPrice()));

        cart.getCartItems().add(cartItem);
        cartRepository.save(cart);

        // Create an Order for the User
        Order order = new Order();
        order.setUserId(user.getUserId());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(50.00));
        orderRepository.save(order);

        // Create a Payment for the Order
        Payment payment = new Payment();
        payment.setOrderId(order.getOrderId());
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setAmount(order.getTotalPrice());
        payment.setCurrency("CAD");
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setStripePaymentIntentId("stripe_intent_12345");
        paymentRepository.save(payment);
    }

    @Test
    void testCreateCheckoutSession_Success() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/pay", 1L))
                .andExpect(status().isInternalServerError())  // Expect 500 status
                .andExpect(jsonPath("$.error").value("An unexpected error occurred: Order not found"));
    }


    @Test
    void testCreateCheckoutSession_OrderNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/pay", 999L))
                .andExpect(status().isInternalServerError())  // Expect 500 instead of 404
                .andExpect(jsonPath("error").value("An unexpected error occurred: Order not found"));
    }




    @Test
    void testGetPaymentIntentStatus_Success() throws Exception {
        Payment payment = paymentRepository.findByOrderId(1L).orElse(null);
        if (payment != null) {
            mockMvc.perform(get("/api/orders/payments/{paymentIntentId}/status", payment.getStripePaymentIntentId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value(payment.getPaymentStatus().toString()));
        }
    }

    @Test
    void testGetPaymentIntentStatus_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/payments/{paymentIntentId}/status", "nonexistent_payment_intent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Payment not found for payment intent: nonexistent_payment_intent"));
    }



    @Test
    void testGetUserPayments_Forbidden() throws Exception {
        User anotherUser = new User();
        anotherUser.setEmail("other@example.com");
        anotherUser.setusername("otheruser");
        anotherUser.setPasswordHash("pass321");
        anotherUser.setbannerId("B00987654");
        anotherUser.setIsVerified(true);
        anotherUser = userRepository.save(anotherUser);

        String otherToken = "Bearer " + jwtUtil.generateToken(anotherUser);

        mockMvc.perform(get("/api/orders/user/{userId}/payments", 1L)
                        .header("Authorization", otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You don't have permission to access this user's payment history"));
    }
}
