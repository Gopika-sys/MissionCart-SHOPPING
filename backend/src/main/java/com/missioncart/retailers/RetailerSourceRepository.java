package com.missioncart.retailers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RetailerSourceRepository extends JpaRepository<RetailerSource, Long> {
    Optional<RetailerSource> findByCodeIgnoreCase(String code);
    List<RetailerSource> findAllByOrderByIdAsc();
}
