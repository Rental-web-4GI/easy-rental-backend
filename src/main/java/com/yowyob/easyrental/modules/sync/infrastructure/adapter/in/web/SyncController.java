package com.yowyob.easyrental.modules.sync.infrastructure.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST adapter for DuckDB-PostgreSQL sync operations.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    /**
     * Pulls latest data for local DuckDB cache.
     *
     * @return sync payload with vehicles and agencies
     */
    @GetMapping("/pull")
    public Mono<Map<String, Object>> pull() {
        return Mono.just(Map.of(
                "vehicles", List.of(),
                "agencies", List.of(),
                "rentals", List.of(),
                "synced_at", System.currentTimeMillis()
        ));
    }

    /**
     * Pushes queued local changes from DuckDB to PostgreSQL.
     *
     * @param payload queued operations from client
     * @return acknowledgment
     */
    @PostMapping("/push")
    public Mono<Map<String, Object>> push(@RequestBody Map<String, Object> payload) {
        return Mono.just(Map.of(
                "status", "accepted",
                "processed", payload.getOrDefault("operations", List.of()).toString()
        ));
    }
}
