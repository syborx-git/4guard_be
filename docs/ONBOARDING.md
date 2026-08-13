# Onboarding ADR & SDD — 4GUARD WMS
## Guia rapida para el equipo (30 min)

---

## Que es un ADR y para que sirve?

Un **Architecture Decision Record (ADR)** documenta UNA decision de arquitectura:
- **Por que** se tomo la decision (contexto).
- **Que** se decidio (la decision en si).
- **Que impacto tiene** (consecuencias positivas y negativas).

> **Regla de oro**: Si la decision afecta a mas de un desarrollador o mas de un sprint,
> necesita un ADR.

---

## Que es el SDD y para que sirve?

El **System Design Document (SDD)** es la **vision general del sistema** como un todo:
- Arquitectura y componentes.
- Flujos de datos criticos.
- Tecnologias usadas.
- Reglas de negocio criticas.
- Estrategia de pruebas y despliegue.

> El SDD es un **living document**: se actualiza cada vez que el sistema cambia.

---

## Como crear un ADR (Windows PowerShell)

```powershell
# Desde la raiz del proyecto:
.\scripts\new-adr.ps1 -Title "Titulo de tu decision"

# Con estado personalizado:
.\scripts\new-adr.ps1 -Title "Migracion a Java 21" -Status "Propuesto"
```

El script automaticamente:
- Asigna el siguiente numero disponible.
- Convierte el titulo a kebab-case.
- Llena la fecha de hoy.
- Crea el archivo basado en la plantilla.

---

## Estructura de un ADR (ejemplos reales del proyecto)

```markdown
# ADR 003: Autenticacion sin estado con JWT (JJWT)

## Estado
Aprobado

## Fecha
2026-01-01

## Contexto
4GUARD WMS expone una API REST consumida por multiples clientes...

## Decision
Adoptamos JSON Web Tokens (JWT) para autenticacion sin estado...

## Alternativas consideradas
| Alternativa | Razon de descarte |
|-------------|-------------------|
| Sesiones HTTP | Rompe la escalabilidad horizontal |

## Consecuencias
### Positivas
- API completamente stateless...

### Negativas
- Requiere Redis para blacklist de tokens...
```

---

## Cuando DEBO crear un ADR?

| Situacion | ADR requerido? |
|-----------|----------------|
| Agregar una nueva libreria de terceros | SI |
| Cambiar el patron de autenticacion | SI |
| Agregar un nuevo endpoint REST | NO |
| Cambiar la estrategia de cache | SI |
| Refactorizar el nombre de un paquete | NO |
| Cambiar de base de datos | SI |
| Agregar un campo nuevo a un DTO | NO |
| Adoptar un nuevo patron de diseño | SI |

---

## ADRs existentes en el proyecto

| # | Decision tomada |
|---|-----------------|
| [ADR-001](docs/adr/001-arquitectura-hexagonal.md) | Por que usamos Arquitectura Hexagonal |
| [ADR-002](docs/adr/002-spring-data-jpa.md) | Por que usamos Spring Data JPA con PostgreSQL |
| [ADR-003](docs/adr/003-jwt-autenticacion.md) | Por que usamos JWT sin estado |
| [ADR-004](docs/adr/004-redis-cache.md) | Por que usamos Redis para cache |
| [ADR-005](docs/adr/005-mapstruct-mapeo.md) | Por que usamos MapStruct para mapeo |
| [ADR-006](docs/adr/006-flyway-migraciones.md) | Por que usamos Flyway para migraciones |

---

## Proceso en Pull Requests

```
Tu tarea                           Tu PR incluye
────────────────────────────────   ─────────────────────────────────
Nueva tecnologia adoptada    →     ADR con estado Propuesto
Nueva funcion importante     →     Codigo + Pruebas + SDD actualizado (si aplica)
Cambio arquitectonico        →     ADR + SDD actualizado
Fix de bug                   →     Solo codigo y prueba (sin ADR)
```

---

## SDD — Como leerlo

El SDD esta en `docs/sdd/index.md`. Leelo cuando:
- Eres nuevo en el proyecto (empieza por aqui).
- Necesitas entender como se conectan los componentes.
- Vas a disenar un nuevo modulo.
- Necesitas saber que tecnologias usa el proyecto y sus versiones.

---

## Referencia rapida de comandos

```powershell
# Crear nuevo ADR
.\scripts\new-adr.ps1 -Title "Mi decision"

# Ver ADRs existentes
Get-ChildItem docs\adr\*.md | Select-Object Name

# Ver el SDD
code docs\sdd\index.md

# Ver la guia completa de contribucion
code CONTRIBUTING.md
```

---

## Flujo completo en 5 pasos

```
1. Identificas una decision arquitectonica
         ↓
2. Ejecutas: .\scripts\new-adr.ps1 -Title "Tu decision"
         ↓
3. Editas el ADR generado (contexto, decision, consecuencias)
         ↓
4. Si el sistema cambia: actualizas docs/sdd/index.md
         ↓
5. Abres el PR con el codigo + ADR + SDD actualizado
```

---

> Dudas? Contacta al arquitecto del proyecto o revisa `CONTRIBUTING.md`.
