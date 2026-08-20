# SDD: Salidas de Almacén (Outbound Despachos F03) — Backend

> **Módulo:** Operación y Logística → Movimientos de Almacén → Salidas de Almacén  
> **HU:** HU-152 — Salidas de Almacén y Despacho Outbound  
> **Versión:** 1.0.0  
> **Estado:** 🏁 IMPLEMENTADO / LISTO PARA PR  
> **Fecha:** 2026-08-19  
> **Patrón:** Hexagonal Architecture + Spec-Driven Development (SDD)  

---

## 1. Descripción del Módulo

El submódulo **Salidas de Almacén (Outbound F03)** gestiona el egreso formal y despacho de mercancía:
1. **Selección FIFO/FEFO:** Consulta de lotes disponibles agrupados por remisión, lote y fecha de caducidad con indicación del lote prioritario por antigüedad.
2. **Registro de Despacho:** Captura de cliente, destino/planta, transporte, tipo de vehículo, placas, chofer, sellos de seguridad y remisión.
3. **Impacto en Inventario:**
   - Cambia el estado de `wms.inventory_items` a `DISPATCHED` (50).
   - Genera movimientos de inventario en `wms.inventory_movements` con tipo `EXIT`.
4. **Cancelación:**
   - Revoca el despacho con autorización de Administrador y restaura las tarimas al estado `AVAILABLE` (30).

---

## 2. Modelo de Base de Datos

### Tabla: `wms.warehouse_outbounds`

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL | Identificador único |
| `organization_id` | UUID | FK `wms.organizations.id`, NOT NULL | Organización |
| `branch_id` | UUID | FK `wms.branches.id`, NOT NULL | Sucursal |
| `folio` | VARCHAR(30) | NOT NULL, UNIQUE | Consecutivo formato `SAL-YYYY-XXXXXX` |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `COMPLETED` | `DRAFT`, `CONFIRMED`, `COMPLETED`, `CANCELLED` |
| `client_id` | UUID | FK `wms.clients.id`, NOT NULL | Cliente |
| `destination_id` | UUID | FK `wms.client_destinations.id`, NULLABLE | Destino/Planta |
| `destination_name` | VARCHAR(200) | NULLABLE | Nombre de destino |
| `destination_address` | TEXT | NULLABLE | Dirección de entrega |
| `carrier_id` | UUID | FK `wms.carriers.id`, NULLABLE | Transportista |
| `transport_type` | VARCHAR(30) | NOT NULL, DEFAULT `TRAILER` | `CAMION`, `TORTON`, `TRAILER` |
| `driver_name` | VARCHAR(150) | NOT NULL | Chofer |
| `economic_number` | VARCHAR(30) | NULLABLE | No. Económico |
| `tractor_plates` | VARCHAR(20) | NOT NULL | Placas tractocamión |
| `box_plates` | VARCHAR(20) | NOT NULL | Placas caja |
| `seal_number` | VARCHAR(50) | NOT NULL | No. de sello / cincho de seguridad |
| `remision_no` | VARCHAR(60) | NOT NULL | No. de remisión de salida |
| `total_pallets` | INT | NOT NULL, DEFAULT 0 | Total tarimas despachadas |
| `total_pieces` | NUMERIC(12,2) | NOT NULL, DEFAULT 0 | Total piezas |
| `distinct_skus` | INT | NOT NULL, DEFAULT 0 | SKUs distintos |
| `cancelled_at` | TIMESTAMPTZ | NULLABLE | Fecha de cancelación |
| `cancellation_reason` | TEXT | NULLABLE | Motivo de cancelación |
| `cancelled_by` | VARCHAR(100) | NULLABLE | Administrador que canceló |
| `version` | BIGINT | NOT NULL, DEFAULT 1 | Optimistic locking |
| `created_at` / `updated_at` | TIMESTAMPTZ | AUTO | Timestamps |

---

## 3. Endpoints REST

**Base path:** `/api/v1/warehouse-outbounds`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/` | `WAREHOUSE_MOVEMENTS_CREATE` | Registrar salida / despacho outbound |
| `GET` | `/{id}` | `WAREHOUSE_MOVEMENTS_READ` | Detalle completo de salida |
| `GET` | `/` | `WAREHOUSE_MOVEMENTS_READ` | Listar salidas con filtros |
| `POST` | `/{id}/cancel` | `WAREHOUSE_MOVEMENTS_CANCEL` | Cancelar salida |
| `GET` | `/inventory-batches` | `WAREHOUSE_MOVEMENTS_READ` | Consultar lotes disponibles con sugerencia FIFO/FEFO |
| `GET` | `/{id}/audit` | `WAREHOUSE_MOVEMENTS_READ` | Consultar auditoría |
