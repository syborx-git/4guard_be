# ADR 003: Autenticacion sin estado con JWT (JJWT)

## Estado
`Aprobado`

## Fecha
2026-01-01

## Contexto
4GUARD WMS expone una API REST consumida por multiples clientes (frontend web, apps moviles,
servicios internos). El sistema requiere autenticacion segura y autorizacion basada en
privilegios granulares. La escalabilidad horizontal requiere que los servidores no mantengan
estado de sesion.

## Decision
Adoptamos **JSON Web Tokens (JWT)** para autenticacion sin estado, implementado con la
libreria **JJWT 0.12.6** (io.jsonwebtoken).

### Flujo de autenticacion

```
Cliente --> POST /auth/login --> AuthController
  --> AuthUseCase (valida credenciales)
  --> JwtService (genera access + refresh token)
  --> Respuesta con tokens

Cliente --> GET /api/** con Authorization: Bearer <token>
  --> JwtAuthenticationFilter (valida token)
  --> SecurityContextHolder.setAuthentication(...)
  --> Controller --> UseCase
```

### Mecanismo de invalidacion de tokens
- Los **refresh tokens** se almacenan en Redis con TTL (ver ADR-004).
- Los **access tokens** revocados se registran en una blacklist en Redis via `TokenBlacklistPort`.
- Al hacer logout, el access token se agrega a la blacklist.

### Autorizacion
- Se usa `@PreAuthorize` con authorities granulares (`SYS_ADMIN`, `BRANCH_MANAGER`, etc.).
- Los authorities se leen del JWT en el filtro de autenticacion.

## Alternativas consideradas

| Alternativa                  | Razon de descarte |
|------------------------------|-------------------|
| Sesiones HTTP (Spring Session) | Requiere estado en servidor; rompe la escalabilidad horizontal |
| OAuth2 / OpenID Connect      | Overhead de infraestructura innecesario para el alcance actual; puede adoptarse en el futuro |
| API Keys estaticas           | No proveen informacion de identidad ni expiracion automatica |

## Consecuencias

### Positivas
- API completamente stateless; escala horizontalmente sin sticky sessions.
- Tokens auto-expirados reducen la ventana de ataque.
- Claims del JWT transportan informacion del usuario sin round-trip a BD.
- Blacklist en Redis permite revocacion inmediata de tokens comprometidos.

### Negativas / Compromisos (Trade-offs)
- Los tokens JWT no pueden invalidarse antes de su expiracion sin infraestructura adicional (Redis).
- El tamano del token crece con cada claim adicional.
- La clave secreta de firma debe rotarse periodicamente y gestionarse de forma segura.

## Referencias
- ADR-004: Cache con Redis (TokenBlacklist)
- JJWT: https://github.com/jwtk/jjwt
- Spring Security: https://spring.io/projects/spring-security
