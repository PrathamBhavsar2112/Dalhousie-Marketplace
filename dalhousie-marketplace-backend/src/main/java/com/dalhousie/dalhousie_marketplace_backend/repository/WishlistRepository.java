package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @EntityGraph(attributePaths = {"wishlistItems", "wishlistItems.listing"})
    Optional<Wishlist> findByUserId(Long userId);
}