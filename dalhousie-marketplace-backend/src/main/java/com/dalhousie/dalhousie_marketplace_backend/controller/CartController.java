package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Cart;
import com.dalhousie.dalhousie_marketplace_backend.service.CartService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    public CartController(CartService cartService, JwtUtil jwtUtil) {
        this.cartService = cartService;
        this.jwtUtil = jwtUtil;
    }


    private Optional<Long> extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);
                return Optional.of(userId);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private ResponseEntity<String> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

    private ResponseEntity<String> forbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }


    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId,
                                     @RequestHeader("Authorization") String authHeader) {
        Optional<Long> tokenUserIdOpt = extractUserId(authHeader);
        if (tokenUserIdOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token");
        }
        if (!userId.equals(tokenUserIdOpt.get())) {
            return forbiddenResponse("You are not authorized to access this cart");
        }
        try {
            Cart cart = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving cart: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/add/{listingId}")
    public ResponseEntity<?> addItemToCart(@PathVariable Long userId,
                                           @PathVariable Long listingId,
                                           @RequestParam int quantity,
                                           @RequestHeader("Authorization") String authHeader) {
        Optional<Long> tokenUserIdOpt = extractUserId(authHeader);
        if (tokenUserIdOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token");
        }
        if (!userId.equals(tokenUserIdOpt.get())) {
            return forbiddenResponse("You are not authorized to modify this cart");
        }
        try {
            Cart updatedCart = cartService.addItemToCart(userId, listingId, quantity);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding item to cart: " + e.getMessage());
        }
    }

    @PutMapping("/{userId}/items/{listingId}")
    public ResponseEntity<?> updateCartItemQuantity(@PathVariable Long userId,
                                                    @PathVariable Long listingId,
                                                    @RequestParam int quantity,
                                                    @RequestHeader("Authorization") String authHeader) {
        Optional<Long> tokenUserIdOpt = extractUserId(authHeader);
        if (tokenUserIdOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token");
        }
        if (!userId.equals(tokenUserIdOpt.get())) {
            return forbiddenResponse("You are not authorized to modify this cart");
        }
        if (quantity < 1) {
            return ResponseEntity.badRequest().body("Quantity must be greater than 0");
        }
        try {
            Cart updatedCart = cartService.updateCartItemQuantity(userId, listingId, quantity);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating cart item: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/items/{listingId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long userId,
                                            @PathVariable Long listingId,
                                            @RequestHeader("Authorization") String authHeader) {
        Optional<Long> tokenUserIdOpt = extractUserId(authHeader);
        if (tokenUserIdOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token");
        }
        if (!userId.equals(tokenUserIdOpt.get())) {
            return forbiddenResponse("You are not authorized to modify this cart");
        }
        try {
            Cart updatedCart = cartService.removeCartItem(userId, listingId);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing item from cart: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable Long userId,
                                       @RequestHeader("Authorization") String authHeader) {
        Optional<Long> tokenUserIdOpt = extractUserId(authHeader);
        if (tokenUserIdOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token");
        }
        if (!userId.equals(tokenUserIdOpt.get())) {
            return forbiddenResponse("You are not authorized to modify this cart");
        }
        try {
            Cart cart = cartService.getCartByUserId(userId);
            cartService.clearCart(cart.getCartId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error clearing cart: " + e.getMessage());
        }
    }
}
