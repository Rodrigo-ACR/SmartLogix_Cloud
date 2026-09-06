# Frontend — SmartLogix

Panel cliente y admin desarrollado con Vue.js 3 + Vite.

## Tecnologías

- Vue.js 3
- Vue Router
- Axios
- Vite
- Cloudinary (imágenes de productos)

## Instalación local

```bash
cd frontend
npm install
npm run dev
```

Acceder en http://localhost:5173

## Con Docker

```bash
docker-compose up --build frontend
```

## Variables de entorno

Las credenciales de Cloudinary están en `src/views/admin/ProductosView.vue`:

```javascript
const CLOUD_NAME   = "diq24kgrd";
const UPLOAD_PRESET = "Productos";
```

## Estructura

```
src/
├── components/
│   ├── NavbarAdmin.vue
│   └── NavbarCliente.vue
├── views/
│   ├── LoginView.vue
│   ├── admin/
│   │   ├── DashboardView.vue
│   │   ├── ProductosView.vue
│   │   ├── PedidosView.vue
│   │   ├── EnviosView.vue
│   │   └── ClientesView.vue
│   └── cliente/
│       ├── InicioView.vue
│       ├── ProductoView.vue
│       ├── MisPedidosView.vue
│       └── PerfilView.vue
├── router/index.js
└── services/api.js
```

## Roles

| Rol | Acceso |
|-----|--------|
| ADMIN | /admin y todas sus subrutas |
| CLIENTE | /inicio, /producto/:id, /mis-pedidos, /perfil |
