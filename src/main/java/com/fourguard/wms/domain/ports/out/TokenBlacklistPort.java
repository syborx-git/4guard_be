package com.fourguard.wms.domain.ports.out;

import java.util.Date;
import java.util.UUID;

/**
 * Port for managing user/token session revocations and blacklist checks.
 */
public interface TokenBlacklistPort {

    /**
     * Records a revocation for the specified user starting at current time.
     *
     * @param userId ID of the user whose sessions are revoked
     */
    void revokeUserSessions(UUID userId);

    /**
     * Checks if a user session/token has been revoked based on its issuedAt timestamp.
     *
     * @param userId ID of the user
     * @param issuedAt Date when the token was issued
     * @return true if the token was issued before or at the user's revocation timestamp
     */
    boolean isUserRevoked(UUID userId, Date issuedAt);
}
