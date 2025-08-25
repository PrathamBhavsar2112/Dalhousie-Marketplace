package com.dalhousie.dalhousie_marketplace_backend.DTO;

public class MessageRequest {
    private String content;
    private Long senderId;
    private Long receiverId; // ✅ Added receiverId
    private Long listingId;

    // Default Constructor
    public MessageRequest() {}

    // Parameterized Constructor
    public MessageRequest(String content, Long senderId, Long receiverId, Long listingId) {
        this.content = content;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.listingId = listingId;
    }

    // Getters
    public String getContent() {
        return content;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public Long getListingId() {
        return listingId;
    }

    // Setters
    public void setContent(String content) {
        this.content = content;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }
}
