package com.fourguard.wms.shared.constants;

/**
 * Application-wide security constants.
 *
 * <p>Centralising these values here avoids magic strings scattered across
 * the codebase and simplifies configuration changes.</p>
 */
public final class SecurityConstants {

    // ── Construction prevention ───────────────────────────────────────────────
    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── URL patterns ──────────────────────────────────────────────────────────

    /** Public auth endpoints — no JWT required. */
    public static final String AUTH_PATTERN              = "/auth/**";

    /** Password reset (temporary) — public, called before login. */
    public static final String RESET_PASSWORD_TEMP_PATTERN = "/users/reset-password-temp";

    /** Public Security Gate endpoints for drivers — no JWT required. */
    public static final String SECURITY_GATE_PUBLIC_PATTERN = "/security-gate/public/**";

    /** Swagger UI assets — public in dev, consider restricting in prod. */
    public static final String SWAGGER_UI_PATTERN   = "/swagger-ui/**";
    public static final String SWAGGER_DOCS_PATTERN = "/v3/api-docs/**";

    /** Spring Boot Actuator health check — always public. */
    public static final String ACTUATOR_HEALTH      = "/actuator/health";

    // ── HTTP Header ───────────────────────────────────────────────────────────

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX        = "Bearer ";

    // ── JWT Claim keys ────────────────────────────────────────────────────────

    public static final String CLAIM_USER_ID     = "userId";
    public static final String CLAIM_EMAIL       = "email";
    public static final String CLAIM_ROLE        = "role";
    public static final String CLAIM_PERMISSIONS = "permissions";

    // ── Token expiration defaults (milliseconds) ──────────────────────────────
    // Real values are read from security.jwt.* properties in application.yml.
    // These constants serve as documented defaults only.

    /** 1 hour. */
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_MS  = 3_600_000L;

    /** 7 days. */
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L;

    /** 15 minutes. */
    public static final long DEFAULT_RESET_TOKEN_EXPIRATION_MS   = 900_000L;

    // ── Cache names ───────────────────────────────────────────────────────────

    public static final String CACHE_ROLES       = "roles";
    public static final String CACHE_PERMISSIONS = "permissions";
    public static final String CACHE_CATALOGUES  = "catalogues";
    public static final String CACHE_SESSIONS    = "sessions";
}