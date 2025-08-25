package com.dalhousie.dalhousie_marketplace_backend.DTO;

import java.util.Date;

public class ListingDTO {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private Integer quantity;
    private Long categoryId;
    private String categoryName;
    private Date purchaseDate;
    private Date createdAt;
    private SellerDTO seller;
    private Boolean biddingAllowed;
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public SellerDTO getSeller() { return seller; }
    public void setSeller(SellerDTO seller) { this.seller = seller; }

    public boolean getbiddingAllowed() { return biddingAllowed; }
    public void setBiddingAllowed(boolean biddingAllowed) { this.biddingAllowed = biddingAllowed; }
}
