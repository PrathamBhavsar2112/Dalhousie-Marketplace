package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.Notification;
import com.dalhousie.dalhousie_marketplace_backend.model.NotificationType;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.repository.NotificationRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserPreferencesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotification(User user, NotificationType type, String content) {
        Notification notification = buildNotification(user, type, content);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/queue/notifications/" + user.getUserId(), notification);
    }

    private Notification buildNotification(User user, NotificationType type, String content) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(content);
        notification.setReadStatus(false);
        notification.setTimestamp(new Date());
        return notification;
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUser_UserIdAndReadStatusFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadStatus(true);
            notificationRepository.save(notification);
        });
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        UserPreferences preferences = getUserPreferences(userId);
        List<Notification> notifications = getUnreadNotifications(userId);

        NotificationFilter filter = new NotificationFilter(
                preferences.isReceiveMessages(),
                preferences.isReceiveItems(),
                preferences.isReceiveBids(),
                preferences.getKeywords()
        );

        return notifications.stream()
                .filter(notification -> isNotificationRelevant(notification, filter))
                .collect(Collectors.toList());
    }

    private boolean isNotificationRelevant(Notification notif, NotificationFilter filter) {
        if (notif.getType() == null) return false;

        switch (notif.getType()) {
            case MESSAGE:
                return filter.receiveMessages;
            case ITEM:
                return isItemNotificationRelevant(notif.getMessage(), filter.receiveItems, filter.keywords);
            case BID:
                return filter.receiveBids;
            default:
                return false;
        }
    }

    private boolean isItemNotificationRelevant(String message, boolean receiveItems, List<String> keywords) {
        if (!receiveItems || message == null) return false;
        if (keywords == null || keywords.isEmpty()) return true;

        return keywords.stream()
                .anyMatch(keyword -> message.toLowerCase().contains(keyword.toLowerCase()));
    }

    private UserPreferences getUserPreferences(Long userId) {
        UserPreferences preferences = userPreferencesRepository.findByUser_UserId(userId);
        if (preferences == null) {
            preferences = new UserPreferences();
            preferences.setReceiveMessages(true);
            preferences.setReceiveItems(true);
            preferences.setReceiveBids(true);
            preferences.setKeywords(new ArrayList<>());
        }
        return preferences;
    }

    public void markNotificationAsRead(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty()) {
            throw new RuntimeException("Notification not found");
        }
        Notification notification = optionalNotification.get();
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    private static class NotificationFilter {
        boolean receiveMessages;
        boolean receiveItems;
        boolean receiveBids;
        List<String> keywords;

        NotificationFilter(boolean receiveMessages, boolean receiveItems, boolean receiveBids, List<String> keywords) {
            this.receiveMessages = receiveMessages;
            this.receiveItems = receiveItems;
            this.receiveBids = receiveBids;
            this.keywords = keywords;
        }
    }
}