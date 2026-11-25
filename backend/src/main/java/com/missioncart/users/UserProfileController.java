package com.missioncart.users;

import com.missioncart.common.ApiResponse;
import com.missioncart.products.Review;
import com.missioncart.products.ReviewRepository;
import com.missioncart.security.AuthenticatedUser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserProfileController {

    private final UserRepository userRepo;
    private final AddressRepository addressRepo;
    private final ReviewRepository reviewRepo;

    public UserProfileController(UserRepository userRepo, AddressRepository addressRepo, ReviewRepository reviewRepo) {
        this.userRepo = userRepo;
        this.addressRepo = addressRepo;
        this.reviewRepo = reviewRepo;
    }

    public record UpdateProfileRequest(String fullName, String phone, String avatarUrl) {}
    public record AddressRequest(String label, String recipientName, String phone, String line1, String line2, String city, String state, String postalCode, boolean defaultAddress) {}
    public record CreateReviewRequest(Long productId, int rating, String title, String comment) {}

    @PatchMapping("/users/profile")
    public ApiResponse<User> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal, @RequestBody UpdateProfileRequest req) {
        User u = userRepo.findById(principal.id()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (req.fullName() != null && !req.fullName().isBlank()) u.setFullName(req.fullName().trim());
        if (req.phone() != null) u.setPhone(req.phone().trim());
        if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl().trim());
        u.setPasswordHash(u.getPasswordHash());
        userRepo.save(u);
        u.setPasswordHash("[PROTECTED]");
        return ApiResponse.ok("Profile updated", u);
    }

    @GetMapping("/addresses")
    public ApiResponse<List<Address>> listAddresses(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(addressRepo.findByUserIdOrderByDefaultAddressDescIdDesc(principal.id()));
    }

    @PostMapping("/addresses")
    public ApiResponse<Address> addAddress(@AuthenticationPrincipal AuthenticatedUser principal, @RequestBody AddressRequest req) {
        Address a = new Address();
        a.setUserId(principal.id());
        a.setLabel(req.label() != null ? req.label() : "Home");
        a.setRecipientName(req.recipientName());
        a.setPhone(req.phone());
        a.setLine1(req.line1());
        a.setLine2(req.line2());
        a.setCity(req.city());
        a.setState(req.state());
        a.setPostalCode(req.postalCode());
        a.setDefaultAddress(req.defaultAddress());
        return ApiResponse.ok("Address saved", addressRepo.save(a));
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        Address a = addressRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Address not found"));
        if (!a.getUserId().equals(principal.id())) throw new IllegalArgumentException("Not authorized");
        addressRepo.delete(a);
        return ApiResponse.ok("Address deleted", null);
    }

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<List<Review>> productReviews(@PathVariable Long productId) {
        return ApiResponse.ok(reviewRepo.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId));
    }

    @PostMapping("/reviews")
    public ApiResponse<Review> submitReview(@AuthenticationPrincipal AuthenticatedUser principal, @RequestBody CreateReviewRequest req) {
        Review r = new Review();
        r.setProductId(req.productId());
        r.setUserId(principal.id());
        r.setRating(Math.min(5, Math.max(1, req.rating())));
        r.setTitle(req.title());
        r.setComment(req.comment());
        return ApiResponse.ok("Review submitted", reviewRepo.save(r));
    }
}
