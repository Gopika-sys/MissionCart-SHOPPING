package com.missioncart.shared;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MissionInvitationRepository extends JpaRepository<MissionInvitation, Long> {
    Optional<MissionInvitation> findByTokenAndActiveTrue(String token);
    Optional<MissionInvitation> findByMissionIdAndActiveTrue(Long missionId);
}
