package com.fourguard.wms.domain.ports.in;

import java.security.Principal;
import java.util.UUID;

/**
 * Port of entry for revoking a target user's active session.
 */
public interface RevokeUserSessionUseCase {

    /**
     * Revokes all active sessions for the given target user ID.
     *
     * @param targetUserId ID of the user whose session will be revoked
     * @param principal Principal of the performing administrator
     */
    void revokeUserSession(UUID targetUserId, Principal principal);
}
