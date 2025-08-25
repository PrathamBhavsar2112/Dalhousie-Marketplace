package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.UserPreferences;
import com.dalhousie.dalhousie_marketplace_backend.service.UserPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserPreferencesControllerTest {

    @Mock
    private UserPreferencesService userPreferencesService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Initialize Mockito mocks
        MockitoAnnotations.openMocks(this);

        // Manually inject the mock into the controller using reflection
        UserPreferencesController controller = new UserPreferencesController();
        Field field = UserPreferencesController.class.getDeclaredField("userPreferencesService");
        field.setAccessible(true);  // Allow access to private field
        field.set(controller, userPreferencesService);  // Inject the mock service

        // Set up MockMvc with the controller and the mock service
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void testGetUserPreferences() throws Exception {
        Long userId = 1L;
        UserPreferences preferences = new UserPreferences();
        preferences.setId(userId);  // Assuming the field is 'id' not 'userId'
        preferences.setKeywords(Arrays.asList("item1", "item2"));

        // Mock the service response
        when(userPreferencesService.getUserPreferences(userId)).thenReturn(preferences);

        mockMvc.perform(get("/api/user/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))  // Change from $.userId to $.id
                .andExpect(jsonPath("$.keywords[0]").value("item1"))
                .andExpect(jsonPath("$.keywords[1]").value("item2"));

        verify(userPreferencesService, times(1)).getUserPreferences(userId);
    }


    @Test
    void testUpdatePreferences() throws Exception {
        Long userId = 1L;
        UserPreferences preferences = new UserPreferences();
        preferences.setId(userId);
        preferences.setKeywords(Arrays.asList("item3", "item4"));

        // Perform POST request to update preferences
        mockMvc.perform(post("/api/user/preferences/{userId}", userId)
                        .contentType("application/json")
                        .content("{\"id\":1, \"keywords\": [\"item3\", \"item4\"]}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Preferences updated successfully"));

        // Use ArgumentCaptor to capture the argument passed to updatePreferences
        ArgumentCaptor<UserPreferences> argumentCaptor = ArgumentCaptor.forClass(UserPreferences.class);
        verify(userPreferencesService, times(1)).updatePreferences(eq(userId), argumentCaptor.capture());

        // Verify that the captured argument matches the expected object
        UserPreferences capturedPreferences = argumentCaptor.getValue();
        assert capturedPreferences != null;
        assert capturedPreferences.getId().equals(userId);
        assert capturedPreferences.getKeywords().containsAll(Arrays.asList("item3", "item4"));
    }

    @Test
    void testGetKeywords() throws Exception {
        Long userId = 1L;
        List<String> keywords = Arrays.asList("item1", "item2");

        UserPreferences preferences = new UserPreferences();
        preferences.setId(userId);
        preferences.setKeywords(keywords);

        // Mock the service response
        when(userPreferencesService.getUserPreferences(userId)).thenReturn(preferences);

        mockMvc.perform(get("/api/user/preferences/{userId}/keywords", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("item1"))
                .andExpect(jsonPath("$[1]").value("item2"));

        verify(userPreferencesService, times(1)).getUserPreferences(userId);
    }

    @Test
    void testAddKeyword() throws Exception {
        Long userId = 1L;
        String keyword = "item5";

        // Perform POST request to add keyword
        mockMvc.perform(post("/api/user/preferences/{userId}/keywords", userId)
                        .contentType("application/json")
                        .content("{\"keyword\": \"item5\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Keyword added"));

        // Verify that the service method was called
        verify(userPreferencesService, times(1)).addKeyword(userId, keyword);
    }

    @Test
    void testDeleteKeyword() throws Exception {
        Long userId = 1L;
        String keyword = "item3";

        // Perform DELETE request to remove keyword
        mockMvc.perform(delete("/api/user/preferences/{userId}/keywords/{keyword}", userId, keyword))
                .andExpect(status().isOk())
                .andExpect(content().string("Keyword removed"));

        // Verify that the service method was called
        verify(userPreferencesService, times(1)).removeKeyword(userId, keyword);
    }
}
