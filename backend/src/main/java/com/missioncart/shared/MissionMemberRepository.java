package com.missioncart.shared;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MissionMemberRepository extends JpaRepository<MissionMember, Long> {
    List<MissionMember> findByMissionIdOrderByJoinedAtAsc(Long missionId);
    Optional<MissionMember> findByMissionIdAndUserId(Long missionId, Long userId);
}
