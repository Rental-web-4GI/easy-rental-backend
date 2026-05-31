package com.yowyob.easyrental.infrastructure.adapter.out.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Elasticsearch adapter for vehicle and agency search.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Component
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class VehicleSearchAdapter {

    /**
     * Indexes a document in Elasticsearch.
     *
     * @param index target index name
     * @param id document id
     * @param document JSON document body
     * @return completion signal
     */
    public Mono<Void> indexDocument(String index, String id, String document) {
        return Mono.empty();
    }
}
