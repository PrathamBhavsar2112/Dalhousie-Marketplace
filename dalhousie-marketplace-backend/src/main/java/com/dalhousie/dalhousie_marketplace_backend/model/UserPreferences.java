package com.dalhousie.dalhousie_marketplace_backend.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ElementCollection
    @CollectionTable(name = "user_keywords", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    private boolean receiveMessages = true;
    private boolean receiveItems = true;
    private boolean receiveBids = true;
    public UserPreferences() {}

    // Parameterized Constructor
    public UserPreferences(User user, boolean receiveMessages, boolean receiveItems, boolean receiveBids, List<String> keywords) {
        this.user = user;
        this.receiveMessages = receiveMessages;
        this.receiveItems = receiveItems;
        this.receiveBids = receiveBids;
        this.keywords = keywords != null ? keywords : new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isReceiveMessages() {
        return receiveMessages;
    }

    public void setReceiveMessages(boolean receiveMessages) {
        this.receiveMessages = receiveMessages;
    }

    public boolean isReceiveItems() {
        return receiveItems;
    }

    public void setReceiveItems(boolean receiveItems) {
        this.receiveItems = receiveItems;
    }

    public boolean isReceiveBids() {
        return receiveBids;
    }

    public void setReceiveBids(boolean receiveBids) {
        this.receiveBids = receiveBids;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

}
