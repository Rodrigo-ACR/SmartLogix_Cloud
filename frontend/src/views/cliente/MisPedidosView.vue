<template>
    <div>
        <NavbarCliente />

        <div class="container page">
            <h1 class="page-title">Mis Pedidos</h1>

            <div v-if="loading" class="loading-list">
                <div v-for="i in 4" :key="i" class="skeleton-row"></div>
            </div>

            <div v-else-if="error" class="error-banner" style="margin:1.5rem 0">
                <span class="error-icon">🔴</span>
                <div>
                    <strong>{{ error }}</strong>
                    <p>El sistema se recuperará automáticamente cuando el servicio vuelva a estar disponible.</p>
                </div>
                <button class="btn-retry" @click="$router.go(0)">🔄 Reintentar</button>
            </div>

            <div v-else-if="pedidos.length === 0" class="empty-state">
                <span>📦</span>
                <h3>Aún no tienes pedidos</h3>
                <p>¡Ve al catálogo y haz tu primera compra!</p>
                <button class="btn btn-primary" @click="$router.push('/inicio')">
                    Ver productos
                </button>
            </div>

            <div v-else class="pedidos-lista">
                <template v-for="(grupo, gi) in pedidosAgrupados" :key="gi">
                    <div class="pedido-card card" v-if="grupo && grupo.items && grupo.items[0]">
                        <div class="pedido-header">
                            <div>
                                <span class="pedido-id">Compra del {{ formatFecha(grupo.fecha) }}</span>
                                <h3 v-if="grupo.items.length === 1">{{ grupo.items[0].nombreProducto || 'Producto' }}
                                </h3>
                                <h3 v-else>{{ grupo.items.length }} productos</h3>
                            </div>
                            <span :class="badgeEstado(estadoFinal(grupo.items[0]))" class="badge">
                                {{ estadoFinal(grupo.items[0]) }}
                            </span>
                        </div>

                        <!-- Lista de productos del grupo -->
                        <div v-if="grupo.items.length > 1" class="grupo-productos">
                            <div v-for="p in grupo.items" :key="p.id" class="grupo-producto-item">
                                <span class="grupo-prod-nombre">{{ p.nombreProducto }}</span>
                                <span class="grupo-prod-qty">x{{ p.cantidad }}</span>
                                <span class="grupo-prod-id text-muted">#{{ p.id }}</span>
                            </div>
                        </div>

                        <div class="pedido-detalles">
                            <div class="detalle-item">
                                <span>Cantidad total</span>
                                <strong>{{grupo.items.reduce((s, p) => s + p.cantidad, 0)}}</strong>
                            </div>
                            <div class="detalle-item">
                                <span>Fecha</span>
                                <strong>{{ formatFecha(grupo.fecha) }}</strong>
                            </div>
                            <div class="detalle-item" v-if="envioDeP(grupo.items[0].id)">
                                <span>Transportista</span>
                                <strong>{{ envioDeP(grupo.items[0].id).transportista || '-' }}</strong>
                            </div>
                            <div class="detalle-item" v-if="envioDeP(grupo.items[0].id)">
                                <span>Entrega estimada</span>
                                <strong>{{ formatFecha(envioDeP(grupo.items[0].id).fechaEstimada) }}</strong>
                            </div>
                            <div class="detalle-item" v-if="envioDeP(grupo.items[0].id)">
                                <span>Dirección</span>
                                <strong>{{ envioDeP(grupo.items[0].id).direccion }}</strong>
                            </div>
                        </div>

                        <!-- Botón cancelar (solo CREADO) -->
                        <div v-if="grupo.items[0].estado === 'CREADO' && !envioDeP(grupo.items[0].id)"
                            style="margin-bottom:1rem">
                            <button class="btn-cancelar-pedido" @click="cancelarPedido(grupo.items[0])">
                                ✕ Cancelar pedido
                            </button>
                        </div>

                        <!-- PROGRESS BAR COMPLETO -->
                        <div class="pedido-progress">
                            <div v-for="(est, i) in todosLosEstados" :key="i" class="progress-step" :class="{
                                active: estadoFinal(grupo.items[0]) === est.key,
                                done: esAnterior(estadoFinal(grupo.items[0]), est.key),
                                disabled: !tieneEnvio(grupo.items[0]) && est.tipo === 'envio',
                                'envio-step': est.tipo === 'envio',
                                'entregado-step': est.key === 'ENTREGADO'
                            }">
                                <div class="step-dot">
                                    <span
                                        v-if="est.key === 'ENTREGADO' && (estadoFinal(grupo.items[0]) === 'ENTREGADO' || esAnterior(estadoFinal(grupo.items[0]), 'ENTREGADO'))"
                                        class="corona-emoji">👑</span>
                                </div>
                                <span>{{ est.label }}</span>
                            </div>
                        </div>

                        <!-- Banner celebración ENTREGADO -->
                        <div v-if="estadoFinal(grupo.items[0]) === 'ENTREGADO'" class="entregado-banner">
                            🎉 ¡Tu pedido fue entregado exitosamente! 👑
                        </div>
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
import NavbarCliente from "../../components/NavbarCliente.vue";
import { getPedidos, getEnvios, cambiarEstadoPedido } from "../../services/api";
import "@/assets/styles/mispedidosview.css";

