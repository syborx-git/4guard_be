# ADR 001: Uso de Arquitectura Hexagonal (Ports and Adapters)

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
El proyecto 4GUARD WMS requiere alta mantenibilidad y testabilidad desde el inicio.
La logica de negocio (gestion de almacen, usuarios, roles, permisos, proveedores, etc.)
debe estar completamente aislada de los detalles de infraestructura (base de datos,
framework web, cache, seguridad).

Los equipos de desarrollo necesitan poder probar la logica de dominio sin levantar
ningun adaptador externo (base de datos, Redis, HTTP).

## Decision
Adoptamos la **Arquitectura Hexagonal (Ports and Adapters)** como patron central del proyecto.

La estructura de paquetes refleja esta decision:

```
com.fourguard.wms
  ├── domain          # Capa pura: modelos, excepciones, puertos (interfaces)
  │   ├── model       # Entidades de dominio sin dependencias externas
  │   ├── ports.in    # Input ports: contratos de casos de uso
  │   └── ports.out   # Output ports: contratos de repositorios/servicios externos
  ├── application     # Implementacion de los casos de uso (orquestacion)
  ├── infrastructure  # Adaptadores de salida: JPA, Redis, JWT, configuraciones
  ├── presentation    # Adaptadores de entrada: REST controllers, filters
  └── shared          # Utilerias transversales sin logica de negocio
```

## Alternativas consideradas

| Alternativa              | Razon de descarte |
|--------------------------|-------------------|
| Arquitectura en capas (N-Tier) | Acopla la logica de negocio a JPA/Spring, dificulta pruebas unitarias puras |
| Clean Architecture pura  | Muy similar; hexagonal es mas familiar al equipo y tiene mejor adopcion en el ecosistema Spring |
| Modular Monolith sin patron | No garantiza el aislamiento requerido de dependencias |

## Consecuencias

### Positivas
- Logica de dominio 100% testeable sin contexto Spring ni base de datos.
- Facilidad para cambiar de proveedor de persistencia (PostgreSQL -> cualquier otro) sin tocar dominio.
- Clara separacion de responsabilidades por capa.
- Onboarding de nuevos desarrolladores guiado por la estructura de paquetes.
- Cada feature se desarrolla de forma vertical y autocontenida.

### Negativas / Compromisos (Trade-offs)
- Mayor cantidad de clases e interfaces (boilerplate inicial mas alto).
- Curva de aprendizaje para desarrolladores acostumbrados a arquitecturas en capas.
- Requiere disciplina del equipo para no violar las fronteras de capa.

## Referencias
- Alistair Cockburn — Hexagonal Architecture (https://alistair.cockburn.us/hexagonal-architecture/)
- Tom Hombergs — Get Your Hands Dirty on Clean Architecture
