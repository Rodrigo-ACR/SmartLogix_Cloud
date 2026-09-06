<template>
    <nav class="navbar">
        <div class="navbar-inner container">
            <div class="navbar-brand" @click="$router.push('/admin')">
                <span>⚡</span> SmartLogix <span class="admin-tag">Admin</span>
            </div>

            <div class="navbar-links">
                <router-link to="/admin">Dashboard</router-link>
                <router-link to="/admin/productos">Productos</router-link>
                <router-link to="/admin/pedidos">Pedidos</router-link>
                <router-link to="/admin/envios">Envíos</router-link>
                <router-link to="/admin/clientes">Clientes</router-link>
                <router-link to="/ep1-demo" class="ep1-link">🎓 Demo EP1</router-link>
            </div>

            <div class="navbar-user">
                <span class="toggle-desktop"><ThemeToggle /></span>
                <span class="user-name">{{ nombre }}</span>
                <button class="btn-logout" @click="logout" title="Cerrar sesión">
                    <Icons name="logout" :size="20" color="currentColor" />
                </button>
            </div>

            <button class="hamburger" @click="menuAbierto = !menuAbierto" :class="{ open: menuAbierto }">
                <span></span><span></span><span></span>
            </button>
        </div>

        <div class="mobile-menu" :class="{ open: menuAbierto }">
            <router-link to="/admin"           @click="menuAbierto = false">🏠 Dashboard</router-link>
            <router-link to="/admin/productos" @click="menuAbierto = false">📦 Productos</router-link>
            <router-link to="/admin/pedidos"   @click="menuAbierto = false">🛒 Pedidos</router-link>
            <router-link to="/admin/envios"    @click="menuAbierto = false">🚚 Envíos</router-link>
            <router-link to="/admin/clientes"  @click="menuAbierto = false">👥 Clientes</router-link>
            <router-link to="/ep1-demo"        @click="menuAbierto = false">🎓 Demo EP1</router-link>
            <div class="mobile-footer">
                <div class="mobile-footer-row">
                    <ThemeToggle />
                    <span style="font-size:0.82rem;color:var(--text-muted)">Tema</span>
                </div>
                <div class="mobile-footer-row">
                    <span class="user-name">{{ nombre }}</span>
                    <button class="btn-logout" @click="logout" title="Cerrar sesión">
                        <Icons name="logout" :size="18" color="currentColor" />
                    </button>
                </div>
            </div>
        </div>
    </nav>
</template>

<script>
import ThemeToggle from "./ThemeToggle.vue";
import Icons from "./Icons.vue";
import "@/assets/styles/navbaradmin.css";
import { logoutMsal, isAuthenticated } from "../msal.js";

export default {
    components: { ThemeToggle, Icons },
    data() {
        return {
            nombre: localStorage.getItem("nombre") || "Admin",
            menuAbierto: false
        };
    },
    methods: {
        async logout() {
            localStorage.clear();
            if (isAuthenticated()) {
                await logoutMsal();
            } else {
                this.$router.push("/login");
            }
        }
    }
}
</script>

<style scoped>
.ep1-link {
    background: rgba(124,92,252,0.15) !important;
    color: var(--accent) !important;
    border-radius: 8px;
    padding: 4px 12px;
    font-weight: 600;
}
.ep1-link:hover { background: rgba(124,92,252,0.25) !important; }
</style>
