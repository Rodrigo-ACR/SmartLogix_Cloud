<template>
    <div>
        <NavbarAdmin />
        <div class="container page">
            <h1 class="page-title">Clientes</h1>

            <div v-if="error" class="error-banner-admin">
                <span>🔴</span>
                <div>
                    <strong>{{ error }}</strong>
                    <p>El sistema se recuperará automáticamente cuando el servicio vuelva a estar disponible.</p>
                </div>
                <button @click="$router.go(0)" class="btn-retry-admin">🔄 Reintentar</button>
            </div>

            <div class="filtros-bar">
                <div class="filtro-search">
                    <span class="search-icon">🔍</span>
                    <input v-model="busqueda" type="text" placeholder="Buscar por nombre o correo..."
                        class="search-input" />
                    <button v-if="busqueda" @click="busqueda = ''" class="search-clear">✕</button>
                </div>
                <span class="filtro-count">{{ clientesFiltrados.length }} de {{ clientes.length }}</span>
            </div>

            <div class="tabla-card card">
                <table class="tabla">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nombre</th>
                            <th>Correo</th>
                            <th>Teléfono</th>
                            <th>Dirección</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-if="loading">
                            <td colspan="6" class="text-center">Cargando...</td>
                        </tr>
                        <tr v-for="u in clientes" :key="u.id">
                            <td class="text-muted">#{{ u.id }}</td>
                            <td>
                                <div class="user-cell">
                                    <div class="user-avatar">{{ iniciales(u.nombre) }}</div>
                                    <span>{{ u.nombre }}</span>
                                </div>
                            </td>
                            <td>{{ u.correo }}</td>
                            <td>{{ u.telefono || '-' }}</td>
                            <td>{{ u.direccion || '-' }}</td>
                            <td>
                                <span :class="u.activo ? 'badge-success' : 'badge-danger'" class="badge">
                                    {{ u.activo ? 'Activo' : 'Inactivo' }}
                                </span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</template>

<script>
import NavbarAdmin from "../../components/NavbarAdmin.vue";
import Icons from "../../components/Icons.vue";
import { getUsuarios } from "../../services/api";
import "@/assets/styles/clientesview.css";

export default {
    components: { NavbarAdmin, Icons },
    data() {
        return { clientes: [], loading: true, error: "", busqueda: "", pagina: 1, porPagina: 8 };
    },
    computed: {
        clientesFiltrados() {
            const texto = this.busqueda.toLowerCase();
            if (!texto) return this.clientes;
            return this.clientes.filter(c =>
                (c.nombre || "").toLowerCase().includes(texto) ||
                (c.correo || "").toLowerCase().includes(texto)
            );
        },
        clientesPaginados() {
            const inicio = (this.pagina - 1) * this.porPagina;
            return this.clientesFiltrados.slice(inicio, inicio + this.porPagina);
        },
        totalPaginas() {
            return Math.ceil(this.clientesFiltrados.length / this.porPagina);
        }
    },
    watch: {
        busqueda() { this.pagina = 1; }
    },
    async mounted() {
        try {
            const todos = await getUsuarios();
            this.clientes = todos.filter(u => u.rol === "CLIENTE");
        } catch { }
        this.loading = false;
    },
    methods: {
        iniciales(nombre) {
            if (!nombre) return "?";
            return nombre.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2);
        }
    }
}
</script>