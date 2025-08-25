package com.dalhousie.dalhousie_marketplace_backend.DTO;

import com.dalhousie.dalhousie_marketplace_backend.model.BidStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for bid status update requests.
 */
public class BidStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private BidStatus status;

    // Getters and Setters
    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }
}