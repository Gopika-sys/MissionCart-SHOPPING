package com.missioncart.categories;

import com.missioncart.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryAndBrandController {

    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;

    public CategoryAndBrandController(CategoryRepository categoryRepo, BrandRepository brandRepo) {
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(categoryRepo.findAllByOrderByDisplayOrderAscNameAsc());
    }

    @GetMapping("/brands")
    public ApiResponse<List<Brand>> brands() {
        return ApiResponse.ok(brandRepo.findAll());
    }
}
