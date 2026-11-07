package com.sharath.banking.common.repository;

import com.sharath.banking.common.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
