package com.dalhousie.dalhousie_marketplace_backend.model;

/**
 * Enumeration representing the possible states of a bid.
 */
public enum BidStatus {
    PENDING,    // Bid is awaiting response from seller
    ACCEPTED,   // Bid has been accepted by the seller
    REJECTED,   // Bid has been rejected by the seller
    COUNTERED,  // Seller has countered with a different offer
    EXPIRED,     // Bid has expired
    PAID
    }