# Guia de Contribucion — 4GUARD WMS Backend

Gracias por contribuir al proyecto. Esta guia explica las convenciones y procesos que
deben seguirse para mantener la calidad y trazabilidad del codigo.

---

## Tabla de Contenido

- [Flujo de trabajo Git](#flujo-de-trabajo-git)
- [Creacion de ADRs](#creacion-de-adrs)
- [Actualizacion del SDD](#actualizacion-del-sdd)
- [Convenciones de codigo](#convenciones-de-codigo)
- [Pruebas](#pruebas)
- [Pull Requests](#pull-requests)

---

## Flujo de trabajo Git

Usamos **Gitflow** simplificado:

```
main           <- produccion estable
develop        <- integracion
feature/XXX    <- nuevas funcionalidades
fix/XXX        <- correcciones de bugs
docs/XXX       <- cambios solo de documentacion
```

### Formato de commits

```
<tipo>(<ambito>): <descripcion corta>

Tipos: feat | fix | docs | refactor | test | chore
Ejemplo: feat(user): add reset password use case
         docs(adr): add ADR-007 for notification system
```

---

## Creacion de ADRs

**Cuando crear un ADR:**
- Adopcion de una nueva tecnologia o libreria.
- Cambio en el patron de arquitectura.
- Decision de diseño significativa que afecte multiples modulos.
- Cambio en la estrategia de seguridad, persistencia o integracion.

**Como crear un ADR:**

### Opcion A — Con adr-tools (recomendado)

```bash
# Instalar adr-tools (Windows via Scoop o WSL)
# WSL/Linux/Mac:
brew install adr-tools

# Inicializar (solo la primera vez en el repo)
adr init docs/adr

# Crear nuevo ADR
adr new "Titulo de la decision"
```

### Opcion B — Manual (Windows PowerShell)

```powershell
# Determinar el siguiente numero
$next = (Get-ChildItem docs\adr\*.md | Where-Object { $_.Name -match '^\d+' } | 
         Sort-Object Name | Select-Object -Last 1).Name.Substring(0,3)
$num = ([int]$next + 1).ToString("000")

# Copiar la plantilla
Copy-Item docs\adr\template.md "docs\adr\$num-titulo-kebab.md"
```

**Reglas:**
1. Estado inicial siempre: `Propuesto`.
2. Incluir el ADR en el mismo PR que el codigo relacionado.
3. Al aprobarse el PR: cambiar el estado a `Aprobado`.
4. Referenciar el ADR en el codigo: `// ADR-NNN`.
5. Agregar el nuevo ADR al indice en `docs/adr/README.md`.

---

## Actualizacion del SDD

El SDD (`docs/sdd/index.md`) es un **living document**. Debe actualizarse cuando:

- Se agrega un nuevo modulo de negocio.
- Cambia la arquitectura de un componente existente.
- Se agrega o elimina una tecnologia del stack.
- Cambian las reglas de negocio criticas.
- Cambia el esquema de despliegue.

### Proceso

1. Editar `docs/sdd/index.md` con los cambios.
2. Actualizar la seccion **Historial de Cambios** al final del documento.
3. Incluir la actualizacion en el mismo PR que el codigo.

---

## Convenciones de codigo

### Arquitectura hexagonal — Reglas obligatorias

| Capa            | Puede importar de...                      | NO puede importar de...          |
|-----------------|-------------------------------------------|----------------------------------|
| `domain`        | Solo Java puro                            | Spring, JPA, Lombok, MapStruct   |
| `application`   | `domain`                                  | `infrastructure`, `presentation` |
| `infrastructure`| `domain`, `application`, Spring, JPA      | `presentation`                   |
| `presentation`  | `domain.ports.in`, `application.dto`, Spring MVC | `infrastructure` directamente |

### Convencion de nombres

| Tipo de clase       | Sufijo / Prefijo           | Ejemplo                        |
|---------------------|----------------------------|--------------------------------|
| Input Port          | `UseCase`                  | `CreateUserUseCase`            |
| Output Port         | `Port`                     | `UserRepositoryPort`           |
| Use Case Impl.      | `*UseCaseImpl` o `*Service`| `UserService`                  |
| Persistence Adapter | `*PersistenceAdapter`      | `UserPersistenceAdapter`       |
| JPA Entity          | `*Entity`                  | `UserEntity`                   |
| JPA Repository      | `*JpaRepository`           | `UserJpaRepository`            |
| DTO Request         | `*Request`                 | `CreateUserRequest`            |
| DTO Response        | `*Response`                | `UserResponse`                 |
| MapStruct Mapper    | `*Mapper`                  | `UserMapper`                   |

---

## Pruebas

Todo codigo nuevo debe incluir pruebas. El objetivo minimo es:

- **Use cases**: 80% de cobertura de ramas.
- **Controllers**: Pruebas de happy path y casos de error principales.

```bash
# Ejecutar pruebas
mvn clean test

# Con reporte (cuando Jacoco este configurado)
mvn clean verify
```

---

## Pull Requests

### Checklist antes de abrir un PR

- [ ] El codigo compila sin errores: `mvn clean package -DskipTests`.
- [ ] Las pruebas pasan: `mvn clean test`.
- [ ] Si la decision es arquitectonica: se crea un ADR con estado `Propuesto`.
- [ ] Si el modulo cambia: el SDD esta actualizado.
- [ ] Los commits siguen el formato convencional.
- [ ] El titulo del PR es descriptivo.

### Plantilla de PR

```
## Que hace este PR?

## ADRs relacionados (si aplica)
- ADR-NNN: ...

## Cambios en el SDD (si aplica)
- Seccion X actualizada porque...

## Como probar
1. ...

## Checklist
- [ ] Tests pasan
- [ ] ADR incluido (si aplica)
- [ ] SDD actualizado (si aplica)
```
