# SDD — Backend: Gestión de Clientes, Contactos Corporativos y Destinos Físicos

> **Módulo:** `clients`
> **Repositorio:** `4guard_be` · **Rama:** `edj-cliente-destino-be`
> **Arquitectura:** Hexagonal (Ports & Adapters)
> **Versión:** 2.0 (Multi-Contacto & Multi-Bodega)
> **Estado:** 🟢 Implementado

---

## 1. Objetivo

Proveer la capa de persistencia, validaciones de negocio y API REST para la gestión completa de **Clientes Depositantes / Owners 3PL** en el WMS 4GUARD, con soporte para:

1. **Datos Corporativos y Fiscales:** Razón Social, RFC/Tax ID, Dirección Fiscal, Teléfono, Email y Contraseña Portal.
2. **Matriz Dinámica de Contactos Corporativos (1:N):** Múltiples contactos por cliente con roles: Logística, Finanzas, Calidad, Compras.
3. **Direcciones Físicas de Destino / Bodegas / Plantas (1:N):** Ship-to Locations para el módulo de Despacho/Outbound.
4. **Auditoría Transaccional:** Bitácora detallada de cambios antes/después de cada operación.

---

## 2. Esquema de Base de Datos (PostgreSQL / schema `wms`)

### Migración: `V5__client_destinations_and_contacts.sql`

```sql
-- TABLA wms.clients (MODIFICADA en V5)
ALTER TABLE wms.clients
    ADD COLUMN address             VARCHAR(300),
    ADD COLUMN phone               VARCHAR(50),
    ADD COLUMN email               VARCHAR(150),
    ADD COLUMN web_portal_password VARCHAR(255);

-- TABLA wms.client_contacts (NUEVA en V5)
CREATE TABLE wms.client_contacts (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id   UUID        NOT NULL REFERENCES wms.clients(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    phone       VARCHAR(50)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    is_primary  BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW(),
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36)
);

-- TABLA wms.client_destinations (NUEVA en V5)
CREATE TABLE wms.client_destinations (
    id                UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id         UUID        NOT NULL REFERENCES wms.clients(id) ON DELETE CASCADE,
    destination_code  VARCHAR(50)  NOT NULL,
    plant_name        VARCHAR(200) NOT NULL,
    full_address      VARCHAR(500) NOT NULL,
    contact_person    VARCHAR(150) NOT NULL,
    phone             VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  DEFAULT 'ACTIVO',
    notes             TEXT,
    version           BIGINT       DEFAULT 1,
    created_at        TIMESTAMPTZ  DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  DEFAULT NOW(),
    created_by        VARCHAR(36),
    updated_by        VARCHAR(36),
    CONSTRAINT uk_client_destination_code UNIQUE (client_id, destination_code)
);
```

### Relación Entidad-Relación

```
organizations (1) ──────< clients (N)
clients       (1) ──────< client_contacts (N)     [ON DELETE CASCADE]
clients       (1) ──────< client_destinations (N) [ON DELETE CASCADE]
```

---

## 3. Capas de la Arquitectura Hexagonal

### 3.1 Domain Layer

| Clase | Descripción |
|---|---|
| `domain/model/Client.java` | Modelo de dominio con todos los campos y listas de contactos/destinos |
| `domain/model/ClientContact.java` | Modelo de dominio para contacto corporativo |
| `domain/model/ClientDestination.java` | Modelo de dominio para destino físico (Ship-to) |
| `domain/ports/in/ClientUseCase.java` | Puerto de entrada con CRUD + toggle + destinos granulares |
| `domain/ports/out/ClientRepositoryPort.java` | Puerto de salida con validaciones de unicidad |
| `domain/ports/out/ClientDestinationRepositoryPort.java` | Puerto de salida para operaciones de destinos |

### 3.2 Application Layer

| Clase | Descripción |
|---|---|
| `application/dto/common/ClientContactDto.java` | DTO reutilizable en request y response de contactos |
| `application/dto/common/PhysicalDestinationDto.java` | DTO reutilizable en request y response de destinos |
| `application/dto/request/CreateClientRequest.java` | DTO de creación con validaciones `@NotBlank`, `@Email`, `@Valid` en listas |
| `application/dto/request/UpdateClientRequest.java` | DTO de actualización con campos opcionales |
| `application/dto/response/ClientResponse.java` | Respuesta completa con `contacts[]` y `destinations[]` |
| `application/mapper/ClientMapper.java` | MapStruct — mapeo entidades↔DTOs con sincronización de colecciones |
| `application/usecase/ClientService.java` | Implementación de lógica de negocio con sincronización inteligente |

### 3.3 Infrastructure Layer

