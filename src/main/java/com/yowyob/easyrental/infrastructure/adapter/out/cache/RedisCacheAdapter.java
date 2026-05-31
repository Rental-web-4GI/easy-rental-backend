package com.yowyob.easyrental.infrastructure.adapter.out.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis cache adapter for reactive caching.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheAdapter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisCacheAdapter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> put(String key, String value, Duration ttl) {
        return redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Mono<String> get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> evict(String key) {
        return redisTemplate.delete(key).map(count -> count > 0);
    }
}