const TODOS_ESTADOS = [
    { key: "CREADO", label: "Creado", tipo: "pedido" },
    { key: "VALIDADO", label: "Validado", tipo: "pedido" },
    { key: "APROBADO", label: "Aprobado", tipo: "pedido" },
    { key: "EN_PREPARACION", label: "En preparación", tipo: "pedido" },
    { key: "PENDIENTE", label: "Pendiente envío", tipo: "envio" },
    { key: "ASIGNADO", label: "Asignado", tipo: "envio" },
    { key: "EN_TRANSITO", label: "En tránsito", tipo: "envio" },
    { key: "ENTREGADO", label: "Entregado", tipo: "envio" },
];

const ORDEN = TODOS_ESTADOS.map(e => e.key);

export default {
    components: { NavbarCliente },
    data() {
        return {
            pedidos: [],
            envios: [],
            loading: true,
            error: "",
            todosLosEstados: TODOS_ESTADOS,
            clienteNombre: localStorage.getItem("nombre") || "",
            clienteCorreo: localStorage.getItem("correo") || ""
        };
    },
    computed: {
        pedidosAgrupados() {
            const grupos = {};
            this.pedidos.forEach(p => {
                const key = p.grupoId || ("solo_" + p.id);
                if (!grupos[key]) grupos[key] = [];
                grupos[key].push(p);
            });
            return Object.entries(grupos)
                .sort((a, b) => {
                    const fa = b[1][0]?.fecha || "";
                    const fb = a[1][0]?.fecha || "";
                    return fa.localeCompare(fb);
                })
                .map(([key, items]) => ({ fecha: items[0]?.fecha, items }))
                .filter(g => g.items && g.items.length > 0);
        }
    },
    async mounted() {
        const [pedidosRes, enviosRes] = await Promise.allSettled([getPedidos(), getEnvios()]);

        if (pedidosRes.status === "fulfilled") {
            this.pedidos = pedidosRes.value.filter(p =>
                p.cliente === this.clienteNombre || p.cliente === this.clienteCorreo
            ).reverse();
        } else {
            this.error = "⚠️ No se pudieron cargar tus pedidos. Intenta de nuevo en unos momentos.";
        }

        if (enviosRes.status === "fulfilled") {
            this.envios = enviosRes.value;
        }

        this.loading = false;
    },
    methods: {
        envioDeP(pedidoId) {
            return this.envios.find(e => e.pedidoId === pedidoId) || null;
        },
        tieneEnvio(p) {
            return !!this.envioDeP(p.id);
        },
        estadoFinal(p) {
            if (!p || !p.id) return "CREADO";
            const envio = this.envioDeP(p.id);
            if (envio) return envio.estado;
            return p.estado;
        },
        esAnterior(estadoActual, key) {
            const idxActual = ORDEN.indexOf(estadoActual);
            const idxKey = ORDEN.indexOf(key);
            return idxKey < idxActual;
        },
        badgeEstado(estado) {
            const map = {
                CREADO: "badge-accent",
                VALIDADO: "badge-warning",
                APROBADO: "badge-success",
                EN_PREPARACION: "badge-warning",
                PENDIENTE: "badge-warning",
                ASIGNADO: "badge-accent",
                EN_TRANSITO: "badge-warning",
                ENTREGADO: "badge-success",
                RECHAZADO: "badge-danger",
                INCIDENCIA: "badge-danger"
            };
            return map[estado] || "badge-accent";
        },
        async cancelarPedido(p) {
            const ok = await window.$confirm.abrir({
                titulo: "¿Cancelar este pedido?",
                mensaje: "El pedido #" + p.id + " de " + (p.nombreProducto || "producto") + " será cancelado. Esta acción no se puede deshacer.",
                icono: "❌", tipo: "danger", textoConfirmar: "Sí, cancelar"
            });
            if (!ok) return;
            try {
                await cambiarEstadoPedido(p.id, "RECHAZADO");
                p.estado = "RECHAZADO";
                window.$toast.mostrar("Pedido #" + p.id + " cancelado", "info");
            } catch {
                window.$toast.mostrar("No se pudo cancelar el pedido", "error");
            }
        },
        formatFecha(f) {
            if (!f) return "-";
            return new Date(f).toLocaleDateString("es-CL");
        }
    }
}
</script>

<style scoped>
.progress-step.disabled .step-dot {
    background: #1e293b;
    border-color: #334155;
}

.progress-step.disabled span {
    color: #334155;
}

.corona-emoji {
    font-size: 16px;
    line-height: 1;
    position: absolute;
    top: -18px;
    left: 50%;
    transform: translateX(-50%);
    animation: coronaFloat 1.5s ease-in-out infinite alternate;
    filter: drop-shadow(0 0 6px #ffd700);
    z-index: 10;
}

@keyframes coronaFloat {
    from {
        transform: translateX(-50%) translateY(0px);
    }

    to {
        transform: translateX(-50%) translateY(-4px);
    }
}
</style>