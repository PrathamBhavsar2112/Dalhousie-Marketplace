package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.service.ProfileStatsService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for user profile statistics.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileStatsController {

    @Autowired
    private ProfileStatsService profileStatsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get comprehensive profile statistics for the authenticated user.
     *
     * @param authHeader Authorization header with JWT token
     * @return Response with all profile statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getProfileStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authorization
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            Map<String, Object> stats = profileStatsService.getProfileStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving profile statistics: " + e.getMessage());
        }
    }

    /**
     * Get profile statistics for a specific user (admin access).
     *
     * @param userId The ID of the user to get statistics for
     * @param authHeader Authorization header with JWT token
     * @return Response with all profile statistics
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getUserProfileStats(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authorization
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long requestingUserId = jwtUtil.extractUserId(token);

            // Check if the user is requesting their own stats or if they have admin privileges
            // TODO: Add proper admin check based on your authentication system
            boolean isAdmin = false; // This should be determined by your auth logic

            if (!userId.equals(requestingUserId) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to view this user's statistics");
            }

            Map<String, Object> stats = profileStatsService.getProfileStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving profile statistics: " + e.getMessage());
        }
    }

    /**
     * Get only buyer statistics for the authenticated user.
     *
     * @param authHeader Authorization header with JWT token
     * @return Response with buyer statistics
     */
    @GetMapping("/stats/buyer")
    public ResponseEntity<?> getBuyerStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authorization
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            Map<String, Object> buyerStats = profileStatsService.getBuyerStats(userId);
            return ResponseEntity.ok(buyerStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving buyer statistics: " + e.getMessage());
        }
    }

    /**
     * Get only seller statistics for the authenticated user.
     *
     * @param authHeader Authorization header with JWT token
     * @return Response with seller statistics
     */
    @GetMapping("/stats/seller")
    public ResponseEntity<?> getSellerStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authorization
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid authorization token");
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            Map<String, Object> sellerStats = profileStatsService.getSellerStats(userId);
            return ResponseEntity.ok(sellerStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving seller statistics: " + e.getMessage());
        }
    }
}