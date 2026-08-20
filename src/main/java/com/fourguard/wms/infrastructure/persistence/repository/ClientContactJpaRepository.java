package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.ClientContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Repositorio JPA — Contactos Corporativos de Clientes (wms.client_contacts). */
@Repository
public interface ClientContactJpaRepository extends JpaRepository<ClientContactEntity, UUID> {
    List<ClientContactEntity> findByClientId(UUID clientId);
    void deleteByClientId(UUID clientId);
}
