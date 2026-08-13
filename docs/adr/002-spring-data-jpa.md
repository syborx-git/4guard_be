# ADR 002: Persistencia con Spring Data JPA y PostgreSQL

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
El sistema 4GUARD WMS maneja entidades relacionales complejas: Organization, Branch,
User, Role, Permission, Supplier, WarehouseSection, Shift, etc. Se necesita un mecanismo
de persistencia robusto, de alta productividad y compatible con la arquitectura hexagonal
(es decir, que los detalles JPA queden confinados a la capa de infraestructura).

## Decision
Adoptamos **Spring Data JPA con Hibernate 6.x** como ORM y **PostgreSQL** como base
de datos relacional.

- Las entidades JPA (`@Entity`) viven exclusivamente en `infrastructure.persistence.entity`.
- Los repositorios Spring Data (`JpaRepository`) viven en `infrastructure.persistence.repository`.
- Los adaptadores de persistencia (`*PersistenceAdapter`) implementan los output ports
  del dominio y actuan como puente entre JPA y el dominio.
- Se usa **Hypersistence Utils 3.9.0** para soporte de tipos JSONB de PostgreSQL en Hibernate 6.x.
- Las migraciones de esquema son gestionadas por **Flyway** (ver ADR-006).

### Patron de acceso a datos

```
Domain Port (out) <-- PersistenceAdapter <-- JpaRepository <-- Entity <-- PostgreSQL
```

## Alternativas consideradas

| Alternativa       | Razon de descarte |
|-------------------|-------------------|
| MyBatis           | Requiere SQL manual; menor productividad para el CRUD de este proyecto |
| JOOQ              | Excelente para SQL complejo; overhead innecesario para el alcance actual |
| MongoDB           | No relacional; el dominio tiene relaciones complejas que se benefician de SQL |
| JDBC puro         | Productividad muy baja; innecesario en este contexto |

## Consecuencias

### Positivas
- Alta productividad con Spring Data (metodos derivados, paginacion, sort incluidos).
- Soporte nativo de JSONB via Hypersistence Utils para columnas de configuracion flexible.
- Integracion nativa con Spring Boot (autoconfiguracion, transacciones, health checks).
- Migracion controlada de esquema via Flyway (sin surpresas en produccion).

### Negativas / Compromisos (Trade-offs)
- Hibernate puede generar N+1 queries si no se controlan las relaciones con `@EntityGraph` o `JOIN FETCH`.
- Las entidades JPA no pueden ser las mismas que los modelos de dominio (separation of concerns).
- Requiere disciplina para no filtrar anotaciones JPA fuera de la capa de infraestructura.

## Referencias
- ADR-006: Migraciones con Flyway
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Hypersistence Utils: https://github.com/vladmihalcea/hypersistence-utils
