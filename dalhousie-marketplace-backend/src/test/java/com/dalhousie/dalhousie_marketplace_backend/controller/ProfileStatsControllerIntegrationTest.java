package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.service.ProfileStatsService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProfileStatsControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private ProfileStatsService profileStatsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private ProfileStatsController profileStatsController;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(profileStatsController).build();

        // Generate a valid JWT token (replace with your method of generating tokens)
        validJwtToken = "valid.jwt.token.here"; // Use a valid token here
    }

    @Test
    void testGetProfileStats_Success() throws Exception {
        // Mock the service layer to return dummy data
        when(profileStatsService.getProfileStats(anyLong())).thenReturn(Map.of("profile", "stats"));

        // Simulate the API call with a valid JWT token
        mockMvc.perform(get("/api/profile/stats")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("stats"));

        // Verify that the service was called with the correct user ID
        verify(profileStatsService, times(1)).getProfileStats(anyLong());
    }



    @Test
    void testGetProfileStats_InternalServerError() throws Exception {
        // Mock an exception in the service layer
        when(profileStatsService.getProfileStats(anyLong())).thenThrow(new RuntimeException("Service error"));

        // Simulate the API call with a valid JWT token
        mockMvc.perform(get("/api/profile/stats")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Error retrieving profile statistics: Service error"));
    }



    @Test
    void testGetUserProfileStats_Forbidden() throws Exception {
        // Simulate a case where the requesting user does not have permission
        when(profileStatsService.getProfileStats(anyLong())).thenReturn(Map.of("profile", "stats"));

        // Simulate the API call with a valid JWT token but trying to access another user's data
        mockMvc.perform(get("/api/profile/stats/{userId}", 2L)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$").value("You don't have permission to view this user's statistics"));
    }

    @Test
    void testGetBuyerStats_Success() throws Exception {
        // Mock the service layer to return dummy data
        when(profileStatsService.getBuyerStats(anyLong())).thenReturn(Map.of("buyer", "stats"));

        // Simulate the API call with a valid JWT token
        mockMvc.perform(get("/api/profile/stats/buyer")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyer").value("stats"));

        // Verify that the service was called with the correct user ID
        verify(profileStatsService, times(1)).getBuyerStats(anyLong());
    }

    @Test
    void testGetSellerStats_Success() throws Exception {
        // Mock the service layer to return dummy data
        when(profileStatsService.getSellerStats(anyLong())).thenReturn(Map.of("seller", "stats"));

        // Simulate the API call with a valid JWT token
        mockMvc.perform(get("/api/profile/stats/seller")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seller").value("stats"));

        // Verify that the service was called with the correct user ID
        verify(profileStatsService, times(1)).getSellerStats(anyLong());
    }
}
