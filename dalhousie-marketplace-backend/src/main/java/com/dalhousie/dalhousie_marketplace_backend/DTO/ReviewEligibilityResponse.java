package com.dalhousie.dalhousie_marketplace_backend.DTO;

import java.util.List;

public class ReviewEligibilityResponse {

    private List<ReviewableItemDTO> eligibleItems;

    public ReviewEligibilityResponse(List<ReviewableItemDTO> eligibleItems) {
        this.eligibleItems = eligibleItems;
    }

    public List<ReviewableItemDTO> getEligibleItems() {
        return eligibleItems;
    }

    public void setEligibleItems(List<ReviewableItemDTO> eligibleItems) {
        this.eligibleItems = eligibleItems;
    }

    public static class ReviewableItemDTO {
        private Long orderItemId;
        private Long listingId;
        private String listingTitle;
        private String listingImage;  // URL to the primary image if available
        private Double price;
        private String purchaseDate;

        public ReviewableItemDTO(Long orderItemId, Long listingId, String listingTitle,
                                 String listingImage, Double price, String purchaseDate) {
            this.orderItemId = orderItemId;
            this.listingId = listingId;
            this.listingTitle = listingTitle;
            this.listingImage = listingImage;
            this.price = price;
            this.purchaseDate = purchaseDate;
        }

        // Getters and setters
        public Long getOrderItemId() {
            return orderItemId;
        }

        public void setOrderItemId(Long orderItemId) {
            this.orderItemId = orderItemId;
        }

        public Long getListingId() {
            return listingId;
        }

        public void setListingId(Long listingId) {
            this.listingId = listingId;
        }

        public String getListingTitle() {
            return listingTitle;
        }

        public void setListingTitle(String listingTitle) {
            this.listingTitle = listingTitle;
        }

        public String getListingImage() {
            return listingImage;
        }

        public void setListingImage(String listingImage) {
            this.listingImage = listingImage;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getPurchaseDate() {
            return purchaseDate;
        }

        public void setPurchaseDate(String purchaseDate) {
            this.purchaseDate = purchaseDate;
        }
    }
}