package com.dalhousie.dalhousie_marketplace_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Entity class for bids placed on listings.
 * Represents offers made by buyers for specific listings.
 */
@Entity
@Table(name = "bids")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "email", "passwordHash", "accountStatus", "isVerified", "lastLogin", "verificationStatus", "createdAt", "updatedAt", "otp", "otpExpiry"})
    private User buyer;

    @Column(name = "proposed_price", nullable = false)
    private Double proposedPrice;

    @Column(name = "additional_terms")
    private String additionalTerms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BidStatus status = BidStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "order_id")
    private Long orderId;

    /**
     * Default constructor
     */
    public Bid() {
    }

    /**
     * Constructor with all fields
     */
    public Bid(Long id, Listing listing, User buyer, Double proposedPrice, String additionalTerms,
               BidStatus status, Date createdAt, Date updatedAt) {
        this.id = id;
        this.listing = listing;
        this.buyer = buyer;
        this.proposedPrice = proposedPrice;
        this.additionalTerms = additionalTerms;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Lifecycle callback to set timestamps before entity is persisted
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Lifecycle callback to update timestamp when entity is updated
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    // Getters and Setters

    /**
     * Get the ID of this bid
     * @return The bid ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the ID of this bid
     * @param id The bid ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the listing this bid is for
     * @return The listing
     */
    public Listing getListing() {
        return listing;
    }

    /**
     * Set the listing this bid is for
     * @param listing The listing
     */
    public void setListing(Listing listing) {
        this.listing = listing;
    }

    /**
     * Get the buyer who made this bid
     * @return The buyer
     */
    public User getBuyer() {
        return buyer;
    }

    /**
     * Set the buyer who made this bid
     * @param buyer The buyer
     */
    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    /**
     * Get the proposed price of this bid
     * @return The proposed price
     */
    public Double getProposedPrice() {
        return proposedPrice;
    }

    /**
     * Set the proposed price of this bid
     * @param proposedPrice The proposed price
     */
    public void setProposedPrice(Double proposedPrice) {
        this.proposedPrice = proposedPrice;
    }

    /**
     * Get any additional terms for this bid
     * @return The additional terms
     */
    public String getAdditionalTerms() {
        return additionalTerms;
    }

    /**
     * Set additional terms for this bid
     * @param additionalTerms The additional terms
     */
    public void setAdditionalTerms(String additionalTerms) {
        this.additionalTerms = additionalTerms;
    }

    /**
     * Get the current status of this bid
     * @return The bid status
     */
    public BidStatus getStatus() {
        return status;
    }

    /**
     * Set the status of this bid
     * @param status The bid status
     */
    public void setStatus(BidStatus status) {
        this.status = status;
    }

    /**
     * Get the creation timestamp of this bid
     * @return The creation timestamp
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the creation timestamp of this bid
     * @param createdAt The creation timestamp
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the last update timestamp of this bid
     * @return The last update timestamp
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last update timestamp of this bid
     * @param updatedAt The last update timestamp
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bid bid = (Bid) o;

        return id != null ? id.equals(bid.id) : bid.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "id=" + id +
                ", listingId=" + (listing != null ? listing.getId() : null) +
                ", buyerId=" + (buyer != null ? buyer.getUserId() : null) +
                ", proposedPrice=" + proposedPrice +
                ", status=" + status +
                '}';
    }


    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}