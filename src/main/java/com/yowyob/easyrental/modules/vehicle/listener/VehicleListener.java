package com.yowyob.easyrental.modules.vehicle.listener;

import com.yowyob.easyrental.shared.events.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VehicleListener {

    @EventListener
    @Async
    public void handleVehicleEvent(AuditEvent event) {
        if ("VEHICLE_MAINTENANCE".equals(event.action())) {
            log.info("🛠️ [Alerte Maintenance] Un véhicule nécessite une attention. Détails : {}", event.details());
            // Ici, on pourrait déclencher l'envoi d'un email au Manager d'agence
        }
        
        if ("VEHICLE_STATUS_CHANGE".equals(event.action())) {
            log.info("🚗 [Mise à jour Flotte] Changement de statut véhicule : {}", event.details());
        }
    }
}