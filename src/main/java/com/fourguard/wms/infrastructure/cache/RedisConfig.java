package com.fourguard.wms.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Decoupled cache configuration — supports both in-memory and Redis.
 *
 * <h2>How to switch between modes</h2>
 * <pre>
 *   # application-dev.yml  (default — no Redis needed)
 *   cache.redis.enabled: false
 *
 *   # application-prod.yml or env var
 *   cache.redis.enabled: true
 *   spring.data.redis.host: redis-host
 *   spring.data.redis.port: 6379
 * </pre>
 *
 * <p>The Redis auto-configurations ({@code RedisAutoConfiguration},
 * {@code RedisReactiveAutoConfiguration}) are excluded globally in
 * {@code GuardWmsApplication} to prevent Spring Boot from attempting a
 * Redis connection at startup when Redis is disabled.</p>
 *
 * <h2>Cache regions</h2>
 * <ul>
 *   <li>{@code roles}       — 1 hour TTL</li>
 *   <li>{@code permissions} — 1 hour TTL</li>
 *   <li>{@code sessions}    — 60 minutes TTL</li>
 *   <li>{@code catalogues}  — 6 hours TTL</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // ─── DEFAULT: In-memory cache ─────────────────────────────────────────────

    /**
     * Active when {@code cache.redis.enabled=false} (default).
     * Zero infrastructure dependencies — application starts without Redis.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        log.info("[Cache] Mode: IN-MEMORY (ConcurrentMapCacheManager). Redis is disabled.");
        return new ConcurrentMapCacheManager(
                "roles", "permissions", "catalogues", "sessions"
        );
    }

    // ─── OPTIONAL: Redis cache ────────────────────────────────────────────────

    /**
     * Creates a Lettuce connection factory only when Redis is explicitly enabled.
     * The global exclusion of {@code RedisAutoConfiguration} ensures this bean
     * is the sole source of Redis connections.
     */
    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("[Cache] Connecting to Redis at {}:{}", redisHost, redisPort);
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * Redis-backed {@link CacheManager} with per-region TTL configuration.
     * Active only when {@code cache.redis.enabled=true}.
     */
    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(LettuceConnectionFactory connectionFactory) {
        log.info("[Cache] Mode: REDIS (RedisCacheManager).");

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration("roles",       base.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("permissions",  base.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("sessions",     base.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("catalogues",   base.entryTtl(Duration.ofHours(6)))
                .build();
    }
}
