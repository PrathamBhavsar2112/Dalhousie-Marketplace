package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.Cart;
import com.dalhousie.dalhousie_marketplace_backend.model.CartItem;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.CartRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the CartService class with focus on active listing validation
 */
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Listing activeListing;
    private Listing inactiveListing;
    private Listing soldListing;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        // Create test cart
        testCart = new Cart(testUser.getUserId());
        testCart.setCartId(1L);

        // Create test listings with different statuses
        activeListing = createListing(1L, "Active Listing", 100.0, Listing.ListingStatus.ACTIVE);
        inactiveListing = createListing(2L, "Inactive Listing", 150.0, Listing.ListingStatus.INACTIVE);
        soldListing = createListing(3L, "Sold Listing", 200.0, Listing.ListingStatus.SOLD);

        // Setup mock responses
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
    }

    /**
     * Helper method to create a listing with the specified properties
     */
    private Listing createListing(Long id, String title, Double price, Listing.ListingStatus status) {
        Listing listing = new Listing();
        listing.setId(id);
        listing.setTitle(title);
        listing.setDescription("Description for " + title);
        listing.setPrice(price);
        listing.setQuantity(5);
        listing.setCategoryId(1L);
        listing.setStatus(status);
        listing.setViews(0);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());
        return listing;
    }

    @Test
    void getCartByUserId_ExistingCart() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));

        Cart result = cartService.getCartByUserId(testUser.getUserId());

        assertEquals(testCart, result);
        verify(cartRepository).findByUserId(testUser.getUserId());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCartByUserId_NewCart() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.getCartByUserId(testUser.getUserId());

        assertEquals(testCart, result);
        verify(cartRepository).findByUserId(testUser.getUserId());
        verify(cartRepository).save(any(Cart.class));
    }
    @Test
    void addItemToCart_WithActiveListing_Success() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(listingRepository.findById(activeListing.getId())).thenReturn(Optional.of(activeListing));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.addItemToCart(testUser.getUserId(), activeListing.getId(), 1);

        assertEquals(1, result.getCartItems().size());
        assertEquals(BigDecimal.valueOf(100.0), result.getTotalPrice());
        verify(cartRepository).save(testCart);
    }

    @Test
    void addItemToCart_ListingNotFound() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), 99L, 1);
        });

        assertEquals("Listing not found", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_InactiveListing() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(listingRepository.findById(inactiveListing.getId())).thenReturn(Optional.of(inactiveListing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), inactiveListing.getId(), 1);
        });

        assertEquals("Only active listings can be added to cart", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_SoldListing() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(listingRepository.findById(soldListing.getId())).thenReturn(Optional.of(soldListing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), soldListing.getId(), 1);
        });

        assertEquals("Only active listings can be added to cart", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_InsufficientQuantity() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(listingRepository.findById(activeListing.getId())).thenReturn(Optional.of(activeListing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), activeListing.getId(), 10);
        });

        assertEquals("Not enough quantity available. Available: 5", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void clearCart() {
        cartService.clearCart(testCart.getCartId());

        verify(cartRepository).deleteById(testCart.getCartId());
    }

    @Test
    void updateCartItemQuantity_Success() {
        CartItem cartItem = new CartItem(testCart, activeListing, 2, BigDecimal.valueOf(activeListing.getPrice()));
        testCart.getCartItems().add(cartItem);
        testCart.setTotalPrice(BigDecimal.valueOf(200.0)); // 2 * 100

        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.updateCartItemQuantity(testUser.getUserId(), activeListing.getId(), 3);

        assertEquals(3, result.getCartItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(300.0), result.getTotalPrice()); // 3 * 100
        verify(cartRepository).save(testCart);
    }

    @Test
    void updateCartItemQuantity_ItemNotFound() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.updateCartItemQuantity(testUser.getUserId(), activeListing.getId(), 3);
        });

        assertEquals("Item not found in cart", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeCartItem_RemovesItemFromList() {
        CartItem cartItem = new CartItem(testCart, activeListing, 2, BigDecimal.valueOf(activeListing.getPrice()));
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem);
        testCart.setCartItems(cartItems);
        testCart.setTotalPrice(BigDecimal.valueOf(200.0));

        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        Cart updatedCart = new Cart(testUser.getUserId());
        updatedCart.setCartId(testCart.getCartId());
        updatedCart.setTotalPrice(BigDecimal.ZERO);
        updatedCart.setCartItems(new ArrayList<>());
        when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

        Cart result = cartService.removeCartItem(testUser.getUserId(), activeListing.getId());

        assertEquals(0, result.getCartItems().size());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCartItem_UpdatesTotalPrice() {
        CartItem cartItem = new CartItem(testCart, activeListing, 2, BigDecimal.valueOf(activeListing.getPrice()));
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem);
        testCart.setCartItems(cartItems);
        testCart.setTotalPrice(BigDecimal.valueOf(200.0));

        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        Cart updatedCart = new Cart(testUser.getUserId());
        updatedCart.setCartId(testCart.getCartId());
        updatedCart.setTotalPrice(BigDecimal.ZERO);
        updatedCart.setCartItems(new ArrayList<>());
        when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

        Cart result = cartService.removeCartItem(testUser.getUserId(), activeListing.getId());

        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCartItem_ItemNotFound() {
        when(cartRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testCart));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.removeCartItem(testUser.getUserId(), activeListing.getId());
        });

        assertEquals("Item not found in cart", exception.getMessage());
        verify(cartRepository, never()).save(any());
    }
    /**
     * Test adding an active listing to cart succeeds
     */
    @Test
    void addItemToCart_withActiveListing_shouldSucceed() {
        // Arrange
        when(listingRepository.findById(activeListing.getId())).thenReturn(Optional.of(activeListing));

        // Act
        Cart result = cartService.addItemToCart(testUser.getUserId(), activeListing.getId(), 1);

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
        verify(listingRepository).findById(activeListing.getId());
    }

    /**
     * Test adding an inactive listing to cart throws exception
     */
    @Test
    void addItemToCart_withInactiveListing_shouldThrowException() {
        // Arrange
        when(listingRepository.findById(inactiveListing.getId())).thenReturn(Optional.of(inactiveListing));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), inactiveListing.getId(), 1);
        });

        assertEquals("Only active listings can be added to cart", exception.getMessage());
        verify(listingRepository).findById(inactiveListing.getId());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    /**
     * Test adding a sold listing to cart throws exception
     */
    @Test
    void addItemToCart_withSoldListing_shouldThrowException() {
        // Arrange
        when(listingRepository.findById(soldListing.getId())).thenReturn(Optional.of(soldListing));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getUserId(), soldListing.getId(), 1);
        });

        assertEquals("Only active listings can be added to cart", exception.getMessage());

        verify(listingRepository).findById(soldListing.getId());
        verify(cartRepository, never()).save(any(Cart.class));
    }
}