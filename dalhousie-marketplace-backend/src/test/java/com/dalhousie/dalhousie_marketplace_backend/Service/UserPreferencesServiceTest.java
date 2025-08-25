package com.dalhousie.dalhousie_marketplace_backend.Service;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserPreferencesRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.UserPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserPreferencesServiceTest {
    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPreferencesService userPreferencesService;

    private User user;
    private UserPreferences existingPreferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Sample user and preferences
        user = new User();
        user.setUserId(1L);

        existingPreferences = new UserPreferences();
        existingPreferences.setUser(user);
        existingPreferences.setReceiveMessages(true);
        existingPreferences.setReceiveItems(false);
        existingPreferences.setReceiveBids(true);
        existingPreferences.setKeywords(new ArrayList<>(Arrays.asList("electronics", "books")));
    }

    // getUserPreferences Tests
    @Test
    void getUserPreferences_ReturnsPreferencesWhenFound() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        UserPreferences result = userPreferencesService.getUserPreferences(1L);

        assertNotNull(result);
    }

    @Test
    void getUserPreferences_ReturnsNullWhenNotFound() {
        when(userPreferencesRepository.findByUser_UserId(2L)).thenReturn(null);

        UserPreferences result = userPreferencesService.getUserPreferences(2L);

        assertNull(result);
    }

    // updatePreferences Tests
    @Test
    void updatePreferences_UpdatesReceiveMessages() {
        UserPreferences newPreferences = new UserPreferences(user, false, true, false, new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.updatePreferences(1L, newPreferences);

        assertFalse(existingPreferences.isReceiveMessages());
    }

    @Test
    void updatePreferences_UpdatesReceiveItems() {
        UserPreferences newPreferences = new UserPreferences(user, false, true, false, new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.updatePreferences(1L, newPreferences);

        assertTrue(existingPreferences.isReceiveItems());
    }

    @Test
    void updatePreferences_UpdatesReceiveBids() {
        UserPreferences newPreferences = new UserPreferences(user, false, true, false, new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.updatePreferences(1L, newPreferences);

        assertFalse(existingPreferences.isReceiveBids());
    }

    @Test
    void updatePreferences_CreatesDefaultWhenNotFound() {
        UserPreferences newPreferences = new UserPreferences(user, false, false, true, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(null);

        userPreferencesService.updatePreferences(1L, newPreferences);

        verify(userPreferencesRepository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_ThrowsExceptionWhenUserNotFound() {
        UserPreferences newPreferences = new UserPreferences(user, false, true, false, new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userPreferencesService.updatePreferences(1L, newPreferences);
        });

        assertEquals("User not found", exception.getMessage());
    }

//    @Test
//    void updatePreferences_KeepsExistingKeywordsWhenNewKeywordsNull() {
//        // Mock existing preferences already stored in DB
//        UserPreferences existingPreferences = new UserPreferences();
//        existingPreferences.setUser(user);
//        existingPreferences.setReceiveMessages(true);
//        existingPreferences.setReceiveItems(false);
//        existingPreferences.setReceiveBids(true);
//        existingPreferences.setKeywords(new ArrayList<>(Arrays.asList("electronics", "books")));
//
//        // Simulate incoming update with null keywords
//        UserPreferences newPreferences = new UserPreferences(user, false, true, false, null);
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);
//
//        userPreferencesService.updatePreferences(1L, newPreferences);
//
//        // ✅ Test that old keywords are preserved
//        assertEquals(Arrays.asList("electronics", "books"), existingPreferences.getKeywords());
//    }


    @Test
    void updatePreferences_UpdatesKeywordsWhenProvided() {
        UserPreferences newPreferences = new UserPreferences(user, false, true, false,
                new ArrayList<>(Arrays.asList("clothes")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.updatePreferences(1L, newPreferences);

        assertEquals(Arrays.asList("clothes"), existingPreferences.getKeywords());
    }

    // addKeyword Tests
    @Test
    void addKeyword_AddsKeywordWhenPreferencesExist() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.addKeyword(1L, "games");

        assertTrue(existingPreferences.getKeywords().contains("games"));
    }

    @Test
    void addKeyword_DoesNotAddNullKeyword() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.addKeyword(1L, null);

        assertEquals(2, existingPreferences.getKeywords().size());
    }

    @Test
    void addKeyword_DoesNotAddEmptyKeyword() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.addKeyword(1L, "   ");

        assertEquals(2, existingPreferences.getKeywords().size());
    }

    @Test
    void addKeyword_DoesNothingWhenPreferencesNotFound() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(null);

        userPreferencesService.addKeyword(1L, "games");

        verify(userPreferencesRepository, never()).save(any());
    }

    // removeKeyword Tests
    @Test
    void removeKeyword_RemovesKeywordCaseInsensitive() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.removeKeyword(1L, "BOOKS");

        assertFalse(existingPreferences.getKeywords().contains("books"));
    }

    @Test
    void removeKeyword_DoesNothingWhenKeywordNotFound() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(existingPreferences);

        userPreferencesService.removeKeyword(1L, "games");

        assertEquals(2, existingPreferences.getKeywords().size());
    }

    @Test
    void removeKeyword_DoesNothingWhenPreferencesNotFound() {
        when(userPreferencesRepository.findByUser_UserId(1L)).thenReturn(null);

        userPreferencesService.removeKeyword(1L, "electronics");

        verify(userPreferencesRepository, never()).save(any());
    }
}
