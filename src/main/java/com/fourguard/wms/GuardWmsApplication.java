package com.fourguard.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 4GUARD WMS — Backend Application Entry Point.
 *
 * <p>Redis auto-configurations are excluded globally. The cache layer is
 * conditionally activated by setting {@code cache.redis.enabled=true}
 * in the active profile. By default, an in-memory cache is used and
 * the application starts correctly without a Redis instance.</p>
 *
 * <p>Architecture: Hexagonal (Ports & Adapters) — no Spring annotations
 * are used inside the {@code domain} package.</p>
 */
@SpringBootApplication(
        excludeName = {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
        }
)
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
public class GuardWmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardWmsApplication.class, args);
    }
}
