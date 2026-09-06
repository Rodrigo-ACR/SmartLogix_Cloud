import { createRouter, createWebHistory } from "vue-router";
import { isAuthenticated } from "../msal.js";

import LoginView     from "../views/LoginView.vue";
import InicioView    from "../views/cliente/InicioView.vue";
import ProductoView  from "../views/cliente/ProductoView.vue";
import MisPedidosView from "../views/cliente/MisPedidosView.vue";
import PerfilView    from "../views/cliente/PerfilView.vue";
import AdminDashboard from "../views/admin/DashboardView.vue";
import AdminProductos from "../views/admin/ProductosView.vue";
import AdminPedidos  from "../views/admin/PedidosView.vue";
import AdminEnvios   from "../views/admin/EnviosView.vue";
import AdminClientes from "../views/admin/ClientesView.vue";
import NotFoundView  from "../views/NotFoundView.vue";
import Ep1DemoView   from "../views/Ep1DemoView.vue";

const routes = [
    { path: "/", redirect: "/login" },
    { path: "/login", component: LoginView },

    // EP1 Demo
    { path: "/ep1-demo", component: Ep1DemoView, meta: { requiresAuth: true, rol: "ADMIN" } },

    // CLIENTE
    { path: "/inicio",        component: InicioView,     meta: { requiresAuth: true, rol: "CLIENTE" } },
    { path: "/producto/:id",  component: ProductoView,   meta: { requiresAuth: true, rol: "CLIENTE" } },
    { path: "/mis-pedidos",   component: MisPedidosView, meta: { requiresAuth: true, rol: "CLIENTE" } },
    { path: "/perfil",        component: PerfilView,     meta: { requiresAuth: true, rol: "CLIENTE" } },

    // ADMIN
    { path: "/admin",              component: AdminDashboard, meta: { requiresAuth: true, rol: "ADMIN" } },
    { path: "/admin/productos",    component: AdminProductos, meta: { requiresAuth: true, rol: "ADMIN" } },
    { path: "/admin/pedidos",      component: AdminPedidos,   meta: { requiresAuth: true, rol: "ADMIN" } },
    { path: "/admin/envios",       component: AdminEnvios,    meta: { requiresAuth: true, rol: "ADMIN" } },
    { path: "/admin/clientes",     component: AdminClientes,  meta: { requiresAuth: true, rol: "ADMIN" } },

    { path: "/:pathMatch(.*)*", name: "NotFound", component: NotFoundView },
];

const router = createRouter({ history: createWebHistory(), routes });

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem("azure_token") || localStorage.getItem("token");
    const rol   = localStorage.getItem("rol");

    if (to.meta.requiresAuth && !token) return next("/login");

    if (to.meta.rol && to.meta.rol !== rol) {
        if (rol === "ADMIN")   return next("/admin");
        if (rol === "CLIENTE") return next("/inicio");
        return next("/login");
    }

    next();
});

export default router;
