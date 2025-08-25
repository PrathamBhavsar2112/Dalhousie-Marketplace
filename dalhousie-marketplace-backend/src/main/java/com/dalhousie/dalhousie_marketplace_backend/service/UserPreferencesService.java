package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserPreferencesRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserPreferencesService {

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieves user preferences by user ID.
     */
    public UserPreferences getUserPreferences(Long userId) {
        return userPreferencesRepository.findByUser_UserId(userId);
    }

    /**
     * Updates preferences for a user.
     */
    public void updatePreferences(Long userId, UserPreferences newPreferences) {
        User user = getUserById(userId);

        UserPreferences preferences = userPreferencesRepository.findByUser_UserId(userId);
        if (preferences == null) {
            preferences = new UserPreferences(user, true, true, true, new ArrayList<>());
        }

        preferences.setReceiveMessages(newPreferences.isReceiveMessages());
        preferences.setReceiveItems(newPreferences.isReceiveItems());
        preferences.setReceiveBids(newPreferences.isReceiveBids());

        if (newPreferences.getKeywords() != null) {
            preferences.setKeywords(newPreferences.getKeywords());
        }

        userPreferencesRepository.save(preferences);
    }

    /**
     * Adds a keyword to user preferences.
     */
    public void addKeyword(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;

        UserPreferences preferences = userPreferencesRepository.findByUser_UserId(userId);
        if (preferences != null) {
            preferences.getKeywords().add(keyword.trim());
            userPreferencesRepository.save(preferences);
        }
    }

    /**
     * Removes a keyword from user preferences.
     */
    public void removeKeyword(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;

        UserPreferences preferences = userPreferencesRepository.findByUser_UserId(userId);
        if (preferences != null) {
            preferences.getKeywords().removeIf(k -> k.equalsIgnoreCase(keyword));
            userPreferencesRepository.save(preferences);
        }
    }

    /**
     * Helper method to fetch a user by ID or throw exception.
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
