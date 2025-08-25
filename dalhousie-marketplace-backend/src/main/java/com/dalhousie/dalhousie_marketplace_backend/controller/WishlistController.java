package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Wishlist;
import com.dalhousie.dalhousie_marketplace_backend.service.WishlistService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final JwtUtil jwtUtil;

    public WishlistController(WishlistService wishlistService, JwtUtil jwtUtil) {
        this.wishlistService = wishlistService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getWishlist(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long tokenUserId = jwtUtil.extractUserId(token);

            if (!userId.equals(tokenUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to access this wishlist");
            }

            Wishlist wishlist = wishlistService.getWishlistByUserId(userId);
            return ResponseEntity.ok(wishlist);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving wishlist: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/add/{listingId}")
    public ResponseEntity<?> addItemToWishlist(
            @PathVariable Long userId,
            @PathVariable Long listingId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long tokenUserId = jwtUtil.extractUserId(token);

            if (!userId.equals(tokenUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to modify this wishlist");
            }

            Wishlist updatedWishlist = wishlistService.addItemToWishlist(userId, listingId);
            return ResponseEntity.ok(updatedWishlist);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Item already exists in wishlist");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding item to wishlist: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding item to wishlist: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/items/{listingId}")
    public ResponseEntity<?> removeWishlistItem(
            @PathVariable Long userId,
            @PathVariable Long listingId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long tokenUserId = jwtUtil.extractUserId(token);

            if (!userId.equals(tokenUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to modify this wishlist");
            }

            Wishlist updatedWishlist = wishlistService.removeWishlistItem(userId, listingId);
            return ResponseEntity.ok(updatedWishlist);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing item from wishlist: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearWishlist(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long tokenUserId = jwtUtil.extractUserId(token);

            if (!userId.equals(tokenUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to modify this wishlist");
            }

            Wishlist wishlist = wishlistService.getWishlistByUserId(userId);
            wishlistService.clearWishlist(wishlist.getWishlistId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error clearing wishlist: " + e.getMessage());
        }
    }
}