package com.missioncart.possessions;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserPossessionRepository extends JpaRepository<UserPossession, Long> {
    List<UserPossession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
