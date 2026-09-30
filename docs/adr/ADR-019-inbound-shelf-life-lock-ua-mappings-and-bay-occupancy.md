# ADR-019: Candado de Calidad de Vida Útil (365 Días), Mapeo Inmutable de UAs y Algoritmo de Ocupación de Bahías (22 Pallets)

- **Estado:** Aceptado
- **Fecha:** 2026-09-23
- **Autores:** Equipo de Arquitectura e Ingeniería 4GUARD WMS (Frontend, Backend & BD)
- **Módulos Afectados:**
  - `4guard_be/src/main/java/com/fourguard/wms/application/usecase/WarehouseReceptionService.java`
  - `4guard_be/src/main/java/com/fourguard/wms/application/usecase/LocationService.java`
  - `4guard_be/src/main/resources/db/migration/V28__reception_shelf_life_ua_mapping_and_audit.sql`
  - `wms.warehouse_receptions`, `wms.warehouse_reception_pallets`, `wms.ua_mappings`, `wms.inventory_audit_log`
  - `apps/admin-console/src/app/features/receiving`
  - `apps/admin-console/src/app/features/warehouse-movements`
  - `apps/rf-terminal` (PWA de Montacarguistas)

---

## 1. Contexto y Justificación de Negocio

En la operación de almacenamiento y distribución 3PL de 4Guard WMS (HU-150 / HU-151):
1. **Política Estricta de Calidad y Vida Útil:** No se permite el ingreso a planta ni la descarga física de ningún lote cuya vida útil restante sea inferior a **365 días (1 año calendario)** respecto a la fecha actual.
2. **Momento de Validación:** El sistema opera por bloques y etapas desacopladas (ADR-017). La Caseta de Seguridad realiza el pre-registro perimetral (`REGISTERED`), pero **la captura formal de lote y caducidad ocurre en el Módulo de Recepción al asignar la rampa y completar los datos del folio**. Es en ese instante donde debe detonarse el candado de rechazo preventivo antes de autorizar la descarga física.
3. **Doble Identidad de UAs (Proveedor vs. 4Guard):** Ciertas cuentas y tarimas requieren re-etiquetado interno con código SSCC GS1-128 de 18 dígitos, pero la normativa fiscal y de auditoría prohíbe sobreescribir la UA original del proveedor.
4. **Cubicaje Estándar de 22 Pallets:** La unidad de transporte y las bahías estándar manejan una capacidad nominal de 22 tarimas (o factor de estiba específico). El sistema debe transparentar el `% de Ocupación` en tiempo real y permitir la anulación manual (**Override Admin**) por parte del supervisor (Pablo).
5. **Trazabilidad Inmutable E2E (Árbol de Vida):** Cada tarima individual conserva su relación con el folio de remisión padre pero genera eventos asíncronos independientes a lo largo de su permanencia en el almacén.

---

## 2. Decisiones de Arquitectura

### 2.1 Candado de Vida Útil en Parámetros de Recepción (Ajuste de 1 Mes a 1 Año)
Al momento de capturar los parámetros de recepción (`PUT /api/v1/warehouse-receptions/{id}/parameters` o al validar la asignación):
$$\text{shelfLifeDays} = \text{ChronoUnit.DAYS.between}(\text{LocalDate.now(ZoneOffset.UTC)}, \text{expirationDate})$$

- **Si $\text{shelfLifeDays} \ge 365$:** Se aprueba la asignación del folio y se permite continuar con la asignación de rampa y descarga en andén.
- **Si $\text{shelfLifeDays} < 365$:** Se bloquea la asignación y se emite el mensaje de validación: *"Rechazo por Política de Vida Útil (< 1 año / 365 días): El lote cuenta con sólo X días restantes. No se permite la descarga física."*

### 2.2 Captura por Bloque de Lotes en Terminal RF (PWA)
- La terminal móvil mantiene el estado **Lote Activo Persistente**.
- Todas las tarimas escaneadas consecutivamente heredan el lote activo sin solicitar confirmaciones emergentes.
- Se incluye un botón de acceso rápido `"Cambiar Lote"` para alternar al siguiente bloque de producto.

### 2.3 Mapeo Inmutable de UAs (`wms.ua_mappings`)
- Se crea la tabla `wms.ua_mappings` con clave foránea a la recepción y tarima, almacenando `supplier_ua_code` y `internal_ua_code` (SSCC).
- El endpoint `POST /api/v1/warehouse-receptions/{id}/relabel-uas` ejecuta el cambio selectivo para las tarimas marcadas por el supervisor en la interfaz web.

### 2.4 Algoritmo de Bahías y Selector Visual con Semáforo
- Capacidad estándar base: **22 pallets**.
- Fórmula de ocupación:
  $$\text{Porcentaje Ocupación} = \left(\frac{\text{Pallets Almacenados}}{22}\right) \times 100$$
- Widget reutilizable `fg-bay-occupancy-selector` desplegado en **Recepción** y en **Transferencias / Reubicaciones**.
- Permiso de **Override Admin** para `ROLE_ADMIN` / `ROLE_SUPERVISOR` con registro en bitácora de auditoría.

### 2.5 Bitácora Inmutable por Tarima (`wms.inventory_audit_log`)
- Registra cada hito de vida de la tarima: recepción, re-etiquetado, putaway, transferencias internas, despachos o destrucción.
- Soporta la reconstrucción visual del Árbol de Vida de la Remisión.

---

## 3. Consecuencias y Beneficios

1. **Cero Mermas por Caducidad:** Se elimina el riesgo de ingresar mercancía no apta para almacenamiento prolongado.
2. **Ergonomía en Andén:** Reducción de más del 50% de toques en la Terminal RF al operar por lotes continuos.
3. **Auditoría Fiscal y de Calidad Inmune:** Capacidad de consultar el histórico de la UA origen del proveedor en cualquier momento.
4. **Optimización de Espacio:** Visibilidad clara del espacio disponible antes de mover tarimas.
