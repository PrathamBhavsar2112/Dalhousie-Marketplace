package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class BiddingListingsController {

    @Autowired
    private ListingService listingService;

    @Autowired
    private JwtUtil jwtUtil;


    @GetMapping("/bidding/seller/{sellerId}")
    public ResponseEntity<?> getBiddingListingsBySeller(@PathVariable Long sellerId) {
        try {
            List<Listing> biddingListings = listingService.getBiddingListingsBySeller(sellerId);
            return ResponseEntity.ok(biddingListings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving bidding listings: " + e.getMessage());
        }
    }


    @GetMapping("/bidding/my-listings")
    public ResponseEntity<?> getMyBiddingListings(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate token and extract seller ID
            Long sellerId = extractSellerIdFromToken(authHeader);
            List<Listing> biddingListings = listingService.getBiddingListingsBySeller(sellerId);
            return ResponseEntity.ok(biddingListings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving bidding listings: " + e.getMessage());
        }
    }


    private Long extractSellerIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid authorization token");
        }

        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
