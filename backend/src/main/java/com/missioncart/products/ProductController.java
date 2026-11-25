package com.missioncart.products;

import com.missioncart.common.ApiResponse;
import com.missioncart.common.PageResponse;
import com.missioncart.products.ProductDtos.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/count")
    public ApiResponse<ProductCountsDto> count() {
        return ApiResponse.ok(productService.getCounts());
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductDto>> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String categories,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String sources,
        @RequestParam(required = false) String sourcePlatform,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String tier,
        @RequestParam(required = false, defaultValue = "true") Boolean verifiedImagesOnly,
        @RequestParam(required = false) Boolean inStockOnly,
        @RequestParam(required = false, defaultValue = "relevance") String sort,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "24") int size
    ) {
        String catParam = categories != null && !categories.isBlank() ? categories : category;
        String srcParam = sources != null && !sources.isBlank() ? sources : (sourcePlatform != null && !sourcePlatform.isBlank() ? sourcePlatform : source);
        return ApiResponse.ok(productService.searchProducts(
            q, catParam, srcParam, brandId, minPrice, maxPrice, tier, verifiedImagesOnly, inStockOnly, sort, page, size
        ));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ProductDto>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String categories,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String sources,
        @RequestParam(required = false) String sourcePlatform,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String tier,
        @RequestParam(required = false, defaultValue = "true") Boolean verifiedImagesOnly,
        @RequestParam(required = false) Boolean inStockOnly,
        @RequestParam(required = false, defaultValue = "relevance") String sort,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "24") int size
    ) {
        String catParam = categories != null && !categories.isBlank() ? categories : category;
        String srcParam = sources != null && !sources.isBlank() ? sources : (sourcePlatform != null && !sourcePlatform.isBlank() ? sourcePlatform : source);
        return ApiResponse.ok(productService.searchProducts(
            q, catParam, srcParam, brandId, minPrice, maxPrice, tier, verifiedImagesOnly, inStockOnly, sort, page, size
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> detail(@PathVariable String id) {
        return ApiResponse.ok(productService.getByIdOrSlug(id));
    }

    @GetMapping("/{id}/alternatives")
    public ApiResponse<AlternativesDto> alternatives(@PathVariable Long id) {
        return ApiResponse.ok(productService.getAlternatives(id));
    }
}
