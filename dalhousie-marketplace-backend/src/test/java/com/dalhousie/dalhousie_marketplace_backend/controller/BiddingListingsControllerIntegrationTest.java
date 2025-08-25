package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.mockito.Mockito.*;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BiddingListingsControllerIntegrationTest.TestConfig.class)
public class BiddingListingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingService listingService;

    @Autowired
    private JwtUtil jwtUtil;

    private String authHeader;

    @BeforeEach
    public void setup() {
        authHeader = "Bearer valid-token";
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
    }

    @Test
    public void testGetBiddingListingsBySeller() throws Exception {
        Listing listing = new Listing();
        listing.setId(1L);

        when(listingService.getBiddingListingsBySeller(1L)).thenReturn(Arrays.asList(listing));

        mockMvc.perform(get("/api/listings/bidding/seller/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    public void testGetMyBiddingListings() throws Exception {
        Listing listing = new Listing();
        listing.setId(2L);

        when(listingService.getBiddingListingsBySeller(1L)).thenReturn(Arrays.asList(listing));

        mockMvc.perform(get("/api/listings/bidding/my-listings")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ListingService listingService() {
            return mock(ListingService.class);
        }

        @Bean
        public JwtUtil jwtUtil() {
            return mock(JwtUtil.class);
        }
    }
}
