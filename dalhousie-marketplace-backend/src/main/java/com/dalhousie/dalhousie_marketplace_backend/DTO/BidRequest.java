package com.dalhousie.dalhousie_marketplace_backend.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object for bid creation requests.
 */
public class BidRequest {

    @NotNull(message = "Proposed price is required")
    @Positive(message = "Proposed price must be positive")
    private Double proposedPrice;

    private String additionalTerms;

    // Getters and Setters
    public Double getProposedPrice() {
        return proposedPrice;
    }

    public void setProposedPrice(Double proposedPrice) {
        this.proposedPrice = proposedPrice;
    }

    public String getAdditionalTerms() {
        return additionalTerms;
    }

    public void setAdditionalTerms(String additionalTerms) {
        this.additionalTerms = additionalTerms;
    }
}