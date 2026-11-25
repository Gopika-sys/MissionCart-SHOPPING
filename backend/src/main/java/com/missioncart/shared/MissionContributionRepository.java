package com.missioncart.shared;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MissionContributionRepository extends JpaRepository<MissionContribution, Long> {
    List<MissionContribution> findByMissionIdOrderByCreatedAtDesc(Long missionId);
}
