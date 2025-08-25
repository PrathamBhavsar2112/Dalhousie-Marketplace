package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.Wishlist;
import com.dalhousie.dalhousie_marketplace_backend.model.WishlistItem;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.repository.WishlistRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ListingRepository listingRepository;

    public WishlistService(WishlistRepository wishlistRepository, ListingRepository listingRepository) {
        this.wishlistRepository = wishlistRepository;
        this.listingRepository = listingRepository;
    }

    public Wishlist getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(new Wishlist(userId)));
    }

    public Wishlist addItemToWishlist(Long userId, Long listingId) {
        Wishlist wishlist = getWishlistByUserId(userId);

        // Check if item already exists in wishlist
        boolean itemExists = wishlist.getWishlistItems().stream()
                .anyMatch(item -> item.getListing().getId().equals(listingId));

        if (itemExists) {
            throw new RuntimeException("Item already exists in wishlist");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        wishlist.addWishlistItem(new WishlistItem(wishlist, listing));
        return wishlistRepository.save(wishlist);
    }

    public void clearWishlist(Long wishlistId) {
        wishlistRepository.deleteById(wishlistId);
    }

    public Wishlist removeWishlistItem(Long userId, Long listingId) {
        Wishlist wishlist = getWishlistByUserId(userId);
        WishlistItem itemToRemove = findItemInWishlist(wishlist, listingId);

        wishlist.removeWishlistItem(itemToRemove);
        return wishlistRepository.save(wishlist);
    }

    private WishlistItem findItemInWishlist(Wishlist wishlist, Long listingId) {
        return wishlist.getWishlistItems().stream()
                .filter(item -> isMatchingListing(item, listingId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in wishlist"));
    }

    private boolean isMatchingListing(WishlistItem item, Long listingId) {
        return item.getListing().getId().equals(listingId);
    }
}
