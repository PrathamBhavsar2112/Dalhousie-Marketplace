package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.MessageRequest;
import com.dalhousie.dalhousie_marketplace_backend.model.Message;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.MessageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.Category;
import com.dalhousie.dalhousie_marketplace_backend.repository.CategoryRepository;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MessageControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private User sender;
    private Long senderId;
    private Long receiverId;
    private Long listingId;
    private String jwtToken;

    @BeforeEach
    void setup() {
        sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPasswordHash("pass");
        sender.setusername("Sender");
        sender.setAccountStatus("ACTIVE");
        sender.setIsVerified(true);
        sender.setbannerId("B01010101");
        sender = userRepository.save(sender);
        senderId = sender.getUserId();

        User receiver = new User();
        receiver.setEmail("receiver@example.com");
        receiver.setPasswordHash("pass");
        receiver.setusername("Receiver");
        receiver.setAccountStatus("ACTIVE");
        receiver.setIsVerified(true);
        receiver.setbannerId("B01020202");
        receiver = userRepository.save(receiver);
        receiverId = receiver.getUserId();

        Category category = new Category();
        category.setName("ChatTest");
        category = categoryRepository.save(category);

        Listing listing = new Listing();
        listing.setTitle("Chat Listing");
        listing.setDescription("For chat testing");
        listing.setPrice(100.0);
        listing.setQuantity(1);
        listing.setSeller(sender);
        listing.setCategoryId(category.getId());
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());
        listing = listingRepository.save(listing);
        jwtToken = "Bearer " + jwtUtil.generateToken(sender);
        listingId = listing.getId();
    }

    @Test
    public void testSendMessage_Success() throws Exception {
        MessageRequest request = new MessageRequest();
        request.setContent("Hello from sender!");
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setListingId(listingId);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello from sender!"));

    }

    @Test
    void testSendMessage_SameSenderAndReceiver_ShouldReturnBadRequest() throws Exception {
        MessageRequest request = new MessageRequest();
        request.setSenderId(senderId);
        request.setReceiverId(senderId);  // same as sender
        request.setListingId(listingId);
        request.setContent("This should fail");

        mockMvc.perform(post("/api/messages/send")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateToken(sender))  // ✅ Add auth
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You cannot send messages to yourself"));

    }
}
