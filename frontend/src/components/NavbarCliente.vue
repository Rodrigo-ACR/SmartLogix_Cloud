<template>
    <nav class="navbar">
        <div class="navbar-inner container">
            <div class="navbar-brand" @click="$router.push('/inicio')">
                <span>⚡</span> SmartLogix
            </div>

            <div class="navbar-links">
                <router-link to="/inicio">Inicio</router-link>
                <router-link to="/mis-pedidos" class="link-badge-wrap">
                    Mis Pedidos
                    <span v-if="pedidosActivos > 0" class="nav-badge">{{ pedidosActivos }}</span>
                </router-link>
                <router-link to="/perfil">Mi Perfil</router-link>
            </div>

            <div class="navbar-user">
                <span class="toggle-desktop">
                    <ThemeToggle />
                </span>
                <span class="user-name">{{ nombre }}</span>
                <button class="btn-logout" @click="logout" title="Cerrar sesión">
                    <Icons name="logout" :size="20" color="currentColor" />
                </button>
            </div>

            <button class="hamburger" @click="menuAbierto = !menuAbierto" :class="{ open: menuAbierto }">
                <span></span>
                <span></span>
                <span></span>
            </button>
        </div>

        <div class="mobile-menu" :class="{ open: menuAbierto }">
            <router-link to="/inicio" @click="menuAbierto = false">🏠 Inicio</router-link>
            <router-link to="/mis-pedidos" @click="menuAbierto = false" class="link-badge-wrap">
                📦 Mis Pedidos
                <span v-if="pedidosActivos > 0" class="nav-badge">{{ pedidosActivos }}</span>
            </router-link>
            <router-link to="/perfil" @click="menuAbierto = false">👤 Mi Perfil</router-link>
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
import { getPedidos, getEnvios } from "../services/api";
import Icons from "./Icons.vue";
import "@/assets/styles/navbarcliente.css";
export default {
    components: { ThemeToggle, Icons },
    data() {
        return {
            nombre: localStorage.getItem("nombre") || "Cliente",
            correo: localStorage.getItem("correo") || "",
            menuAbierto: false,
            pedidosActivos: 0
        };
    },
    async mounted() {
        try {
            const nombre = localStorage.getItem("nombre") || "";
            const correo = localStorage.getItem("correo") || "";
            const [pedidosRes, enviosRes] = await Promise.allSettled([getPedidos(), getEnvios()]);

            const misPedidos = pedidosRes.status === "fulfilled"
                ? pedidosRes.value.filter(p => p.cliente === nombre || p.cliente === correo)
                : [];
            const misEnvios = enviosRes.status === "fulfilled" ? enviosRes.value : [];

            // Agrupar por grupoId (o id individual si no tiene grupo)
            const grupos = {};
            misPedidos.forEach(p => {
                const key = p.grupoId || ("solo_" + p.id);
                if (!grupos[key]) grupos[key] = [];
                grupos[key].push(p);
            });

            // Contar grupos cuyo estado final no sea ENTREGADO ni RECHAZADO
            let activos = 0;
            Object.values(grupos).forEach(items => {
                const envio = misEnvios.find(e => e.pedidoId === items[0].id);
                const estadoFinal = envio ? envio.estado : items[0].estado;
                if (!["RECHAZADO", "ENTREGADO"].includes(estadoFinal)) activos++;
            });
            this.pedidosActivos = activos;
        } catch { this.pedidosActivos = 0; }
    },
    methods: {
        logout() {
            localStorage.clear();
            this.$router.push("/login");
        }
    }
}
</script>