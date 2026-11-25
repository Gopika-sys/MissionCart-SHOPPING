package com.missioncart.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatalogImportJobRepository extends JpaRepository<CatalogImportJob, Long> {
    List<CatalogImportJob> findAllByOrderByCreatedAtDesc();
}
