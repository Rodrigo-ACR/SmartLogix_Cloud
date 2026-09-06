# ⚡ SmartLogix

Plataforma de gestión logística para PyMEs que venden en línea.

## Arquitectura

```
Frontend Vue.js (5173) → BFF (8085) → MS Inventario (8091)
                                     → MS Pedidos    (8092)
                                     → MS Envíos     (8093)
                                     → MS Usuarios   (8094)
```

## Requisitos

- Docker Desktop
- Java 17+ (para desarrollo local)
- Node.js 22+ (para desarrollo local)

## Levantar todo el sistema

```bash
docker-compose up --build
```

Una vez levantado:
- Frontend: http://localhost:5173
- BFF API: http://localhost:8085
- Swagger Inventario: http://localhost:8091/swagger-ui.html
- Swagger Pedidos: http://localhost:8092/swagger-ui.html
- Swagger Envíos: http://localhost:8093/swagger-ui.html
- Swagger Usuarios: http://localhost:8094/swagger-ui.html

## Crear usuario admin

```bash
curl -X POST http://localhost:8094/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Admin","correo":"admin@smartlogix.cl","password":"1234","rol":"ADMIN","activo":true}'
```

## Repositorio

https://github.com/Rodrigo-ACR/SmartLogix

## Estructura del proyecto

```
SmartLogix/
├── frontend/          # Vue.js 3 + Vite
├── bff/               # Spring Boot — BFF + JWT + Circuit Breaker
├── inventario/        # Spring Boot — MS Inventario
├── pedidos/           # Spring Boot — MS Pedidos
├── envios/            # Spring Boot — MS Envíos
├── usuarios/          # Spring Boot — MS Usuarios
└── docker-compose.yml
```
