# ============================================================
#  SmartLogix — Repositorios GitHub
#  Evaluación Parcial N°3 · DSY1106 · Desarrollo Fullstack III
#  Integrante: Rodrigo Concha
# ============================================================

## REPOSITORIO PRINCIPAL
URL:         https://github.com/Rodrigo-ACR/SmartLogix
Descripción: Monorepo con todos los componentes del proyecto SmartLogix.
             Incluye frontend, BFF, 4 microservicios backend, Eureka Server,
             docker-compose.yml, documentación y pruebas.
Rama activa: main
Commits:     9+ commits con mensajes semánticos (feat/fix/test)

## PIPELINE CI/CD
URL:         https://github.com/Rodrigo-ACR/SmartLogix/actions
Descripción: GitHub Actions con 4 jobs paralelos que ejecutan mvn test +
             JaCoCo en cada push a main.
Estado:      4/4 jobs verdes — 0 failures

# ============================================================
#  COMPONENTES DEL REPOSITORIO
# ============================================================

## FRONTEND
Ruta en repo: /frontend
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/frontend
Tecnología:   Vue.js 3 + Vite + Vue Router
Puerto:       5173
Descripción:  SPA con vistas para roles ADMIN y CLIENTE. Incluye login,
             catálogo de productos, carrito, mis pedidos, y panel admin
             con gestión de productos, pedidos, envíos y clientes.
Pruebas E2E:  Cypress v15 — 2 escenarios, 10 tests, 100% éxito
Ejecutar:     npm install && npm run dev

## BFF (Backend for Frontend)
Ruta en repo: /bff
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/bff
Tecnología:   Spring Boot 3.x + JWT (jjwt) + Resilience4j Circuit Breaker
Puerto:       8085
Descripción:  Centraliza la comunicación entre el frontend y los microservicios.
             Genera tokens JWT en /auth/login. Implementa Circuit Breaker con
             fallback HTTP 503 cuando un microservicio no responde.
Ejecutar:     ./mvnw spring-boot:run

## EUREKA SERVER (Service Discovery)
Ruta en repo: /eureka-server
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/eureka-server
Tecnología:   Spring Cloud Netflix Eureka
Puerto:       8761
Descripción:  Servidor de descubrimiento de servicios. Todos los microservicios
             se registran automáticamente al arrancar.
Estado:       5/5 servicios UP (BFF, Inventario, Pedidos, Envíos, Usuarios)
Dashboard:    http://localhost:8761

## MS INVENTARIO
Ruta en repo: /inventario
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/inventario
Tecnología:   Spring Boot 3.x + Spring Data JPA + PostgreSQL + H2 (tests)
Puerto:       8092
BD:           postgres-inventario · Puerto 5433
Descripción:  Gestión del catálogo de productos con imágenes (Cloudinary),
             precios y control de stock. Endpoint descontarStock para integración
             con MS Pedidos.
Pruebas:      31 tests unitarios + 3 integración · Cobertura: 89%
TF detectado: TF-04 — descontarStock aceptaba cantidad 0 o negativa
Swagger:      http://localhost:8091/swagger-ui.html
Ejecutar:     ./mvnw test (H2) | ./mvnw spring-boot:run (PostgreSQL)

## MS PEDIDOS
Ruta en repo: /pedidos
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/pedidos
Tecnología:   Spring Boot 3.x + Spring Data JPA + PostgreSQL + H2 (tests)
Puerto:       8092
BD:           postgres-pedidos · Puerto 5434
Descripción:  Control del ciclo de vida de pedidos. Implementa máquina de estados
             (patrón State simplificado): CREADO → VALIDADO → APROBADO →
             EN_PREPARACION. Transiciones inválidas retornan HTTP 409.
Pruebas:      20 tests unitarios + 3 integración · Cobertura: 96%
TF detectado: TF-01 — cambiarEstado aceptaba cualquier transición
Swagger:      http://localhost:8092/swagger-ui.html
Ejecutar:     ./mvnw test (H2) | ./mvnw spring-boot:run (PostgreSQL)

## MS ENVÍOS
Ruta en repo: /envios
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/envios
Tecnología:   Spring Boot 3.x + Spring Data JPA + PostgreSQL + H2 (tests)
Puerto:       8093
BD:           postgres-envios · Puerto 5435
Descripción:  Seguimiento de envíos en tiempo real. Máquina de estados:
             PENDIENTE → ASIGNADO → EN_TRANSITO → ENTREGADO | INCIDENCIA.
             Estado ENTREGADO es terminal.
Pruebas:      30 tests unitarios + 3 integración · Cobertura: 96%
TF detectado: TF-02 — cambiarEstado aceptaba transiciones inválidas
Swagger:      http://localhost:8093/swagger-ui.html
Ejecutar:     ./mvnw test (H2) | ./mvnw spring-boot:run (PostgreSQL)

## MS USUARIOS
Ruta en repo: /usuarios
URL directa:  https://github.com/Rodrigo-ACR/SmartLogix/tree/main/usuarios
Tecnología:   Spring Boot 3.x + Spring Data JPA + PostgreSQL + H2 (tests)
Puerto:       8094
BD:           postgres-usuarios · Puerto 5436
Descripción:  Gestión de usuarios con roles ADMIN y CLIENTE. Registro,
             login con validación de contraseña, y control de usuarios
             inhabilitados (activo=false).
Pruebas:      31 tests unitarios + 3 integración · Cobertura: 90%
TF detectado: TF-03 — login permitía acceso con usuario inactivo
Swagger:      http://localhost:8094/swagger-ui.html
Ejecutar:     ./mvnw test (H2) | ./mvnw spring-boot:run (PostgreSQL)

# ============================================================
#  LEVANTAR EL SISTEMA COMPLETO
# ============================================================

Requisito: Docker Desktop corriendo

cd SmartLogix
docker-compose up --build -d

Servicios disponibles:
  Frontend:      http://localhost:5173
  BFF:           http://localhost:8085
  Eureka:        http://localhost:8761
  Inventario:    http://localhost:8091/swagger-ui.html
  Pedidos:       http://localhost:8092/swagger-ui.html
  Envíos:        http://localhost:8093/swagger-ui.html
  Usuarios:      http://localhost:8094/swagger-ui.html

Credenciales de prueba:
  Admin:    admin@smartlogix.cl / 1234
  Cliente:  rodrigoconcharomero+1@gmail.com / Rodrigo2481

# ============================================================
#  RESUMEN DE PRUEBAS
# ============================================================

Tipo              Tests   Failures   Cobertura
-------------------------------------------------
Unitarias         113     0          89-96%
Integración        12     0          100%
E2E (Cypress)      10     0          100%
-------------------------------------------------
TOTAL             145     0          BUILD SUCCESS

Defectos TDD detectados y corregidos: TF-01, TF-02, TF-03, TF-04
Patrón de diseño aplicado: State (máquina de estados) en Pedidos y Envíos
CI/CD: GitHub Actions · 4 jobs paralelos · JaCoCo coverage report
