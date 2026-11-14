package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.response.audit.UserWorkHoursResponse;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port IN — Caso de uso exclusivo de Dirección General para consultar las horas trabajadas
 * efectivas de todos los usuarios del WMS.
 */
public interface GetUserWorkHoursUseCase {

    /**
     * Calcula las horas trabajadas efectivas agrupadas por usuario para una fecha o rango determinado.
     *
     * @param date            Fecha a consultar (por defecto hoy).
     * @param organizationId  Organización objetivo.
     * @param branchId        Sucursal objetivo (opcional).
     * @param roleFilter      Filtro por nombre de rol (opcional).
     * @param principal       Usuario solicitante (debe ser ADMIN o CEO).
     * @return Lista de horas trabajadas consolidadas por usuario.
     */
    List<UserWorkHoursResponse> getWorkHours(
            LocalDate date,
            UUID organizationId,
            UUID branchId,
            String roleFilter,
            Principal principal);
}

