package com.yowyob.easyrental.modules.subscription.listener;

import com.yowyob.easyrental.shared.events.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionListener {
    @EventListener
    @Async
    public void onPlanChange(AuditEvent event) {
        if ("UPGRADE_PLAN".equals(event.action())) {
            log.info("🚀 [QUOTA UPDATE] L'organisation a changé de plan. Détails: {}", event.details());
        }
    }
}