# SDD: Recepción de Mercancía (Inbound F01) — Backend

> **Módulo:** Operación y Logística → Movimientos de Almacén → Recepción de Mercancía  
> **HU:** HU-150 — Recepción de Mercancía F01  
> **Versión:** 1.0.0  
> **Estado:** 🏁 IMPLEMENTADO / LISTO PARA PR  
> **Fecha:** 2026-08-19  
> **Patrón:** Hexagonal Architecture + Spec-Driven Development (SDD)  

---

## 1. Descripción del Módulo

El submódulo **Recepción de Mercancía (F01)** controla el flujo transaccional de ingreso de mercancías al almacén:
1. **Caseta de Seguridad (Check-In):** Registro inicial del arribo de transporte, operador, placas, remisión, rampa y sellos de seguridad. Genera el folio consecutivo con estatus `REGISTERED`.
2. **Andén de Descarga:** Captura de parámetros de lote (lote, elaboración, caducidad, SKU, proveedor, piezas por tarima, tipo de tarima y ubicación sugerida).
3. **Escáner de UAs:** Registro unitario o por lote de códigos de tarima (SSCC/UA) con validación de unicidad.
4. **Cierre y Autorización con Doble Factor:** Autorización de Líder/Supervisor de Almacén (`/complete`) que impacta `wms.inventory_items` (estado DISPONIBLE) y genera movimientos `ENTRY` en `wms.inventory_movements`.
5. **Cancelación Extraordinaria:** Cancelación por Administrador con motivo obligatorio y credenciales de seguridad.
6. **Auditoría Integral:** Trazabilidad de cada cambio (`RECEPCION_CREADA`, `RECEPCION_ACTUALIZADA`, `TARIMA_EDITADA`, `RECEPCION_COMPLETADA`, `RECEPCION_CANCELADA`, `REMISION_MODIFICADA`).

---

## 2. Modelo de Base de Datos

### Tabla: `wms.warehouse_receptions`

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL | Identificador único |
| `organization_id` | UUID | FK `wms.organizations.id`, NOT NULL | Organización |
| `branch_id` | UUID | FK `wms.branches.id`, NOT NULL | Sucursal |
| `folio` | VARCHAR(30) | NOT NULL, UNIQUE | Consecutivo numérico (ej. 26510) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `REGISTERED` | `REGISTERED`, `COMPLETED`, `CANCELLED` |
| `carrier_id` | UUID | FK `wms.carriers.id`, NULLABLE | Transportista |
| `client_id` | UUID | FK `wms.clients.id`, NOT NULL | Cliente |
| `ramp_id` | UUID | FK `wms.locations.id`, NULLABLE | Rampa asignada |
| `forklift_operator_id` | UUID | FK `wms.forklift_operators.id`, NULLABLE | Montacarguista responsable |
| `doc_number` | VARCHAR(60) | NOT NULL | No. de remisión / documento |
| `doc_date` | DATE | NOT NULL | Fecha del documento |
| `reception_time` | TIME | NOT NULL | Hora de recepción |
| `driver_name` | VARCHAR(150) | NOT NULL | Nombre del chofer |
| `tractor_plates` | VARCHAR(20) | NOT NULL | Placas tractocamión |
| `box_plates` | VARCHAR(20) | NOT NULL | Placas caja |
| `sku_id` | UUID | FK `wms.products_sku.id`, NULLABLE | SKU recibido |
| `supplier_id` | UUID | FK `wms.suppliers.id`, NULLABLE | Proveedor |
| `lot_number` | VARCHAR(50) | NULLABLE | Lote de fabricación |
| `elaboration_date` | DATE | NULLABLE | Fecha de elaboración |
| `expiration_date` | DATE | NULLABLE | Fecha de caducidad |
| `pieces_per_pallet` | NUMERIC(10,2) | DEFAULT 0 | Piezas estándar por tarima |
| `pallet_type` | VARCHAR(30) | DEFAULT `MADERA_ESTANDAR` | Tipo de tarima |
| `storage_location_id` | UUID | FK `wms.locations.id`, NULLABLE | Ubicación de almacenaje |
| `observations` | TEXT | NULLABLE | Observaciones generales |
| `completed_at` | TIMESTAMPTZ | NULLABLE | Fecha y hora de cierre |
| `leader_authorized_by` | VARCHAR(100) | NULLABLE | Nombre del líder autorizador |
| `cancelled_at` | TIMESTAMPTZ | NULLABLE | Fecha de cancelación |
| `cancellation_reason` | TEXT | NULLABLE | Motivo de cancelación |
| `cancelled_by` | VARCHAR(100) | NULLABLE | Administrador que canceló |
| `version` | BIGINT | NOT NULL, DEFAULT 1 | Optimistic locking |
| `created_at` / `updated_at` | TIMESTAMPTZ | AUTO | Timestamps |
| `created_by` / `updated_by` | VARCHAR(36) | NULLABLE | User audit |

