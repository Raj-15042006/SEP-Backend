package com.passport.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query(value = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<AuditLog> findTopByOrderByTimestampDesc();

    List<AuditLog> findByResourceIdOrderByTimestampDesc(String resourceId);

    List<AuditLog> findTop50ByOrderByTimestampDesc();
}
