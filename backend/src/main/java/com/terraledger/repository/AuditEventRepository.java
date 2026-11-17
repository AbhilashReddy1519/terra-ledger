package com.terraledger.repository;

import com.terraledger.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findByEntityIdOrderByCreatedAtDesc(String entityId);
    List<AuditEvent> findAllByOrderByCreatedAtDesc();
}
