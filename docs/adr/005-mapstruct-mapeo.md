# ADR 005: Mapeo de objetos con MapStruct

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
En la arquitectura hexagonal del proyecto existen tres tipos de objetos que se deben
transformar entre si:
- **Modelos de dominio** (paquete `domain.model`): POJOs puros sin anotaciones de framework.
- **DTOs** (paquete `application.dto`): Requests y Responses expuestos via REST.
- **Entidades JPA** (paquete `infrastructure.persistence.entity`): Mapeadas a PostgreSQL.

Cada caso de uso requiere conversiones en ambas direcciones, lo que genera un volumen
significativo de codigo de mapeo repetitivo.

## Decision
Adoptamos **MapStruct 1.6.3** como framework de mapeo de objetos, procesado en tiempo
de compilacion mediante el annotation processor de Maven.

### Configuracion del annotation processor (pom.xml)
```
Lombok (1.18.36) --> lombok-mapstruct-binding (0.2.0) --> mapstruct-processor (1.6.3)
```
El orden es critico: Lombok debe ejecutarse antes que MapStruct para que los getters/setters
generados sean visibles para MapStruct.

Todos los mappers se exponen como beans Spring con:
```
-Amapstruct.defaultComponentModel=spring
```

### Patron de mapeo por capa
```
DTO <--> Mapper <--> Dominio <--> Mapper <--> Entidad JPA
```
Cada entidad de dominio tiene su propio `*Mapper` (ej. `UserMapper`, `RoleMapper`).

## Alternativas consideradas

| Alternativa         | Razon de descarte |
|---------------------|-------------------|
| ModelMapper         | Refleccion en tiempo de ejecucion; mas lento y propenso a errores silenciosos |
| Dozer               | Abandonado / sin mantenimiento activo |
| Mapeo manual (builders) | Viable pero genera codigo repetitivo y dificil de mantener a escala |
| BeanUtils de Spring | Basado en refleccion; sin tipo seguro en tiempo de compilacion |

## Consecuencias

### Positivas
- Mapeo 100% en tiempo de compilacion: errores detectados antes de ejecutar.
- Codigo generado legible e inspeccionable en `target/generated-sources`.
- Soporte de expresiones, decoradores y logica personalizada cuando es necesario.
- Integracion sin friccion con Lombok y Spring.

### Negativas / Compromisos (Trade-offs)
- Requiere configuracion cuidadosa del orden de annotation processors en Maven.
- Las clases generadas aumentan el tamano del codigo compilado.
- Los mappings complejos (jerarquias, colecciones anidadas) requieren metodos adicionales.

## Referencias
- MapStruct: https://mapstruct.org/
- ADR-001: Arquitectura hexagonal (justifica la necesidad de multiples capas de objetos)
