package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.Cart;
import com.dalhousie.dalhousie_marketplace_backend.model.CartItem;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.repository.CartRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ListingRepository listingRepository;

    public CartService(CartRepository cartRepository, ListingRepository listingRepository) {
        this.cartRepository = cartRepository;
        this.listingRepository = listingRepository;
    }

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart(userId);
            return cartRepository.save(newCart);
        });
    }

    public Cart addItemToCart(Long userId, Long listingId, int quantity) {
        Cart cart = getCartByUserId(userId);
        Optional<Listing> listingOpt = listingRepository.findById(listingId);

        if (listingOpt.isEmpty()) {
            throw new RuntimeException("Listing not found");
        }

        Listing listing = listingOpt.get();

        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new RuntimeException("Only active listings can be added to cart");
        }

        if (listing.getQuantity() < quantity) {
            throw new RuntimeException("Not enough quantity available. Available: " + listing.getQuantity());
        }

        CartItem cartItem = new CartItem(cart, listing, quantity, BigDecimal.valueOf(listing.getPrice()));
        cart.addCartItem(cartItem);

        return cartRepository.save(cart);
    }

    public void clearCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }

    public Cart updateCartItemQuantity(Long userId, Long listingId, int newQuantity) {
        Cart cart = getCartByUserId(userId);
        CartItem item = findCartItem(cart, listingId);

        updateTotalPriceForQuantityChange(cart, item, newQuantity);
        item.setQuantity(newQuantity);

        return cartRepository.save(cart);
    }

    private CartItem findCartItem(Cart cart, Long listingId) {
        return cart.getCartItems().stream()
                .filter(i -> i.getListing().getId().equals(listingId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
    }

    private void updateTotalPriceForQuantityChange(Cart cart, CartItem item, int newQuantity) {
        BigDecimal oldTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal newTotal = item.getPrice().multiply(BigDecimal.valueOf(newQuantity));

        cart.setTotalPrice(cart.getTotalPrice().subtract(oldTotal).add(newTotal));
    }

    public Cart removeCartItem(Long userId, Long listingId) {
        Cart cart = getCartByUserId(userId);
        CartItem item = findItemInCart(cart, listingId);

        updateCartTotal(cart, item);
        cart.getCartItems().remove(item);

        return cartRepository.save(cart);
    }

    private CartItem findItemInCart(Cart cart, Long listingId) {
        return cart.getCartItems().stream()
                .filter(i -> i.getListing().getId().equals(listingId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
    }

    private void updateCartTotal(Cart cart, CartItem item) {
        BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(itemTotal));
    }
}
