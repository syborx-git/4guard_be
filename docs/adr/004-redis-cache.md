# ADR 004: Cache distribuida con Redis

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
El sistema requiere:
1. Almacenar refresh tokens con TTL para autenticacion (ver ADR-003).
2. Implementar una blacklist de access tokens revocados.
3. Potencial cache de consultas frecuentes (configuraciones de organizacion, permisos, etc.)
   para reducir carga en PostgreSQL.

Se necesita una solucion que funcione tanto en entorno de un solo nodo como distribuido,
y que sea opcionalmente habilitada segun el entorno (`cache.redis.enabled`).

## Decision
Adoptamos **Redis** como almacen de cache distribuida, integrado via
**Spring Boot Starter Data Redis** con configuracion condicional.

### Usos concretos en el sistema
| Caso de uso                  | Clave Redis                        | TTL      |
|------------------------------|------------------------------------|----------|
| Refresh token activo         | `refresh:<userId>`                 | 7 dias   |
| Blacklist de access tokens   | `blacklist:<jti>`                  | TTL del token |
| Cache de permisos (futuro)   | `perms:<userId>`                   | 15 min   |

### Diseno del adaptador
- `TokenBlacklistPort` (output port en dominio) define el contrato.
- `TokenBlacklistAdapter` (en infrastructure) implementa el port usando RedisTemplate.
- La configuracion de Redis es condicional: si Redis no esta disponible en dev, se usa
  cache en memoria como fallback.

## Alternativas consideradas

| Alternativa       | Razon de descarte |
|-------------------|-------------------|
| Memcached         | No soporta TTL por clave de forma nativa; no tiene soporte de estructuras de datos complejas |
| Hazelcast         | Mayor complejidad de configuracion para el alcance actual |
| Cache en memoria  | No es distribuida; inutil en despliegues multi-instancia |
| Base de datos BD  | Demasiado costoso en latencia para operaciones de cache/token |

## Consecuencias

### Positivas
- TTL automatico elimina entradas expiradas sin jobs de limpieza.
- Soporte nativo de estructuras (String, Hash, Set) para diferentes casos de uso.
- Muy bajo latencia (<1ms en operaciones simples).
- Integracion transparente con Spring Cache (`@Cacheable`, `@CacheEvict`).

### Negativas / Compromisos (Trade-offs)
- Agrega una dependencia de infraestructura adicional (Redis server).
- En entornos de desarrollo sin Redis, requiere fallback o contenedor Docker.
- Datos en Redis son volatiles; no usar como fuente de verdad.

## Referencias
- ADR-003: JWT y blacklist de tokens
- Spring Data Redis: https://spring.io/projects/spring-data-redis
- Redis: https://redis.io/
