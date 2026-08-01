package com.fourguard.wms.infrastructure.cache;

import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.shared.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for managing user session revocation timestamps.
 * Integrates with Spring Cache (Redis/In-Memory) and fallback local memory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistAdapter implements TokenBlacklistPort {

    private final CacheManager cacheManager;
    private final Map<UUID, Long> localRevocationMap = new ConcurrentHashMap<>();

    @Override
    public void revokeUserSessions(UUID userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        localRevocationMap.put(userId, now);

        try {
            Cache cache = cacheManager.getCache(SecurityConstants.CACHE_SESSIONS);
            if (cache != null) {
                cache.put(userId.toString(), now);
            }
        } catch (Exception e) {
            log.warn("[Cache] Failed to put user revocation into cache manager: {}", e.getMessage());
        }

        log.info("[Security] Sessions revoked for userId: {} at timestamp: {}", userId, now);
    }

    @Override
    public boolean isUserRevoked(UUID userId, Date issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }

        long tokenIssuedAt = issuedAt.getTime();
        Long revokedAt = getRevocationTimestamp(userId);

        if (revokedAt == null) {
            return false;
        }

        // If the token was issued before or at the revocation timestamp, it is revoked.
        return tokenIssuedAt <= revokedAt;
    }

    private Long getRevocationTimestamp(UUID userId) {
        try {
            Cache cache = cacheManager.getCache(SecurityConstants.CACHE_SESSIONS);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(userId.toString());
                if (wrapper != null && wrapper.get() instanceof Number) {
                    return ((Number) wrapper.get()).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("[Cache] Failed to get user revocation from cache manager: {}", e.getMessage());
        }

        return localRevocationMap.get(userId);
    }
}
