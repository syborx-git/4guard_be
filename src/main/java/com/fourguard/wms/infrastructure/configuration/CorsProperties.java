package com.fourguard.wms.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS settings bound from {@code security.cors.*}.
 */
@Component
@ConfigurationProperties(prefix = "security.cors")
@Getter
@Setter
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:8080",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:4201"
    ));

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.ngrok-free.dev",
            "https://*.onrender.com"
    ));

    private List<String> allowedMethods = new ArrayList<>(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    );

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private boolean allowCredentials = true;
}
