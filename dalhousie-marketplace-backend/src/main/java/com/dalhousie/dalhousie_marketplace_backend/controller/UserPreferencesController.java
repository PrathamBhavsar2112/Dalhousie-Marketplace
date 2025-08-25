package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.service.UserPreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferencesController {
    @Autowired
    private UserPreferencesService userPreferencesService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserPreferences> getUserPreferences(@PathVariable Long userId) {
        UserPreferences preferences = userPreferencesService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> updatePreferences(
            @PathVariable Long userId,
            @RequestBody UserPreferences preferences
    ) {
        userPreferencesService.updatePreferences(userId, preferences);
        return ResponseEntity.ok("Preferences updated successfully");
    }

    @GetMapping("/{userId}/keywords")
    public ResponseEntity<List<String>> getKeywords(@PathVariable Long userId) {
        UserPreferences preferences = userPreferencesService.getUserPreferences(userId);
        List<String> keywords = (preferences != null && preferences.getKeywords() != null)
                ? preferences.getKeywords()
                : new ArrayList<>();
        return ResponseEntity.ok(keywords);
    }


    @PostMapping("/{userId}/keywords")
    public ResponseEntity<String> addKeyword(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        String keyword = request.get("keyword");
        userPreferencesService.addKeyword(userId, keyword);
        return ResponseEntity.ok("Keyword added");
    }


    @DeleteMapping("/{userId}/keywords/{keyword}")
    public ResponseEntity<String> deleteKeyword(@PathVariable Long userId, @PathVariable String keyword) {
        userPreferencesService.removeKeyword(userId, keyword);
        return ResponseEntity.ok("Keyword removed");
    }
}

