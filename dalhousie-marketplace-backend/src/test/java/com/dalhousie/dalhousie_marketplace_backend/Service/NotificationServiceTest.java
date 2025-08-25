package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.Notification;
import com.dalhousie.dalhousie_marketplace_backend.model.NotificationType;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.repository.NotificationRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserPreferencesRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;
    private UserPreferences userPreferences;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("testuser@example.com");

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setMessage("New item posted!");
        testNotification.setReadStatus(false);
        testNotification.setTimestamp(new Date());
        testNotification.setType(NotificationType.ITEM);

        userPreferences = new UserPreferences();
        userPreferences.setUser(testUser);
        userPreferences.setReceiveItems(true);
        userPreferences.setReceiveMessages(false);
        userPreferences.setReceiveBids(true);
    }

    // Test sendNotification()
    @Test
    void sendNotification_SavesNotification() {
        notificationService.sendNotification(testUser, NotificationType.ITEM, "New item added");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendNotification_SendsWebSocketMessage() {
        notificationService.sendNotification(testUser, NotificationType.ITEM, "New item added");

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/queue/notifications/" + testUser.getUserId()), any(Notification.class));
    }


    // Test getUnreadNotifications()
    @Test
    void getUnreadNotifications_ReturnsCorrectSize() {
        when(notificationRepository.findByUser_UserIdAndReadStatusFalse(testUser.getUserId()))
                .thenReturn(Arrays.asList(testNotification));

        List<Notification> result = notificationService.getUnreadNotifications(testUser.getUserId());

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadNotifications_CallsRepository() {
        when(notificationRepository.findByUser_UserIdAndReadStatusFalse(testUser.getUserId()))
                .thenReturn(Arrays.asList(testNotification));

        notificationService.getUnreadNotifications(testUser.getUserId());

        verify(notificationRepository, times(1)).findByUser_UserIdAndReadStatusFalse(testUser.getUserId());
    }

    // Test markAsRead()
    @Test
    void markAsRead_MarksNotificationRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(1L);

        assertTrue(testNotification.getReadStatus());
    }

    @Test
    void markAsRead_SavesChanges() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(1L);

        verify(notificationRepository, times(1)).save(testNotification);
    }

    // Test getNotificationsForUser()
    @Test
    void getNotificationsForUser_ReturnsCorrectSize() {
        when(userPreferencesRepository.findByUser_UserId(testUser.getUserId())).thenReturn(userPreferences);
        when(notificationRepository.findByUser_UserIdAndReadStatusFalse(testUser.getUserId()))
                .thenReturn(Arrays.asList(testNotification));

        List<Notification> result = notificationService.getNotificationsForUser(testUser.getUserId());

        assertEquals(1, result.size());
    }

    @Test
    void getNotificationsForUser_CallsNotificationRepository() {
        when(userPreferencesRepository.findByUser_UserId(testUser.getUserId())).thenReturn(userPreferences);
        when(notificationRepository.findByUser_UserIdAndReadStatusFalse(testUser.getUserId()))
                .thenReturn(Arrays.asList(testNotification));

        notificationService.getNotificationsForUser(testUser.getUserId());

        verify(notificationRepository, times(1)).findByUser_UserIdAndReadStatusFalse(testUser.getUserId());
    }

    // Test markNotificationAsRead() - Success
    @Test
    void markNotificationAsRead_Success_MarksRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markNotificationAsRead(1L);

        assertTrue(testNotification.getReadStatus());
    }

    @Test
    void markNotificationAsRead_Success_SavesNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markNotificationAsRead(1L);

        verify(notificationRepository, times(1)).save(testNotification);
    }

    // Test markNotificationAsRead() - Not Found
    @Test
    void markNotificationAsRead_NotFound_ThrowsException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markNotificationAsRead(99L);
        });

        assertEquals("Notification not found", exception.getMessage());
    }


}
