package com.fourguard.wms.infrastructure.configuration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration strategy.
 *
 * <p>Ensures that any previous failed migration records (such as those that failed
 * before a bug fix) are repaired/cleaned automatically prior to running migrations,
 * preventing 'Migration failed! Please run repair' errors in cloud deployment environments
 * like Render, Supabase, or AWS RDS.</p>
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway repair before migration to clean up any failed migration states...");
            flyway.repair();
            log.info("Executing Flyway migrate...");
            flyway.migrate();
            log.info("Flyway migration completed successfully.");
        };
    }
}
