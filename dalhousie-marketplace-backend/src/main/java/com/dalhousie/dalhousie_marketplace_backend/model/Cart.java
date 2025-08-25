package com.dalhousie.dalhousie_marketplace_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cart")
    private List<CartItem> cartItems = new ArrayList<>();

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public Cart() {}

    public Cart(Long userId) {
        this.userId = userId;
    }

    public void addCartItem(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
        totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    public void removeCartItem(CartItem item) {
        cartItems.remove(item);
        totalPrice = totalPrice.subtract(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    // Getters and Setters remain the same
    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void updateCartItemQuantity(CartItem item, int newQuantity) {
        if (cartItems.contains(item)) {
            // Update total price
            BigDecimal oldAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal newAmount = item.getPrice().multiply(BigDecimal.valueOf(newQuantity));
            totalPrice = totalPrice.subtract(oldAmount).add(newAmount);

            // Update item quantity
            item.setQuantity(newQuantity);
        }
    }

}