package com.missioncart.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {
    List<PaymentAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
