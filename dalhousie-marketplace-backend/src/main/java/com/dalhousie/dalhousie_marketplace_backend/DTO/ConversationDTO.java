package com.dalhousie.dalhousie_marketplace_backend.DTO;

public class ConversationDTO {
    private String conversationId;
    private Long buyerId;
    private String buyerName;
    private String lastMessage;
    private String timestamp;
    private Long listingId;
    private String listingTitle;

    // Default no-args constructor
    public ConversationDTO() {
    }

    // All-args constructor
    public ConversationDTO(String conversationId, Long buyerId, String buyerName,
                           String lastMessage, String timestamp, Long listingId,
                           String listingTitle) {
        this.conversationId = conversationId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
    }

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
}