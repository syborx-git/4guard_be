# Architecture Decision Records (ADR)

Este directorio contiene los registros formales de decisiones arquitectónicas del proyecto **4GUARD WMS Backend**.

---

## ¿Qué es un ADR?

Un **Architecture Decision Record (ADR)** es un documento corto que captura:
- El **contexto** en que se tomó una decisión de arquitectura.
- La **decisión** tomada.
- Las **consecuencias** esperadas (positivas y negativas).

Los ADRs son inmutables: una vez **Aprobado**, no se modifica. Si la decisión cambia, se crea un **nuevo ADR** que lo supercede.

---

## Convención de nombres

```
NNN-titulo-kebab-case.md
```

Ejemplos:
- `001-arquitectura-hexagonal.md`
- `002-spring-data-jpa.md`
- `003-jwt-autenticacion.md`

---

## Estados posibles

| Estado       | Descripción |
|--------------|-------------|
| `Propuesto`  | En discusión, aún no aprobado |
| `Aprobado`   | Decisión tomada y vigente |
| `Rechazado`  | Se evaluó pero no se adoptó |
| `Superseded` | Reemplazado por otro ADR (indicar cuál) |

---

## Proceso de revisión

1. Crea un ADR con estado **Propuesto** basándote en `template.md`.
2. Abre un **Pull Request** con el ADR y el código relacionado.
3. El equipo revisa el ADR durante el code review.
4. Una vez aprobado el PR, cambia el estado a **Aprobado**.
5. Referencia el ADR en código con el comentario `// ADR-NNN`.

---

## Indice de ADRs

| #   | Titulo                                                        | Estado    |
|-----|---------------------------------------------------------------|-----------|
| [001](001-arquitectura-hexagonal.md)   | Uso de Arquitectura Hexagonal (Ports and Adapters) | Aprobado |
| [002](002-spring-data-jpa.md)          | Persistencia con Spring Data JPA y PostgreSQL      | Aprobado |
| [003](003-jwt-autenticacion.md)        | Autenticacion sin estado con JWT (JJWT)            | Aprobado |
| [004](004-redis-cache.md)              | Cache distribuida con Redis                        | Aprobado |
| [005](005-mapstruct-mapeo.md)          | Mapeo de objetos con MapStruct                     | Aprobado |
| [006](006-flyway-migraciones.md)       | Migraciones de base de datos con Flyway            | Aprobado |

---

## Creacion rapida de un nuevo ADR

### Con adr-tools (Go) — Recomendado

```bash
# Instalar adr-tools (Linux/Mac)
brew install adr-tools

# Inicializar (solo primera vez)
adr init docs/adr

# Crear un nuevo ADR
adr new "Titulo de la decision"
```

### Manual

```bash
# Windows PowerShell
Copy-Item docs\adr\template.md docs\adr\NNN-titulo-kebab.md
```
