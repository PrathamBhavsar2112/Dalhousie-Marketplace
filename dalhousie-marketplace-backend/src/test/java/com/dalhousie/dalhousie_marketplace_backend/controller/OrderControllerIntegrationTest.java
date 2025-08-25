package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import com.dalhousie.dalhousie_marketplace_backend.service.OrderService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private CategoryRepository categoryRepository;


    @Autowired
    private JwtUtil jwtUtil;

    private Long userId;
    private String token;

    @BeforeEach
    void setUp() {
        // Create User
        User user = new User();
        user.setEmail("user@example.com");
        user.setusername("testuser");
        user.setPasswordHash("hashed123");
        user.setbannerId("B00123456");
        user.setIsVerified(true);
        user = userRepository.saveAndFlush(user);

        userId = user.getUserId();
        token = "Bearer " + jwtUtil.generateToken(user);

        // Create Category
        Category category = new Category();
        category.setName("Sample Category"); // Example category
        category = categoryRepository.saveAndFlush(category);

        // Create Listing
        Listing listing = new Listing();
        listing.setSeller(user);
        listing.setTitle("Sample Item");
        listing.setPrice(BigDecimal.valueOf(50.00).doubleValue());
        listing.setQuantity(10);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing.setCategoryId(category.getId()); // Set the category
        listing = listingRepository.saveAndFlush(listing);

        // Create Cart and CartItem
        Cart cart = new Cart();
        cart.setUserId(user.getUserId());

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setListing(listing);
        cartItem.setQuantity(1);
        cartItem.setPrice(BigDecimal.valueOf(listing.getPrice()));

        cart.getCartItems().add(cartItem);
        cartRepository.saveAndFlush(cart);
    }



    @Test
    void testConvertCartToOrder_Success() throws Exception {
        mockMvc.perform(post("/api/orders/cart/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUserOrders_Forbidden() throws Exception {
        User anotherUser = new User();
        anotherUser.setEmail("other@example.com");
        anotherUser.setusername("otheruser");
        anotherUser.setPasswordHash("pass321");
        anotherUser.setbannerId("B00987654");
        anotherUser.setIsVerified(true);
        anotherUser = userRepository.save(anotherUser);

        String otherToken = "Bearer " + jwtUtil.generateToken(anotherUser);

        mockMvc.perform(get("/api/orders/user/" + userId)
                        .header("Authorization", otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserOrders_Success() throws Exception {
        mockMvc.perform(get("/api/orders/user/" + userId)
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Order not found for ID: 999999"));
    }
}
