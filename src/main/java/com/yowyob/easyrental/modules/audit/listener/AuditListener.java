package com.yowyob.easyrental.modules.audit.listener;

import com.yowyob.easyrental.modules.audit.domain.AuditEntity;
import com.yowyob.easyrental.modules.audit.infrastructure.adapter.out.persistence.AuditRepository;
import com.yowyob.easyrental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditListener {

    private final AuditRepository auditRepository;

    @EventListener
    @Async
    public void handleAuditEvent(AuditEvent event) {
        AuditEntity audit = AuditEntity.builder()
                .id(UUID.randomUUID())
                .action(event.action())
                .module(event.module())
                .details(event.details())
                .timestamp(LocalDateTime.now())
                .isNewRecord(true) // <--- IMPORTANT : Force l'INSERT
                .build();

        if (audit != null) {
            auditRepository.save(audit)
                    .doOnError(e -> log.error("Erreur lors de l'enregistrement de l'audit: {}", e.getMessage()))
                    .subscribe(); // Fire-and-forget
}
    }
}
