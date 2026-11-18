package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    /** Login lookup — supports username OR email (case-insensitive). */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)")
    Optional<UserEntity> findByUsernameOrEmail(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UserEntity> findByOrganizationId(UUID organizationId);

    /**
     * Finds the primary administrator of a given organisation.
     * The admin is the enabled user whose role has the lowest level (level = 1),
     * ordered by account creation date to get a deterministic result.
     */
    @Query("SELECT u FROM UserEntity u JOIN u.role r " +
           "WHERE u.organization.id = :orgId AND r.level = 1 AND u.isEnabled = true " +
           "ORDER BY u.createdAt ASC")
    Optional<UserEntity> findTopAdminByOrganizationId(@Param("orgId") UUID orgId);
}

