package com.missioncart.missions;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Mission> findByShareToken(String shareToken);
    Optional<Mission> findBySlug(String slug);
}
