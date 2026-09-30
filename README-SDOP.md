🛡️ Manual de Operación SDOP: 4GUARD WMS 3PL
Este documento establece el estándar de desarrollo e integración para el ecosistema 4GUARD WMS 3PL, compuesto por las aplicaciones de frontend en Angular (admin-console y rf-terminal) y el backend en Spring Boot (4guard_be)
.
El proyecto opera bajo la metodología SDOP (Spec-Driven Oracle-Bridge Framework), alcanzando un nivel de madurez técnica pre-migración del 96%
.
📐 Principios de Arquitectura en 4GUARD
Patrón Bridge & Adaptadores Duales (Angular): La capa de presentación (admin-console y rf-terminal) depende exclusivamente de tokens e interfaces abstractas (repository.tokens.ts, license.repository.ts, inventory.repository.ts). Esto permite alternar entre MockAdapter y HttpAdapter mediante Feature Flags sin tocar los componentes visuales
.
Saneamiento Hexagonal (Spring Boot): Los servicios de caso de uso (WarehouseTransferService, WarehouseReceptionService, WarehouseOutboundService) se desacoplan de JpaRepository e interactúan a través de puertos de salida (\*RepositoryPort) y adaptadores de persistencia
.
Oráculo de Comportamiento (Playwright): Se utiliza @playwright/test como oráculo automatizado para ejecutar pruebas E2E de equivalencia funcional y regresión visual (pixel-matching)
.
Puntos de Control Humano (Human-in-the-Loop): Ningún cambio se integra al backend ni a producción sin la revisión y aprobación explícita del Arquitecto en el SDD y en la Suite de Playwright
.
📂 Estructura de Workspace y Gobernanza
4guard-workspace/
├── docs/ <-- SSOT de Gobernanza Centralizada
│ ├── adr/ <-- Catálogo Maestro de 23 Decisiones Arquitectónicas
│ └── sdd/ <-- Catálogo Maestro de 19 Especificaciones SDD
├── .agents/
│ └── skills/ <-- Skills de Agentes para Antigravity IDE
│ ├── sdop-framework/SKILL.md <-- Definición de pilares inmutables
│ └── playwright-oracle/SKILL.md <-- Guía de operación del oráculo
├── 4Guard_FE_UI/ <-- Frontend Angular
│ ├── apps/
│ │ ├── admin-console/ <-- Consola Administrativa Principal
│ │ └── rf-terminal/ <-- Aplicación PWA para Terminales RF
│ ├── libs/shared-core/ <-- Core Compartido (Domain Ports & Infrastructure)
│ │ └── src/lib/domain/ports/
│ │ └── repository.tokens.ts <-- Injection Tokens (LICENSE, INVENTORY, SUPPLIER)
│ ├── e2e/ <-- Suite de Oráculo con Playwright
│ │ └── auth/auth-oracle.spec.ts
│ └── playwright.config.ts
└── 4guard_be/ <-- Backend Spring Boot
└── src/main/java/com/fourguard/wms/
├── domain/ports/out/ <-- Puertos de Salida (Transfer, Reception, Outbound)
├── infrastructure/persistence/ <-- Adaptadores JPA de Persistencia
└── application/usecase/ <-- Servicios de Dominio Hexagonales
🔄 Flujo de Trabajo Paso a Paso (Guía para Developers)
[ FASE 0: Prototipado Demo ] ──► (Inyección de MockAdapter en Angular)
│
▼
[ FASE 1: Análisis de GAPs READ-ONLY ] ──► (Auditoría de Reutilización en Spring Boot)
│
▼
[ FASE 2: Generación del SDD ] ──► 🛑 PUNTO DE CONTROL #1 (Aprobación del Arquitecto)
│
▼
[ FASE 3: Integración Backend ] ──► (Spring Boot Ports + HttpAdapter + Feature Flags)
│
▼
[ FASE 4: Oráculo Playwright ] ──► 🛑 PUNTO DE CONTROL #2 (Aprobación Final / Merge)
FASE 0: Prototipado e Inyección de Mocks en Angular
Objetivo: Desarrollar o ajustar el módulo en admin-console o rf-terminal utilizando el MockAdapter local y la interfaz Repository
.
📋 Prompt a copiar en Antigravity IDE (Fase 0)
Actúa como Senior Angular Developer en SyborX para el proyecto 4Guard_FE_UI.

