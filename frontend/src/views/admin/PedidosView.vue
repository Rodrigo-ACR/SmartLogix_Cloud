<template>
    <div>
        <NavbarAdmin />
        <div class="container page">
            <h1 class="page-title">Pedidos</h1>

            <div v-if="error" class="error-banner-admin">
                <span>🔴</span>
                <div>
                    <strong>{{ error }}</strong>
                    <p>El sistema se recuperará automáticamente cuando el servicio vuelva a estar disponible.</p>
                </div>
                <button @click="$router.go(0)" class="btn-retry-admin">🔄 Reintentar</button>
            </div>

            <!-- Barra búsqueda y filtro -->
            <div class="filtros-bar">
                <div class="filtro-search">
                    <span class="search-icon">🔍</span>
                    <input v-model="busqueda" type="text" placeholder="Buscar por cliente, producto o ID..."
                        class="search-input" />
                    <button v-if="busqueda" @click="busqueda = ''" class="search-clear">✕</button>
                </div>
                <select v-model="filtroEstado" class="filtro-select">
                    <option value="">Todos los estados</option>
                    <option value="CREADO">CREADO</option>
                    <option value="VALIDADO">VALIDADO</option>
                    <option value="APROBADO">APROBADO</option>
                    <option value="EN_PREPARACION">EN_PREPARACION</option>
                    <option value="RECHAZADO">RECHAZADO</option>
                    <option value="PENDIENTE">PENDIENTE (envío)</option>
                    <option value="ASIGNADO">ASIGNADO (envío)</option>
                    <option value="EN_TRANSITO">EN_TRANSITO (envío)</option>
                    <option value="ENTREGADO">ENTREGADO (envío)</option>
                </select>
                <span class="filtro-count">{{ pedidosFiltrados.length }} de {{ pedidos.length }}</span>
            </div>

            <div class="tabla-card card">
                <table class="tabla">
                    <thead>
                        <tr>
                            <th>Cliente</th>
                            <th>Productos</th>
                            <th>Cant. total</th>
                            <th>Fecha</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <template v-if="loading">
                            <tr v-for="i in 5" :key="'sk' + i" class="skeleton-row-tr">
                                <td v-for="j in 6" :key="j">
                                    <div class="sk-cell"></div>
                                </td>
                            </tr>
                        </template>
                        <tr v-if="!loading && pedidosPaginados.length === 0">
                            <td colspan="6" class="text-center text-muted">
                                {{ busqueda || filtroEstado ? 'No hay resultados' : 'No hay pedidos registrados' }}
                            </td>
                        </tr>
                        <tr v-for="grupo in pedidosPaginados" :key="grupo.key">
                            <td>{{ grupo.cliente }}</td>
                            <td>
                                <div v-for="p in grupo.items" :key="p.id" class="grupo-prod-row">
                                    <span class="texto-prod">{{ p.nombreProducto || '-' }}</span>
                                    <span class="badge-qty">x{{ p.cantidad }}</span>
                                    <span class="text-muted" style="font-size:0.75rem">#{{ p.id }}</span>
                                </div>
                            </td>
                            <td>{{ grupo.cantidadTotal }}</td>
                            <td>{{ formatFecha(grupo.fecha) }}</td>
                            <td>
                                <span :class="badgeEstado(estadoGrupo(grupo).estado)" class="badge">
                                    {{ estadoGrupo(grupo).estado }}
                                </span>
                                <span v-if="estadoGrupo(grupo).tieneEnvio"
                                    style="display:block;font-size:0.72rem;color:var(--text-muted);margin-top:2px">🚚
                                    con envío</span>
                            </td>
                            <td>
                                <select v-if="!estadoGrupo(grupo).tieneEnvio" class="estado-select"
                                    :value="grupo.estado" @change="cambiarEstadoGrupo(grupo, $event.target.value)">
                                    <option value="CREADO">CREADO</option>
                                    <option value="VALIDADO">VALIDADO</option>
                                    <option value="APROBADO">APROBADO</option>
                                    <option value="RECHAZADO">RECHAZADO</option>
                                    <option value="EN_PREPARACION">EN_PREPARACION</option>
                                </select>
                                <span v-else class="text-muted" style="font-size:0.82rem">Gestionar en Envíos</span>
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
import { getPedidos, cambiarEstadoPedido, getEnvios } from "../../services/api";
import "@/assets/styles/pedidosview.css";
export default {
    components: { NavbarAdmin, Icons },
    data() {
        return { pedidos: [], envios: [], loading: true, error: "", busqueda: "", filtroEstado: "", pagina: 1, porPagina: 8 };
    },
    computed: {
        pedidosAgrupados() {
            const grupos = {};
            this.pedidos.forEach(p => {
                const key = p.grupoId || ((p.cliente || "") + "_" + (p.fecha ? p.fecha.substring(0, 16) : "") + "_" + p.id);
                if (!grupos[key]) grupos[key] = [];
                grupos[key].push(p);
            });
            return Object.entries(grupos)
                .sort((a, b) => b[0].localeCompare(a[0]))
                .map(([key, items]) => ({
                    key,
                    cliente: items[0].cliente,
                    fecha: items[0].fecha,
                    estado: items[0].estado,
                    items,
                    cantidadTotal: items.reduce((s, p) => s + p.cantidad, 0)
                }));
        },
        pedidosFiltrados() {
            return this.pedidosAgrupados.filter(g => {
                const texto = this.busqueda.toLowerCase();
                const coincide = !texto ||
                    (g.cliente || "").toLowerCase().includes(texto) ||
                    g.items.some(p => (p.nombreProducto || "").toLowerCase().includes(texto)) ||
                    g.items.some(p => String(p.id).includes(texto));
                const estadoReal = this.estadoGrupo ? this.estadoGrupo(g).estado : g.estado;
                const estado = !this.filtroEstado || estadoReal === this.filtroEstado;
                return coincide && estado;
            });
        },
        pedidosPaginados() {
            const inicio = (this.pagina - 1) * this.porPagina;
            return this.pedidosFiltrados.slice(inicio, inicio + this.porPagina);
        },
        totalPaginas() {
            return Math.ceil(this.pedidosFiltrados.length / this.porPagina);
        }
    },
    watch: {
        busqueda() { this.pagina = 1; },
        filtroEstado() { this.pagina = 1; }
    },
    async mounted() {
        try {
            const [pedidos, envios] = await Promise.allSettled([getPedidos(), getEnvios()]);
            if (pedidos.status === "fulfilled") this.pedidos = pedidos.value;
            else this.error = "⚠️ No se pudieron cargar los pedidos.";
            if (envios.status === "fulfilled") this.envios = envios.value;
        } catch { this.error = "⚠️ Error al cargar datos."; }
        this.loading = false;
    },
    methods: {
        async cambiarEstado(id, estado) {
            try {
                await cambiarEstadoPedido(id, estado);
                const p = this.pedidos.find(p => p.id === id);
                if (p) p.estado = estado;
            } catch { window.$toast.mostrar("Error al cambiar estado del pedido", "error"); }
        },
        async cambiarEstadoGrupo(grupo, estado) {
            // Cambia el estado de todos los pedidos del grupo
            try {
                for (const p of grupo.items) {
                    await cambiarEstadoPedido(p.id, estado);
                    const original = this.pedidos.find(x => x.id === p.id);
                    if (original) original.estado = estado;
                }
                grupo.estado = estado;
                window.$toast.mostrar("Estado actualizado para " + grupo.items.length + " pedido(s)", "success");
            } catch { window.$toast.mostrar("Error al cambiar estado", "error"); }
        },
        estadoGrupo(grupo) {
            // Buscar si algún pedido del grupo tiene envío
            for (const p of grupo.items) {
                const envio = this.envios.find(e => e.pedidoId === p.id);
                if (envio) return { estado: envio.estado, tieneEnvio: true };
            }
            return { estado: grupo.estado, tieneEnvio: false };
        },
        badgeEstado(e) {
            const m = { CREADO: "badge-accent", VALIDADO: "badge-warning", APROBADO: "badge-success", RECHAZADO: "badge-danger", EN_PREPARACION: "badge-warning" };
            return m[e] || "badge-accent";
        },
        formatFecha(f) {
            if (!f) return "-";
            return new Date(f).toLocaleDateString("es-CL");
        }
    }
}
</script>