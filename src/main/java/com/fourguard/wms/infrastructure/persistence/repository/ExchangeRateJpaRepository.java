package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ExchangeRateStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateEntity, UUID> {

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.organizationId = :organizationId " +
           "AND e.fromCurrencyId = :fromCurrencyId AND e.toCurrencyId = :toCurrencyId " +
           "AND e.effectiveDate <= :date AND e.status = 'ACTIVE' " +
           "ORDER BY e.effectiveDate DESC, e.createdAt DESC")
    List<ExchangeRateEntity> findTopRates(@Param("organizationId") UUID organizationId,
                                          @Param("fromCurrencyId") UUID fromCurrencyId,
                                          @Param("toCurrencyId") UUID toCurrencyId,
                                          @Param("date") LocalDate date);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.organizationId = :organizationId " +
           "AND (:fromCurrencyId IS NULL OR e.fromCurrencyId = :fromCurrencyId) " +
           "AND (:toCurrencyId IS NULL OR e.toCurrencyId = :toCurrencyId) " +
           "AND (:date IS NULL OR e.effectiveDate = :date) " +
           "ORDER BY e.effectiveDate DESC, e.createdAt DESC")
    List<ExchangeRateEntity> findWithFilters(@Param("organizationId") UUID organizationId,
                                             @Param("fromCurrencyId") UUID fromCurrencyId,
                                             @Param("toCurrencyId") UUID toCurrencyId,
                                             @Param("date") LocalDate date);

    List<ExchangeRateEntity> findAllByOrganizationIdAndStatus(UUID organizationId, ExchangeRateStatus status);
}