Necesito construir/adaptar el módulo [NOMBRE_MODULO, ej: license-management] dentro de /4Guard_FE_UI/apps/admin-console/src/app/features/[NOMBRE_MODULO]/.

Instrucciones:

1. Define o reutiliza el InjectionToken correspondiente en /libs/shared-core/src/lib/domain/ports/repository.tokens.ts.
2. Define la interfaz del puerto `I[Modulo]Repository` en [modulo].repository.ts.
3. Implementa la clase `Mock[Modulo]RepositoryAdapter` dentro de adapters/ persisitiendo los datos en memoria/LocalStorage.
4. Conecta el componente visual inyectando el token `[MODULO]_REPOSITORY`. Los archivos .html y .scss deben depender únicamente de la interfaz.
5. Inyecta el adaptador Mock en app.config.ts usando la Feature Flag `featureFlags.useMockData = true` en environment.ts.

Verifica la compilación con `npx ng build admin-console`.
FASE 1: Diagnóstico de GAPs en Solo Lectura (READ-ONLY)
Objetivo: Comparar la pantalla de Angular contra el backend en Spring Boot (4guard_be) sin modificar código
.
📋 Prompt a copiar en Antigravity IDE (Fase 1)
Ejecuta la Skill demo-gap-analysis en modo SOLO LECTURA (READ-ONLY) sobre el módulo [NOMBRE_MODULO].

Fuentes a evaluar:

- Frontend Angular: /4Guard_FE_UI/apps/admin-console/src/app/features/[NOMBRE_MODULO]/
- Backend Spring Boot: /4guard_be/src/main/java/com/fourguard/wms/

REGLAS STRICTAS:

1. MODO READ-ONLY: Queda estrictamente prohibido modificar o crear archivos de código fuente.
2. AUDITORÍA BE: Examina los Puertos de Salida, Servicios de Caso de Uso, Entidades JPA y DTOs actuales en Spring Boot. Identifica qué componentes YA EXISTEN y pueden REUTILIZARSE o ADAPTARSE.
3. CLASIFICACIÓN DE GAPs: Registra las brechas en MISSING, PARTIAL, INCORRECT, VISUAL, DATA.
4. Genera el reporte de brechas en /docs/gap-analysis/gap-report-[NOMBRE_MODULO].md.
   FASE 2: Generación del Documento de Diseño de Software (SDD)
   Objetivo: Crear la especificación técnica con la firma de endpoints, DTOs, cambios en Spring Boot y el HttpAdapter de Angular
   .
   📋 Prompt a copiar en Antigravity IDE (Fase 2)
   Actúa como Lead Architect en SyborX.
   Lee /docs/gap-analysis/gap-report-[NOMBRE_MODULO].md y las especificaciones existentes en /docs/sdd/.

Genera el archivo /docs/sdd/SDD-[NUMERO]-[NOMBRE_MODULO].md especificando:

1. PLAN DE REUTILIZACIÓN: Servicios, Entidades y Repositorios de Spring Boot a reutilizar.
2. ARQUITECTURA HEXAGONAL: Firma de métodos a agregar en [Modulo]RepositoryPort.java y [Modulo]PersistenceAdapter.java.
3. REST CONTROLLERS & DTOs: Contrato JSON de endpoints en Spring Boot.
4. ADAPTADOR HTTP ANGULAR: Definición de Http[Modulo]Adapter.ts consumiendo HttpClient.
5. PLAN DE VALIDACIÓN ORÁCULO EN PLAYWRIGHT.

