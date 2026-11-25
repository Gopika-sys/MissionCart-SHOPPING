package com.missioncart.cart;

import com.missioncart.cart.CartService.CartSummaryDto;
import com.missioncart.common.ApiResponse;
import com.missioncart.products.ProductDtos.ProductDto;
import com.missioncart.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartAndWishlistController {

    private final CartService cartService;

    public CartAndWishlistController(CartService cartService) {
        this.cartService = cartService;
    }

    public record AddCartItemRequest(Long productId, Integer quantity, Long missionId) {}
    public record UpdateCartItemRequest(Integer quantity, Boolean savedForLater) {}

    @GetMapping("/cart")
    public ApiResponse<CartSummaryDto> getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(cartService.getCart(user.id()));
    }

    @PostMapping("/cart/items")
    public ApiResponse<CartSummaryDto> addToCart(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody AddCartItemRequest req) {
        return ApiResponse.ok("Added to bag", cartService.addItem(user.id(), req.productId(), req.quantity() != null ? req.quantity() : 1, req.missionId()));
    }

    @PatchMapping("/cart/items/{id}")
    public ApiResponse<CartSummaryDto> updateItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @RequestBody UpdateCartItemRequest req) {
        return ApiResponse.ok("Bag updated", cartService.updateItem(user.id(), id, req.quantity(), req.savedForLater()));
    }

    @DeleteMapping("/cart/items/{id}")
    public ApiResponse<CartSummaryDto> removeItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok("Item removed", cartService.removeItem(user.id(), id));
    }

    @GetMapping("/wishlist")
    public ApiResponse<List<ProductDto>> getWishlist(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(cartService.getWishlist(user.id()));
    }

    @PostMapping("/wishlist/{productId}")
    public ApiResponse<List<ProductDto>> toggleWishlist(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId) {
        return ApiResponse.ok("Wishlist updated", cartService.toggleWishlist(user.id(), productId));
    }
}
