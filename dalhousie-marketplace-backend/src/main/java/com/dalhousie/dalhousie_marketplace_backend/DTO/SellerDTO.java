package com.dalhousie.dalhousie_marketplace_backend.DTO;

public class SellerDTO {
    private Long userId;
    private String username;

    public SellerDTO(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
