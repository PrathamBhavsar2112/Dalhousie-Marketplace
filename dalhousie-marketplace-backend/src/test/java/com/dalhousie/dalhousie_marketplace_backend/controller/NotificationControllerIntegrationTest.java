package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Notification;
import com.dalhousie.dalhousie_marketplace_backend.model.NotificationType;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.MessageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.NotificationRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MessageRepository messageRepository;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();         // Delete children
        notificationRepository.deleteAll();    // Delete children
        userRepository.deleteAll();            // Now safe to delete

        user = new User();
        user.setEmail("notify@example.com");
        user.setusername("notification-user");
        user.setPasswordHash("Test@1234");
        user.setbannerId("B00123456");
        user.setIsVerified(true);
        user = userRepository.save(user);

        notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Test Notification");
        notification.setTimestamp(new Date());
        notification.setReadStatus(false);
        notification.setType(NotificationType.MESSAGE);
        notification = notificationRepository.save(notification);
    }

    @Test
    void testGetUserNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/" + user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message", is("Test Notification")))
                .andExpect(jsonPath("$[0].readStatus", is(false)));
    }

    @Test
    void testMarkNotificationAsRead() throws Exception {
        mockMvc.perform(put("/api/notifications/" + notification.getId() + "/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification marked as read"));

        Notification updated = notificationRepository.findById(notification.getId()).orElse(null);
        assert updated != null;
        assert updated.getReadStatus();
    }
}
