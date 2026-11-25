package com.missioncart.lifecycle;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LifecycleRequestRepository extends JpaRepository<LifecycleRequest, Long> {
    List<LifecycleRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LifecycleRequest> findAllByOrderByCreatedAtDesc();
    long countByStatusContainingIgnoreCase(String part);
}