### Tabla: `wms.warehouse_reception_pallets`

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL | Identificador único de tarima |
| `reception_id` | UUID | FK `wms.warehouse_receptions.id`, NOT NULL | Recepción padre |
| `pallet_number` | INT | NOT NULL, DEFAULT 1 | Número consecutivo de tarima (1, 2, 3...) |
| `pallet_code` | VARCHAR(50) | NOT NULL, UNIQUE(reception_id, pallet_code) | Código de barras UA / SSCC |
| `sku_id` | UUID | FK `wms.products_sku.id`, NOT NULL | SKU de la tarima |
| `supplier_id` | UUID | FK `wms.suppliers.id`, NULLABLE | Proveedor |
| `pieces` | NUMERIC(10,2) | NOT NULL, DEFAULT 0 | Piezas contenidas |
| `pallet_type` | VARCHAR(30) | NOT NULL, DEFAULT `MADERA_ESTANDAR` | Tipo de tarima |
| `observations` | TEXT | NULLABLE | Observaciones por tarima |
| `inventory_item_id` | UUID | FK `wms.inventory_items.id`, NULLABLE | Enlace al inventario tras cierre |

---

## 3. Endpoints REST

**Base path:** `/api/v1/warehouse-receptions`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/check-in` | `WAREHOUSE_MOVEMENTS_CREATE` | Registro de caseta (crea recepción en estado `REGISTERED`) |
| `PUT` | `/{id}/parameters` | `WAREHOUSE_MOVEMENTS_UPDATE` | Actualizar parámetros de descarga en andén |
| `GET` | `/{id}` | `WAREHOUSE_MOVEMENTS_READ` | Detalle completo de recepción y sus tarimas |
| `GET` | `/` | `WAREHOUSE_MOVEMENTS_READ` | Listado filtrado por organización, estatus y búsqueda |
| `POST` | `/{id}/pallets` | `WAREHOUSE_MOVEMENTS_UPDATE` | Agregar tarimas / UAs escaneadas |
| `PUT` | `/{id}/pallets/{palletId}` | `WAREHOUSE_MOVEMENTS_UPDATE` | Editar tarima individual (piezas, tipo, observaciones) |
| `DELETE` | `/{id}/pallets/{palletId}` | `WAREHOUSE_MOVEMENTS_UPDATE` | Eliminar tarima de recepción abierta |
| `POST` | `/{id}/complete` | `WAREHOUSE_MOVEMENTS_AUTHORIZE` | Cierre formal F01 con credenciales de Líder |
| `POST` | `/{id}/cancel` | `WAREHOUSE_MOVEMENTS_CANCEL` | Cancelación con credenciales Admin y motivo obligatorio |
| `PUT` | `/{id}/change-remision` | `WAREHOUSE_MOVEMENTS_UPDATE` | Modificar No. Remisión con justificación |
| `GET` | `/{id}/audit` | `WAREHOUSE_MOVEMENTS_READ` | Consultar línea de tiempo de auditoría |