| Clase | Descripción |
|---|---|
| `persistence/entity/ClientEntity.java` | JPA con `@OneToMany(cascade = ALL, orphanRemoval = true)` |
| `persistence/entity/ClientContactEntity.java` | JPA para `wms.client_contacts` |
| `persistence/entity/ClientDestinationEntity.java` | JPA para `wms.client_destinations` con `@Version` |
| `persistence/repository/ClientJpaRepository.java` | Spring Data JPA con query methods de unicidad |
| `persistence/repository/ClientContactJpaRepository.java` | Spring Data JPA para contactos |
| `persistence/repository/ClientDestinationJpaRepository.java` | Spring Data JPA para destinos |
| `persistence/adapter/ClientPersistenceAdapter.java` | Adaptador que implementa `ClientRepositoryPort` |
| `persistence/adapter/ClientDestinationPersistenceAdapter.java` | Adaptador que implementa `ClientDestinationRepositoryPort` |

### 3.4 Presentation Layer

| Clase | Descripción |
|---|---|
| `presentation/controller/ClientController.java` | REST Controller con 11 endpoints documentados en Swagger |

---

## 4. Endpoints REST (OpenAPI)

| Método | Path | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/clients` | `CLIENTS_CREATE` | Crear cliente con contactos y destinos |
| `PUT` | `/api/v1/clients` | `CLIENTS_UPDATE` | Actualizar cliente (sincronización inteligente) |
| `GET` | `/api/v1/clients/{id}` | `CLIENTS_READ` | Obtener cliente por UUID |
| `GET` | `/api/v1/clients?organizationId=` | `CLIENTS_READ` | Listar clientes por organización |
| `DELETE` | `/api/v1/clients/{id}` | `CLIENTS_DELETE` | Eliminar cliente (hard delete) |
| `PATCH` | `/api/v1/clients/{id}/status` | `CLIENTS_UPDATE` | Toggle ACTIVE ↔ INACTIVE |
| `GET` | `/api/v1/clients/{id}/audit` | `CLIENTS_READ` | Historial de auditoría |
| `GET` | `/api/v1/clients/{id}/destinations` | `CLIENTS_READ` | Listar destinos del cliente |
| `POST` | `/api/v1/clients/{id}/destinations` | `CLIENTS_UPDATE` | Agregar destino físico |
| `PUT` | `/api/v1/clients/{id}/destinations/{destId}` | `CLIENTS_UPDATE` | Actualizar destino físico |
| `DELETE` | `/api/v1/clients/{id}/destinations/{destId}` | `CLIENTS_UPDATE` | Eliminar destino físico |

---

## 5. Reglas de Negocio Implementadas

| ID | Regla | Implementación |
|---|---|---|
| RN-CLI-001 | External ID único por organización (excepto RFCs genéricos) | `validateExternalIdUniqueness()` en `ClientService` |
| RN-CLI-002 | Tax ID / RFC único por organización (excepto XAXX010101000 / XEXX010101000) | `validateTaxIdUniqueness()` en `ClientService` |
| RN-CLI-003 | Código de destino (`destinationCode`) único por cliente | `validateDestinationCodeForNew()` + constraint `UNIQUE(client_id, destination_code)` |
| RN-CLI-004 | Sincronización inteligente de contactos (add/update/delete en misma operación) | `syncContacts()` con `orphanRemoval = true` |
| RN-CLI-005 | Sincronización inteligente de destinos | `syncDestinations()` con `orphanRemoval = true` |
| RN-CLI-006 | Baja Lógica — clientes con historial no se borran físicamente | `PATCH /{id}/status` (`toggleClientStatus()`) |
| RN-CLI-007 | Destinos INACTIVOS excluidos del módulo Outbound | Filtrado por `status = 'ACTIVO'` en `ClientDestinationJpaRepository` |

---

## 6. Auditoría

Las acciones auditadas son:

| Acción | Descripción |
|---|---|
| `CLIENT_CREATED` | Alta de nuevo cliente |
| `CLIENT_UPDATED` | Modificación de datos del cliente |
| `CLIENT_DELETED` | Eliminación del cliente |
| `CLIENT_STATUS_CHANGED` | Cambio de estado ACTIVE ↔ INACTIVE |
| `CLIENT_DESTINATION_ADDED` | Nueva bodega/planta vinculada |
| `CLIENT_DESTINATION_UPDATED` | Actualización de destino físico |
| `CLIENT_DESTINATION_DELETED` | Eliminación de destino físico |

Todos los eventos se registran en `wms.audit_logs` con estado `beforeState` y `afterState` como JSON.

---

## 7. Cobertura de Pruebas

**Clase:** `ClientServiceTest.java` (9 casos)

| Caso | Resultado esperado |
|---|---|
| Crear cliente con datos válidos | `ClientResponse` con id y status ACTIVE |
| Crear con External ID duplicado | `ValidationException` |
| Crear con RFC genérico | Éxito sin validar unicidad de Tax ID |
| Actualizar cliente con datos válidos | `ClientResponse` actualizado |
| Eliminar cliente existente | Llamada a `deleteById` verificada |
| Obtener cliente con UUID inválido | `EntityNotFoundException` |
| Toggle estado ACTIVE → INACTIVE | `save()` con status=INACTIVE |
| Obtener logs de auditoría | Lista con acción `CLIENT_CREATED` |
| Agregar destino con código único | `PhysicalDestinationDto` guardado |
| Agregar destino con código duplicado | `ValidationException` |
