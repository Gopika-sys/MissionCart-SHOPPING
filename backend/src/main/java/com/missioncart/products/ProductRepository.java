package com.missioncart.products;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySourcePlatformAndSourceProductId(String sourcePlatform, String sourceProductId);
    List<Product> findByPublishedTrueAndApprovedTrue();
    List<Product> findByPublishedTrueAndApprovedTrueAndImageStatus(String imageStatus);
    List<Product> findByCategoryIdAndPublishedTrueAndApprovedTrueAndIdNot(Long categoryId, Long id);
    List<Product> findBySellerId(Long sellerId);
    long countByStockQuantityLessThanEqual(int threshold);

    long countByPublishedTrueAndApprovedTrue();
    long countByImageStatus(String imageStatus);
    long countByPublishedTrueAndApprovedTrueAndImageStatus(String imageStatus);
    long countByAvailabilityStatus(String availabilityStatus);

    @Query("SELECT p.sourcePlatform, COUNT(p) FROM Product p GROUP BY p.sourcePlatform")
    List<Object[]> countGroupedByRetailer();

    @Query("SELECT p.categoryId, COUNT(p) FROM Product p GROUP BY p.categoryId")
    List<Object[]> countGroupedByCategory();
}
