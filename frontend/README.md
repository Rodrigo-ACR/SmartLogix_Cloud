# SmartLogix — Cambios MSAL EP1

## ¿Qué archivos reemplazar?

### FRONTEND (carpeta frontend/)
| Archivo del zip | Reemplaza en el proyecto |
|---|---|
| `frontend/src/msal.js` | nuevo archivo |
| `frontend/src/main.js` | reemplaza el existente |
| `frontend/src/services/api.js` | reemplaza el existente |
| `frontend/src/router/index.js` | reemplaza el existente |
| `frontend/src/views/LoginView.vue` | reemplaza el existente |
| `frontend/src/views/Ep1DemoView.vue` | nuevo archivo |
| `frontend/src/components/NavbarAdmin.vue` | reemplaza el existente |
| `frontend/package.json` | reemplaza el existente |

### BFF (carpeta bff/)
| Archivo del zip | Reemplaza en el proyecto |
|---|---|
| `bff/pom.xml` | reemplaza el existente |
| `bff/Dockerfile` | reemplaza el existente |
| `bff/src/main/java/.../security/SecurityConfig.java` | reemplaza el existente |
| `bff/src/main/java/.../security/JwtFilter.java` | reemplaza el existente |
| `bff/src/main/resources/application.properties` | reemplaza el existente |

## Después de reemplazar
```bash
# Frontend (instalar msal-browser)
cd frontend
npm install

# Docker (recompila el BFF con Maven)
docker compose down
docker compose up --build
```

## Flujo
1. `http://localhost:5173` → redirige a Microsoft Entra ID
2. Login con cuenta del Tenant Interno 01
3. Azure AD emite Access Token con scopes read/write
4. BFF valida el token con JWKS de Azure AD
5. Sin token → 401, Con token válido → 200 OK
