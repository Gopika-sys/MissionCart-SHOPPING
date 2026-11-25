package com.missioncart.cart;

import com.missioncart.categories.Brand;
import com.missioncart.categories.BrandRepository;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.compatibility.CompatibilityEngine;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import com.missioncart.products.Product;
import com.missioncart.products.ProductDtos.ProductDto;
import com.missioncart.products.ProductRepository;
import com.missioncart.products.ProductService;
import com.missioncart.retailers.RetailerSource;
import com.missioncart.retailers.RetailerSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartService {

    public record CartItemView(
        Long id,
        Long productId,
        Long missionId,
        int quantity,
        boolean savedForLater,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean nativeCheckoutEligible,
        ProductDto product
    ) {}

    public record RetailerCartGroup(
        String sourceCode,
        String sourceName,
        String officialWebsite,
        boolean nativeCheckout,
        BigDecimal subtotal,
        List<CartItemView> items
    ) {}

    public record CartSummaryDto(
        Long cartId,
        List<CartItemView> activeItems,
        List<CartItemView> savedForLaterItems,
        List<RetailerCartGroup> externalRetailerGroups,
        BigDecimal nativeSubtotal,
        BigDecimal externalSubtotal,
        BigDecimal grandTotal,
        String currency,
        CompatibilityReport compatibilityReport
    ) {}

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final WishlistRepository wishlistRepo;
    private final WishlistItemRepository wishlistItemRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final RetailerSourceRepository retailerRepo;
    private final ProductService productService;
    private final CompatibilityEngine compatibilityEngine;

    public CartService(
        CartRepository cartRepo,
        CartItemRepository itemRepo,
        WishlistRepository wishlistRepo,
        WishlistItemRepository wishlistItemRepo,
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        BrandRepository brandRepo,
        RetailerSourceRepository retailerRepo,
        ProductService productService,
        CompatibilityEngine compatibilityEngine
    ) {
        this.cartRepo = cartRepo;
        this.itemRepo = itemRepo;
        this.wishlistRepo = wishlistRepo;
        this.wishlistItemRepo = wishlistItemRepo;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.retailerRepo = retailerRepo;
        this.productService = productService;
        this.compatibilityEngine = compatibilityEngine;
    }

    public CartSummaryDto getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> rawItems = itemRepo.findByCartIdOrderByCreatedAtAsc(cart.getId());
        Map<Long, Product> productMap = productRepo.findAllById(rawItems.stream().map(CartItem::getProductId).toList())
            .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Category> catMap = categoryRepo.findAll().stream().collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, Brand> brandMap = brandRepo.findAll().stream().collect(Collectors.toMap(Brand::getId, b -> b));
        Map<String, RetailerSource> sourceMap = retailerRepo.findAll().stream()
            .collect(Collectors.toMap(s -> s.getCode().toLowerCase(), s -> s, (a, b) -> a));

        List<CartItemView> active = new ArrayList<>();
        List<CartItemView> saved = new ArrayList<>();
        BigDecimal nativeSub = BigDecimal.ZERO;
        BigDecimal externalSub = BigDecimal.ZERO;
        Map<String, List<CartItemView>> extGrouped = new LinkedHashMap<>();

        for (CartItem ci : rawItems) {
            Product p = productMap.get(ci.getProductId());
            if (p == null) continue;
            ProductDto pdto = productService.toDto(p, catMap.get(p.getCategoryId()), p.getBrandId() != null ? brandMap.get(p.getBrandId()) : null, false);
            BigDecimal unit = p.getPrice();
            BigDecimal line = unit != null ? unit.multiply(BigDecimal.valueOf(ci.getQuantity())) : null;
            boolean isNative = "MISSIONCART_DIRECT".equalsIgnoreCase(p.getProductType()) || "SELLER_MANAGED".equalsIgnoreCase(p.getProductType());
            CartItemView view = new CartItemView(ci.getId(), p.getId(), ci.getMissionId(), ci.getQuantity(), ci.isSavedForLater(), unit, line, isNative, pdto);

            if (ci.isSavedForLater()) {
                saved.add(view);
            } else {
                active.add(view);
                if (isNative && line != null) {
                    nativeSub = nativeSub.add(line);
                } else if (!isNative) {
                    if (line != null) externalSub = externalSub.add(line);
                    extGrouped.computeIfAbsent(p.getSourcePlatform(), k -> new ArrayList<>()).add(view);
                }
            }
        }

        List<RetailerCartGroup> externalGroups = new ArrayList<>();
        for (Map.Entry<String, List<CartItemView>> entry : extGrouped.entrySet()) {
            RetailerSource src = sourceMap.get(entry.getKey().toLowerCase());
            BigDecimal sub = entry.getValue().stream().map(CartItemView::lineTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            externalGroups.add(new RetailerCartGroup(
                entry.getKey(),
                src != null ? src.getName() : entry.getKey(),
                src != null ? src.getOfficialWebsite() : "",
                src != null && src.isNativeCheckout(),
                sub,
                entry.getValue()
            ));
        }

        CompatibilityReport report = compatibilityEngine.evaluate(
            active.stream().map(CartItemView::productId).toList(), Map.of()
        );

        return new CartSummaryDto(
            cart.getId(), active, saved, externalGroups,
            nativeSub, externalSub, nativeSub.add(externalSub), "INR", report
        );
    }

    @Transactional
    public CartSummaryDto addItem(Long userId, Long productId, int quantity, Long missionId) {
        Product p = productRepo.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        Cart cart = getOrCreateCart(userId);
        CartItem existing = itemRepo.findByCartIdAndProductId(cart.getId(), p.getId()).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + Math.max(1, quantity));
            existing.setSavedForLater(false);
            if (missionId != null) existing.setMissionId(missionId);
            itemRepo.save(existing);
        } else {
            CartItem ci = new CartItem();
            ci.setCartId(cart.getId());
            ci.setProductId(p.getId());
            ci.setMissionId(missionId);
            ci.setQuantity(Math.max(1, quantity));
            itemRepo.save(ci);
        }
        cart.setUpdatedAt(Instant.now());
        cartRepo.save(cart);
        return getCart(userId);
    }

    @Transactional
    public CartSummaryDto updateItem(Long userId, Long itemId, Integer quantity, Boolean savedForLater) {
        Cart cart = getOrCreateCart(userId);
        CartItem ci = itemRepo.findById(itemId).orElseThrow(() -> new EntityNotFoundException("Cart item not found"));
        if (!ci.getCartId().equals(cart.getId())) throw new IllegalArgumentException("Not authorized");
        if (quantity != null) {
            if (quantity <= 0) {
                itemRepo.delete(ci);
                return getCart(userId);
            }
            ci.setQuantity(quantity);
        }
        if (savedForLater != null) ci.setSavedForLater(savedForLater);
        itemRepo.save(ci);
        return getCart(userId);
    }

    @Transactional
    public CartSummaryDto removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem ci = itemRepo.findById(itemId).orElseThrow(() -> new EntityNotFoundException("Cart item not found"));
        if (!ci.getCartId().equals(cart.getId())) throw new IllegalArgumentException("Not authorized");
        itemRepo.delete(ci);
        return getCart(userId);
    }

    @Transactional
    public List<ProductDto> getWishlist(Long userId) {
        Wishlist wl = getOrCreateWishlist(userId);
        List<WishlistItem> items = wishlistItemRepo.findByWishlistIdOrderByCreatedAtDesc(wl.getId());
        Map<Long, Category> catMap = categoryRepo.findAll().stream().collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, Brand> brandMap = brandRepo.findAll().stream().collect(Collectors.toMap(Brand::getId, b -> b));
        return productRepo.findAllById(items.stream().map(WishlistItem::getProductId).toList()).stream()
            .map(p -> productService.toDto(p, catMap.get(p.getCategoryId()), p.getBrandId() != null ? brandMap.get(p.getBrandId()) : null, false))
            .toList();
    }

    @Transactional
    public List<ProductDto> toggleWishlist(Long userId, Long productId) {
        productRepo.findById(productId).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        Wishlist wl = getOrCreateWishlist(userId);
        Optional<WishlistItem> existing = wishlistItemRepo.findByWishlistIdAndProductId(wl.getId(), productId);
        if (existing.isPresent()) {
            wishlistItemRepo.delete(existing.get());
        } else {
            WishlistItem wi = new WishlistItem();
            wi.setWishlistId(wl.getId());
            wi.setProductId(productId);
            wishlistItemRepo.save(wi);
        }
        return getWishlist(userId);
    }

    public Cart getOrCreateCart(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return cartRepo.save(c);
        });
    }

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepo.findByUserId(userId).orElseGet(() -> {
            Wishlist w = new Wishlist();
            w.setUserId(userId);
            return wishlistRepo.save(w);
        });
    }
}
