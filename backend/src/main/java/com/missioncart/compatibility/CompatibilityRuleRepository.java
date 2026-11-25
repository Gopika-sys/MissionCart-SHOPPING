package com.missioncart.compatibility;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {
    List<CompatibilityRule> findByActiveTrue();
}
