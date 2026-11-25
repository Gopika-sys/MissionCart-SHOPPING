package com.missioncart.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByWishlistIdOrderByCreatedAtDesc(Long wishlistId);
    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);
}
