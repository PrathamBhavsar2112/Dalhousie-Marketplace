package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.BidRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.BidStatusUpdateRequest;
import com.dalhousie.dalhousie_marketplace_backend.model.Bid;
import com.dalhousie.dalhousie_marketplace_backend.model.BidStatus;
import com.dalhousie.dalhousie_marketplace_backend.service.BidPaymentService;
import com.dalhousie.dalhousie_marketplace_backend.service.BidService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling bid-related API endpoints.
 */
@RestController
@RequestMapping("/api/bids")
public class BidController {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidPaymentService bidPaymentService;

    @Autowired
    private JwtUtil jwtUtil;

    private ResponseEntity<?> validateAuthorization(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization token");
        }
        return null;
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }


    @PostMapping("/{listingId}")
    public ResponseEntity<?> createBid(
            @PathVariable Long listingId,
            @Valid @RequestBody BidRequest bidRequest,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);

            Bid bid = bidService.createBid(
                    listingId,
                    userId,
                    bidRequest.getProposedPrice(),
                    bidRequest.getAdditionalTerms()
            );

            return ResponseEntity.ok(bid);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating bid: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating bid: " + e.getMessage());
        }
    }


    @PutMapping("/{bidId}/status")
    public ResponseEntity<?> updateBidStatus(
            @PathVariable Long bidId,
            @Valid @RequestBody BidStatusUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);

            // Update bid status
            Bid updatedBid = bidService.updateBidStatus(
                    bidId,
                    userId,
                    request.getStatus()
            );

            return ResponseEntity.ok(updatedBid);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating bid status: " + e.getMessage());
        }
    }


    @GetMapping("/listing/{listingId}")
    public ResponseEntity<?> getBidsByListing(
            @PathVariable Long listingId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            List<Bid> bids = bidService.getBidsByListing(listingId);
            return ResponseEntity.ok(bids);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving bids: " + e.getMessage());
        }
    }


    @GetMapping("/user")
    public ResponseEntity<?> getUserBids(
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<Bid> bids = bidService.getBidsByUser(userId);
            return ResponseEntity.ok(bids);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user bids: " + e.getMessage());
        }
    }


    @GetMapping("/{bidId}")
    public ResponseEntity<?> getBidById(
            @PathVariable Long bidId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Bid not found with ID: " + bidId);
            }
            return ResponseEntity.ok(bid);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving bid: " + e.getMessage());
        }
    }


    @PostMapping("/{bidId}/counter")
    public ResponseEntity<?> counterBid(
            @PathVariable Long bidId,
            @Valid @RequestBody BidRequest counterRequest,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            Bid counterBid = createCounterBid(bidId, userId, counterRequest);

            return ResponseEntity.ok(counterBid);
        } catch (RuntimeException e) {
            return handleCounterBidError(e);
        }
    }

    private Bid createCounterBid(Long bidId, Long userId, BidRequest counterRequest) {
        return bidService.counterBid(
                bidId,
                userId,
                counterRequest.getProposedPrice(),
                counterRequest.getAdditionalTerms()
        );
    }

    private ResponseEntity<String> handleCounterBidError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating counter offer: " + e.getMessage());
    }


    @PostMapping("/listing/{listingId}/finalize")
    public ResponseEntity<?> finalizeBidding(
            @PathVariable Long listingId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            Bid winningBid = bidService.finalizeBidding(listingId, userId);
            return ResponseEntity.ok(winningBid);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error finalizing bidding: " + e.getMessage());
        }
    }


    @GetMapping("/listing/{listingId}/count")
    public ResponseEntity<?> getActiveBidCount(@PathVariable Long listingId) {
        try {
            int count = bidService.getActiveBidCount(listingId);
            return ResponseEntity.ok(count);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting bid count: " + e.getMessage());
        }
    }


    @PostMapping("/{bidId}/pay")
    public ResponseEntity<?> payForAcceptedBid(
            @PathVariable Long bidId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            String checkoutUrl = bidPaymentService.createBidCheckoutSession(bidId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", checkoutUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment: " + e.getMessage());
        }
    }


    @PostMapping("/{bidId}/reject")
    public ResponseEntity<?> rejectSingleBid(
            @PathVariable Long bidId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            Bid rejectedBid = bidService.updateBidStatus(
                    bidId,
                    userId,
                    BidStatus.REJECTED
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Bid successfully rejected",
                    "bid", rejectedBid
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error rejecting bid: " + e.getMessage());
        }
    }


    @PostMapping("/{bidId}/accept")
    public ResponseEntity<?> acceptSingleBid(
            @PathVariable Long bidId,
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authorizationError = validateAuthorization(authHeader);
        if (authorizationError != null) return authorizationError;

        try {
            Long userId = extractUserIdFromToken(authHeader);
            Bid acceptedBid = bidService.acceptSingleBid(
                    bidId,
                    userId
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Bid successfully accepted. All other bids for this listing were rejected.",
                    "bid", acceptedBid
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error accepting bid: " + e.getMessage());
        }
    }
}