Genera ÚNICAMENTE el archivo SDD. NO modifiques código aún.
🛑 PUNTO DE CONTROL HUMANO #1 (APROBACIÓN DEL ARQUITECTO)
El Arquitecto revisa: El archivo /docs/sdd/SDD-[NUMERO]-[NOMBRE_MODULO].md.
Criterio: Garantizar que Spring Boot no acople JpaRepository directamente en los servicios de caso de uso y que se utilicen los puertos hexagonales
.
Respuesta del Arquitecto: "SDD Aprobado. Procede con la Fase 3".
FASE 3: Construcción e Integración Fullstack
Objetivo: Implementar los adaptadores en Spring Boot y Angular y conmuta las Feature Flags
.
📋 Prompt a copiar en Antigravity IDE (Fase 3 - Tras Aprobación)
Ejecuta la Skill demo-gap-implementation para migrar el módulo [NOMBRE_MODULO] usando la orden de trabajo en /docs/sdd/SDD-[NUMERO]-[NOMBRE_MODULO].md.

Pasos a ejecutar:

1. SPRING BOOT:
   - Implementa o extiende los métodos en [Modulo]RepositoryPort.java y [Modulo]PersistenceAdapter.java.
   - Actualiza el Servicio de Caso de Uso asegurando cero acoplamiento directo a JpaRepository.
   - Expón o adapta el Controller REST y sus DTOs.
2. ANGULAR:
   - Crea Http[Modulo]Adapter.ts e implementa la interfaz I[Modulo]Repository.
   - Actualiza environment.ts (`useMockData: false`) y revee `Http[Modulo]Adapter` en app.config.ts.
3. RESTRICCIÓN VISUAL: La plantilla .html y los estilos .scss en Angular deben permanecer INTACTOS.

Ejecuta compilación limpia de verificación al terminar.
FASE 4: Inspección de Calidad con el Oráculo Playwright
Objetivo: Confirmar equivalencia funcional, ausencia de errores en consola y cero regresiones visuales
.
📋 Prompt a copiar en Antigravity IDE (Fase 4)
Actúa como QA Automation Lead en SyborX.

Ejecuta la FASE DE VALIDACIÓN Y ORÁCULO DE COMPORTAMIENTO con Playwright para el módulo [NOMBRE_MODULO].

Instrucciones:

1. Ejecuta o crea la suite de prueba en /4Guard_FE_UI/e2e/[NOMBRE_MODULO]/[modulo]-oracle.spec.ts.
2. Valida la navegación, formularios y mensajes contra el backend en Spring Boot.
3. Escucha errores en consola (`page.on('console')` y `page.on('pageerror')`). La prueba debe fallar si existen errores HTTP 500 o excepciones no capturadas.
4. Ejecuta la regresión visual (`toHaveScreenshot({ maxDiffPixelRatio: 0.01 })`).
5. Genera el informe en /docs/gap-analysis/e2e-report-[NOMBRE_MODULO].md.
   🛑 PUNTO DE CONTROL HUMANO #2 (FIRMA DE LANZAMIENTO Y MERGE)
   Criterios de Aprobación:
   Reporte /docs/gap-analysis/e2e-report-[NOMBRE_MODULO].md con resultado PASS
   .
   Compilación exitosa en Backend y Frontend
   .
   🛠️ Comandos de Verificación Rápida
   Ejecuta estos comandos en tu terminal para validar el estado de salud del proyecto
   :

# 1. Validar Backend Spring Boot

cd 4guard_be
./mvnw.cmd test-compile -DskipTests

# 2. Validar Admin Console (Angular)

cd ../4Guard_FE_UI
npx ng build admin-console --no-progress

# 3. Validar RF-Terminal PWA (Angular)

npx ng build rf-terminal --no-progress

# 4. Ejecutar Oráculo de Pruebas E2E (Playwright)

npm run test:e2e
