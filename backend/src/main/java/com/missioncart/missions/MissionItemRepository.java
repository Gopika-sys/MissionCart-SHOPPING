package com.missioncart.missions;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MissionItemRepository extends JpaRepository<MissionItem, Long> {
    List<MissionItem> findByMissionIdOrderBySortOrderAscIdAsc(Long missionId);
}
