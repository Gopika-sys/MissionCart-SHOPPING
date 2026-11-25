package com.missioncart.products;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByPrimaryImageDescSortOrderAsc(Long productId);
    List<ProductImage> findByImageStatus(String imageStatus);
}
