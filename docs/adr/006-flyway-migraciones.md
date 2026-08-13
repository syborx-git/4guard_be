# ADR 006: Migraciones de base de datos con Flyway

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
El esquema de base de datos de 4GUARD WMS evoluciona continuamente a medida que se agregan
nuevas entidades (Suppliers, WarehouseSection, Shift, Carrier, etc.). Se necesita una
estrategia que garantice:
- Reproducibilidad del esquema en cualquier entorno (dev, staging, produccion).
- Trazabilidad de cada cambio de esquema con fecha, autor y descripcion.
- Rollback controlado en caso de fallos de despliegue.
- Compatibilidad con el ciclo de vida de Spring Boot (migraciones al arranque).

## Decision
Adoptamos **Flyway** como herramienta de control de versiones de esquema de base de datos,
integrado con Spring Boot y con soporte explicito para PostgreSQL.

### Convencion de nombres para migraciones
```
V{version}__{descripcion_snake_case}.sql
```
Ejemplos:
- `V1__create_initial_schema.sql`
- `V2__add_suppliers_table.sql`
- `V3__add_warehouse_section_status_column.sql`

Las migraciones viven en `src/main/resources/db/migration/`.

### Configuracion
- `spring.flyway.enabled=true` en todos los perfiles excepto cuando se indica explicitamente.
- En pruebas, se usa H2 en modo de compatibilidad PostgreSQL o Testcontainers.
- Las migraciones son **siempre hacia adelante** (no se usan migraciones de rollback por defecto).

## Alternativas consideradas

| Alternativa             | Razon de descarte |
|-------------------------|-------------------|
| Liquibase               | Mayor complejidad de configuracion (XML/YAML); Flyway es suficiente y mas simple |
| Hibernate `ddl-auto`    | Peligroso en produccion; no provee trazabilidad ni control de cambios |
| Migraciones manuales    | No reproducibles; propenso a inconsistencias entre entornos |

## Consecuencias

### Positivas
- Esquema reproducible y versionado en todos los entornos.
- El equipo puede revisar cambios de esquema en PRs (archivos SQL versionados).
- Deteccion automatica de migraciones fuera de orden o con checksum invalido.
- Integracion transparente con Spring Boot: migraciones se ejecutan al inicio de la app.

### Negativas / Compromisos (Trade-offs)
- Los archivos SQL de migracion son inmutables una vez aplicados en produccion.
- Errores en migraciones pueden dejar la BD en estado parcial (requiere intervencion manual).
- No se puede usar `hibernate.ddl-auto=create` en ningun entorno (conflicto con Flyway).

## Referencias
- ADR-002: Persistencia con Spring Data JPA
- Flyway: https://flywaydb.org/
- spring-boot-starter-flyway + flyway-database-postgresql: pom.xml lineas 78-85
