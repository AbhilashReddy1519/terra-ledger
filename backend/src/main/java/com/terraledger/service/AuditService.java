package com.terraledger.service;

import com.terraledger.domain.AuditEvent;
import com.terraledger.dto.AuditEventResponse;
import com.terraledger.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent logEvent(String actor, String action, String entity, String entityId, String detail) {
        AuditEvent event = new AuditEvent(actor, action, entity, entityId, detail);
        AuditEvent saved = auditEventRepository.save(event);
        log.info("Audit logged: [action={}] [actor={}] [entity={}:{}]", action, actor, entity, entityId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getEventsForEntity(String entityId) {
        return auditEventRepository.findByEntityIdOrderByCreatedAtDesc(entityId)
                .stream()
                .map(AuditEventResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAllEvents() {
        return auditEventRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AuditEventResponse::fromEntity)
                .toList();
    }
}
