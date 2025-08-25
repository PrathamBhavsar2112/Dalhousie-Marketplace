package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Wishlist;
import com.dalhousie.dalhousie_marketplace_backend.service.WishlistService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WishlistControllerTest {

    private MockMvc mockMvc;

    private WishlistService wishlistService;
    private JwtUtil jwtUtil;
    private WishlistController wishlistController;

    @BeforeEach
    void setup() {
        wishlistService = mock(WishlistService.class);
        jwtUtil = mock(JwtUtil.class);

        wishlistController = new WishlistController(wishlistService, jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(wishlistController).build();

        when(jwtUtil.extractUserId(anyString())).thenReturn(1L); // default for all tests
    }

    @Test
    void testGetWishlist() throws Exception {
        Long userId = 1L;
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);

        when(wishlistService.getWishlistByUserId(userId)).thenReturn(wishlist);

        mockMvc.perform(get("/api/wishlist/{userId}", userId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void testAddItemToWishlist() throws Exception {
        Long userId = 1L, listingId = 100L;
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);

        when(wishlistService.addItemToWishlist(userId, listingId)).thenReturn(wishlist);

        mockMvc.perform(post("/api/wishlist/{userId}/add/{listingId}", userId, listingId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void testRemoveItemFromWishlist() throws Exception {
        Long userId = 1L, listingId = 50L;
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);

        when(wishlistService.removeWishlistItem(userId, listingId)).thenReturn(wishlist);

        mockMvc.perform(delete("/api/wishlist/{userId}/items/{listingId}", userId, listingId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void testClearWishlist() throws Exception {
        Long userId = 1L;
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setWishlistId(999L);

        when(wishlistService.getWishlistByUserId(userId)).thenReturn(wishlist);
        doNothing().when(wishlistService).clearWishlist(anyLong());

        mockMvc.perform(delete("/api/wishlist/{userId}", userId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/wishlist/1")
                        .header("Authorization", "InvalidHeader")) // Pass header but not in "Bearer ..." format
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Missing or invalid authorization token"));
    }


    @Test
    void testForbidden() throws Exception {
        Long userId = 1L;
        when(jwtUtil.extractUserId(anyString())).thenReturn(2L); // mismatch

        mockMvc.perform(get("/api/wishlist/{userId}", userId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You are not authorized to access this wishlist"));
    }
}
