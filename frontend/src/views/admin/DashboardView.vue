<template>
    <div>
        <NavbarAdmin />
        <div class="container page">

            <div class="dash-header">
                <div>
                    <h1>Dashboard</h1>
                    <p class="text-muted">Bienvenido de vuelta, {{ nombre }}</p>
                </div>
            </div>

            <!-- Banner error -->
            <div v-if="serviciosCaidos.length > 0" class="error-banner-admin" style="margin-bottom:1.5rem">
                <span>🔴</span>
                <div>
                    <strong>⚠️ Servicios no disponibles: {{ serviciosCaidos.join(', ') }}</strong>
                    <p>Los contadores de esos servicios no están disponibles temporalmente.</p>
                </div>
                <button @click="$router.go(0)" class="btn-retry-admin">🔄 Reintentar</button>
            </div>

            <!-- Stats cards -->
            <div class="stats-grid">
                <div v-for="(card, i) in statCards" :key="i" class="stat-card card" @click="$router.push(card.ruta)">
                    <div class="stat-icon">{{ card.icon }}</div>
                    <div class="stat-info">
                        <span class="stat-label">{{ card.label }}</span>
                        <span v-if="cargando" class="stat-skeleton"></span>
                        <span v-else class="stat-value" :class="{ 'stat-error': card.error }">
                            {{ card.error ? '—' : card.valor }}
                        </span>
                    </div>
                    <Icons name="arrow" :size="20" />
                </div>
            </div>

            <!-- Gráficos -->
            <div class="dash-grid" style="margin-bottom:1.5rem">
                <!-- Dona: pedidos por estado -->
                <div class="card chart-card">
                    <div class="chart-header">
                        <h3>📦 Pedidos por estado</h3>
                        <span class="chart-total">{{ stats.pedidos }} total</span>
                    </div>
                    <div v-if="errores.pedidos" class="empty-mini text-danger">⚠️ Servicio no disponible</div>
                    <div v-else-if="stats.pedidos === 0" class="empty-mini">Sin pedidos aún</div>
                    <div v-else class="chart-wrap">
                        <canvas ref="chartPedidos"></canvas>
                        <div class="chart-legend">
                            <div v-for="(item, i) in pedidosStats" :key="i" class="legend-item">
                                <span class="legend-dot" :style="{ background: item.color }"></span>
                                <span class="legend-label">{{ item.label }}</span>
                                <span class="legend-val">{{ item.value }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Barras: envíos por estado -->
                <div class="card chart-card">
                    <div class="chart-header">
                        <h3>🚚 Envíos por estado</h3>
                        <span class="chart-total">{{ stats.envios }} total</span>
                    </div>
                    <div v-if="errores.envios" class="empty-mini text-danger">⚠️ Servicio no disponible</div>
                    <div v-else-if="stats.envios === 0" class="empty-mini">Sin envíos aún</div>
                    <div v-else class="chart-wrap">
                        <canvas ref="chartEnvios"></canvas>
                    </div>
                </div>
            </div>

            <!-- Últimos registros -->
            <div class="dash-grid">
                <div class="card dash-section">
                    <h3>Últimos pedidos</h3>
                    <div v-if="errores.pedidos" class="empty-mini text-danger">⚠️ Servicio no disponible</div>
                    <div v-else-if="pedidos.length === 0" class="empty-mini">Sin pedidos aún</div>
                    <div v-for="p in pedidos.slice(0, 5)" :key="p.id" class="mini-row">
                        <div>
                            <span class="mini-id">#{{ p.id }}</span>
                            <span class="mini-nombre">{{ p.nombreProducto || 'Producto' }}</span>
                        </div>
                        <span :class="badgeEstado(p.estado)" class="badge">{{ p.estado }}</span>
                    </div>
                </div>
                <div class="card dash-section">
                    <h3>Últimos envíos</h3>
                    <div v-if="errores.envios" class="empty-mini text-danger">⚠️ Servicio no disponible</div>
                    <div v-else-if="envios.length === 0" class="empty-mini">Sin envíos aún</div>
                    <div v-for="e in envios.slice(0, 5)" :key="e.id" class="mini-row">
                        <div>
                            <span class="mini-id">#{{ e.id }}</span>
                            <span class="mini-nombre">{{ e.direccion }}</span>
                        </div>
                        <span :class="badgeEnvio(e.estado)" class="badge">{{ e.estado }}</span>
                    </div>
                </div>
            </div>

        </div>
    </div>
</template>

<script>
import Icons from "../../components/Icons.vue";
import NavbarAdmin from "../../components/NavbarAdmin.vue";
import { getProductos, getPedidos, getEnvios, getUsuarios } from "../../services/api";
import "@/assets/styles/admindashboardview.css";
import { Chart, DoughnutController, BarController, ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from "chart.js";

Chart.register(DoughnutController, BarController, ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

export default {
    components: { Icons, NavbarAdmin },
    data() {
        return {
            nombre: localStorage.getItem("nombre") || "Admin",
            cargando: true,
            stats: { productos: 0, pedidos: 0, envios: 0, clientes: 0 },
            errores: { productos: false, pedidos: false, envios: false, clientes: false },
            pedidos: [],
            envios: [],
            pedidosStats: [],
            enviosStats: [],
            chartPedidosInst: null,
            chartEnviosInst: null
        };
    },
    computed: {
        statCards() {
            return [
                { icon: '🛍️', label: 'Productos', valor: this.stats.productos, error: this.errores.productos, ruta: '/admin/productos' },
                { icon: '📦', label: 'Pedidos', valor: this.stats.pedidos, error: this.errores.pedidos, ruta: '/admin/pedidos' },
                { icon: '🚚', label: 'Envíos', valor: this.stats.envios, error: this.errores.envios, ruta: '/admin/envios' },
                { icon: '👥', label: 'Clientes', valor: this.stats.clientes, error: this.errores.clientes, ruta: '/admin/clientes' },
            ];
        },
        serviciosCaidos() {
            const nombres = { productos: "Inventario", pedidos: "Pedidos", envios: "Envíos", clientes: "Usuarios" };
            return Object.entries(this.errores).filter(([, v]) => v).map(([k]) => nombres[k]);
        }
    },
    async mounted() {
        const [productos, pedidos, envios, usuarios] = await Promise.allSettled([
            getProductos(), getPedidos(), getEnvios(), getUsuarios()
        ]);

        if (productos.status === "fulfilled") {
            this.stats.productos = productos.value.length;
        } else { this.errores.productos = true; }

        if (pedidos.status === "fulfilled") {
            const todosEnvios = envios.status === "fulfilled" ? envios.value : [];
            const pedidosConEnvio = new Set(todosEnvios.map(e => e.pedidoId));

            // Contar compras agrupadas SIN envío asignado
            const grupos = {};
            pedidos.value.forEach(p => {
                const key = p.grupoId || ("solo_" + p.id);
                if (!grupos[key]) grupos[key] = { tieneEnvio: false };
                if (pedidosConEnvio.has(p.id)) grupos[key].tieneEnvio = true;
            });
            this.stats.pedidos = Object.values(grupos).filter(g => !g.tieneEnvio).length;
            this.pedidos = [...pedidos.value].reverse();
            this.calcularEstadosPedidos(pedidos.value, todosEnvios);
        } else { this.errores.pedidos = true; }

        if (envios.status === "fulfilled") {
            this.envios = [...envios.value].reverse();
            // Contar grupos únicos de envíos
            const gruposEnvio = new Set(envios.value.map(e => e.grupoId || ("envio_" + e.id)));
            this.stats.envios = gruposEnvio.size;
            this.calcularEstadosEnvios(envios.value);
        } else { this.errores.envios = true; }

        if (usuarios.status === "fulfilled") {
            this.stats.clientes = usuarios.value.filter(u => u.rol === "CLIENTE").length;
        } else { this.errores.clientes = true; }

        this.cargando = false;

        await this.$nextTick();
        if (this.pedidosStats.length) this.renderChartPedidos();
        if (this.enviosStats.length) this.renderChartEnvios();
    },
    methods: {
        calcularEstadosPedidos(data, todosEnvios = []) {
            const colores = {
                CREADO: "#7c5cfc", VALIDADO: "#f59e0b", APROBADO: "#22c55e",
                EN_PREPARACION: "#0d9488", RECHAZADO: "#ef4444"
            };
            // IDs de pedidos que ya tienen envío asignado
            const pedidosConEnvio = new Set(todosEnvios.map(e => e.pedidoId));

            // Agrupar por grupoId, excluir grupos que YA tienen envío
            const grupos = {};
            data.forEach(p => {
                const key = p.grupoId || ("solo_" + p.id);
                if (!grupos[key]) grupos[key] = { estado: p.estado, tieneEnvio: false };
                if (pedidosConEnvio.has(p.id)) grupos[key].tieneEnvio = true;
            });

            // Solo contar grupos SIN envío
            const conteo = {};
            Object.values(grupos).forEach(g => {
                if (!g.tieneEnvio) {
                    conteo[g.estado] = (conteo[g.estado] || 0) + 1;
                }
            });

            this.pedidosStats = Object.entries(conteo).map(([label, value]) => ({
                label, value, color: colores[label] || "#64748b"
            }));
        },
        calcularEstadosEnvios(data) {
            const colores = {
                PENDIENTE: "#f59e0b", ASIGNADO: "#7c5cfc",
                EN_TRANSITO: "#0d9488", ENTREGADO: "#22c55e", INCIDENCIA: "#ef4444"
            };
            // Agrupar por grupoId, usar estado del primer envío del grupo
            const grupos = {};
            data.forEach(e => {
                const key = e.grupoId || ("envio_" + e.id);
                if (!grupos[key]) grupos[key] = e.estado;
            });
            const conteo = {};
            Object.values(grupos).forEach(estado => {
                conteo[estado] = (conteo[estado] || 0) + 1;
            });
            this.enviosStats = Object.entries(conteo).map(([label, value]) => ({
                label, value, color: colores[label] || "#64748b"
            }));
        },
        renderChartPedidos() {
            if (!this.$refs.chartPedidos) return;
            this.chartPedidosInst = new Chart(this.$refs.chartPedidos, {
                type: "doughnut",
                data: {
                    labels: this.pedidosStats.map(s => s.label),
                    datasets: [{ data: this.pedidosStats.map(s => s.value), backgroundColor: this.pedidosStats.map(s => s.color), borderWidth: 0, hoverOffset: 6 }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    cutout: "70%",
                    plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${ctx.raw}` } } }
                }
            });
        },
        renderChartEnvios() {
            if (!this.$refs.chartEnvios) return;
            this.chartEnviosInst = new Chart(this.$refs.chartEnvios, {
                type: "bar",
                data: {
                    labels: this.enviosStats.map(s => s.label),
                    datasets: [{ data: this.enviosStats.map(s => s.value), backgroundColor: this.enviosStats.map(s => s.color), borderRadius: 8, borderWidth: 0 }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => ` ${ctx.raw} envíos` } } },
                    scales: {
                        x: { grid: { display: false }, ticks: { color: "#64748b", font: { size: 11 } } },
                        y: { grid: { color: "rgba(255,255,255,0.05)" }, ticks: { color: "#64748b", stepSize: 1 }, beginAtZero: true }
                    }
                }
            });
        },
        badgeEstado(e) {
            const m = { CREADO: "badge-accent", VALIDADO: "badge-warning", APROBADO: "badge-success", RECHAZADO: "badge-danger" };
            return m[e] || "badge-accent";
        },
        badgeEnvio(e) {
            const m = { PENDIENTE: "badge-warning", ASIGNADO: "badge-accent", EN_TRANSITO: "badge-warning", ENTREGADO: "badge-success" };
            return m[e] || "badge-accent";
        }
    },
    beforeUnmount() {
        if (this.chartPedidosInst) this.chartPedidosInst.destroy();
        if (this.chartEnviosInst) this.chartEnviosInst.destroy();
    }
}
</script>

<style scoped>
.stat-error {
    color: var(--text-muted);
    font-size: 1.2rem;
}

.text-danger {
    color: var(--danger);
}

.chart-card {
    padding: 1.2rem 1.5rem;
}

.chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.chart-header h3 {
    margin: 0;
    font-size: 1rem;
}

.chart-total {
    font-size: 0.82rem;
    color: var(--text-muted);
}

.chart-wrap {
    display: flex;
    align-items: center;
    gap: 1.5rem;
}

.chart-wrap canvas {
    max-width: 160px;
    max-height: 160px;
    flex-shrink: 0;
}

.chart-legend {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.82rem;
}

.legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
}

.legend-label {
    flex: 1;
    color: var(--text-secondary);
}

.legend-val {
    font-weight: 600;
    color: var(--text-primary);
}

.empty-mini {
    color: var(--text-muted);
    font-size: 0.9rem;
    padding: 1rem 0;
    text-align: center;
}

@media (max-width:600px) {
    .chart-wrap {
        flex-direction: column;
    }

    .chart-wrap canvas {
        max-width: 140px;
        max-height: 140px;
    }
}
</style>