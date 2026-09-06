<template>
    <div>
        <NavbarCliente />

        <!-- CARRUSEL CATEGORÍAS -->
        <section class="carousel-section">
            <div class="carousel-track">
                <div v-for="(cat, i) in categorias" :key="i" class="carousel-card" :class="'cat-' + cat.key">
                    <span class="cat-emoji">{{ cat.emoji }}</span>
                    <span class="cat-nombre">{{ cat.nombre }}</span>
                </div>
            </div>
        </section>

        <!-- HERO -->
        <section class="hero container">
            <div class="hero-text">
                <span class="hero-tag">Bienvenido, {{ nombre }} 👋</span>
                <h1>Tu operación logística, <span class="accent">más inteligente</span></h1>
                <p>Gestiona tus pedidos, controla tu inventario y rastrea tus envíos en tiempo real desde una sola
                    plataforma.</p>
            </div>
            <div class="hero-search">
                <input v-model="busqueda" type="text" placeholder="Buscar en el catálogo de productos..."
                    class="search-input" />
            </div>
        </section>

        <!-- CATÁLOGO -->
        <section class="catalogo container">
            <div class="catalogo-header">
                <h2>Catálogo</h2>
                <span class="text-muted">{{ productosFiltrados.length }} productos</span>
            </div>

            <div v-if="loading" class="loading-grid">
                <div v-for="i in 8" :key="i" class="skeleton-card"></div>
            </div>

            <!-- Banner de error Circuit Breaker -->
            <div v-if="!loading && error" class="error-banner">
                <span class="error-icon">🔴</span>
                <div>
                    <strong>{{ error }}</strong>
                    <p>El sistema de inventario está temporalmente fuera de servicio. Se recuperará automáticamente.</p>
                </div>
                <button class="btn-retry" @click="cargarProductos">🔄 Reintentar</button>
            </div>

            <div v-else class="productos-grid">
                <div v-for="p in productosFiltrados" :key="p.id" class="producto-card card"
                    @click="$router.push('/producto/' + p.id)">
                    <div class="producto-img">
                        <img v-if="p.imagen1" :src="p.imagen1" :alt="p.nombre" @error="onImgError" />
                        <div v-else class="producto-img-placeholder">
                            🛍️
                        </div>
                        <span v-if="p.stock <= 5 && p.stock > 0" class="badge badge-warning stock-badge">
                            Últimas {{ p.stock }} unidades
                        </span>
                        <span v-if="p.stock === 0" class="badge badge-danger stock-badge">
                            Agotado
                        </span>
                    </div>
                    <div class="producto-info">
                        <h3>{{ p.nombre }}</h3>
                        <p class="producto-desc">{{ p.descripcion || 'Sin descripción' }}</p>
                        <div class="producto-footer">
                            <span class="producto-precio">${{ formatPrecio(p.precio) }}</span>
                            <!-- Controles qty si ya está en carrito -->
                            <div v-if="enCarrito(p)" class="qty-controls-card">
                                <button class="qty-btn-card" @click.stop="decrementar(enCarrito(p))">−</button>
                                <span class="qty-num-card">{{ enCarrito(p).qty }}</span>
                                <button class="qty-btn-card" @click.stop="incrementar(enCarrito(p))"
                                    :disabled="enCarrito(p).qty >= p.stock">+</button>
                            </div>
                            <!-- Botón agregar si no está -->
                            <button v-else class="btn btn-primary btn-sm" @click.stop="agregarCarrito(p)"
                                :disabled="p.stock === 0">
                                + Agregar
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <p v-if="!loading && productosFiltrados.length === 0" class="empty-state">
                No se encontraron productos 😔
            </p>
        </section>

        <!-- CARRITO FLOTANTE -->
        <div v-if="carrito.length > 0" class="carrito-fab" @click="mostrarCarrito = true; pasoCheckout = 1">
            🛒 <span class="carrito-count">{{ totalItems }}</span>
            <span class="carrito-total">${{ formatPrecio(totalPrecio) }}</span>
        </div>

        <!-- MODAL CARRITO -->
        <!-- MODAL CARRITO / CHECKOUT -->
        <div v-if="mostrarCarrito" class="modal-overlay" @click.self="mostrarCarrito = false">
            <div class="modal checkout-modal">

                <!-- PASO 1: Carrito -->
                <div v-if="pasoCheckout === 1">
                    <div class="modal-header">
                        <h3>🛒 Tu carrito</h3>
                        <button @click="mostrarCarrito = false" class="btn-close">✕</button>
                    </div>
                    <div class="modal-body">
                        <div v-for="item in carrito" :key="item.id" class="carrito-item">
                            <div class="carrito-item-info">
                                <span class="carrito-item-nombre">{{ item.nombre }}</span>
                                <span class="carrito-item-precio">${{ formatPrecio(item.precio * item.qty) }}</span>
                            </div>
                            <div class="carrito-item-actions">
                                <button @click="decrementar(item)" class="qty-btn">−</button>
                                <span class="qty-num">{{ item.qty }}</span>
                                <button @click="incrementar(item)" class="qty-btn">+</button>
                                <button @click="quitarCarrito(item)" class="btn-remove">🗑</button>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <div class="carrito-resumen">
                            <span>{{ totalItems }} producto{{ totalItems !== 1 ? 's' : '' }}</span>
                            <span class="total-grande">${{ formatPrecio(totalPrecio) }}</span>
                        </div>
                        <button class="btn btn-primary w-full" @click="pasoCheckout = 2"
                            style="display:flex;align-items:center;justify-content:center;gap:8px">
                            Continuar con la compra
                            <Icons name="arrow-right" :size="18" color="white" />
                        </button>
                    </div>
                </div>

                <!-- PASO 2: Confirmación con dirección -->
                <div v-if="pasoCheckout === 2">
                    <div class="modal-header">
                        <button @click="pasoCheckout = 1" class="btn-back"
                            style="display:flex;align-items:center;gap:4px">
                            <Icons name="arrow-left" :size="16" color="currentColor" /> Volver
                        </button>
                        <h3>📋 Confirmar pedido</h3>
                        <button @click="mostrarCarrito = false" class="btn-close">✕</button>
                    </div>
                    <div class="modal-body">

                        <!-- Resumen de productos -->
                        <div class="checkout-seccion">
                            <h4 class="checkout-label">Productos</h4>
                            <div v-for="item in carrito" :key="item.id" class="checkout-item">
                                <span class="checkout-item-nombre">{{ item.nombre }}</span>
                                <span class="checkout-item-det">x{{ item.qty }}</span>
                                <span class="checkout-item-precio">${{ formatPrecio(item.precio * item.qty) }}</span>
                            </div>
                            <div class="checkout-total">
                                <span>Total</span>
                                <span>${{ formatPrecio(totalPrecio) }}</span>
                            </div>
                        </div>

                        <!-- Dirección de entrega -->
                        <div class="checkout-seccion">
                            <h4 class="checkout-label">Dirección de entrega</h4>
                            <div v-if="direcciones.length > 0" class="dir-opciones">
                                <label v-for="(d, i) in direcciones" :key="i" class="dir-opcion"
                                    :class="{ selected: direccionSeleccionada === d }">
                                    <input type="radio" v-model="direccionSeleccionada" :value="d" />
                                    <span>📍 {{ d }}</span>
                                </label>
                            </div>
                            <div class="nueva-dir-checkout">
                                <input v-model="nuevaDirCheckout" type="text" class="checkout-input"
                                    :placeholder="direcciones.length > 0 ? 'O ingresa una nueva dirección' : 'Ej: Av. Principal 123, Santiago'" />
                            </div>
                            <p v-if="errorDir" class="checkout-error">{{ errorDir }}</p>
                        </div>

                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-primary w-full" @click="confirmarPedido" :disabled="procesando">
                            {{ procesando ? 'Procesando...' : '✅ Confirmar compra' }}
                        </button>
                    </div>
                </div>

            </div>
        </div>

    </div>
</template>

<script>
import NavbarCliente from "../../components/NavbarCliente.vue";
import Icons from "../../components/Icons.vue";
import { getProductos, crearPedido } from "../../services/api";
import "@/assets/styles/inicioview.css";

const CATEGORIAS = [
    { nombre: "Electrónica", emoji: "💻", key: "electronica" },
    { nombre: "Ropa", emoji: "👕", key: "ropa" },
    { nombre: "Alimentos", emoji: "🍯", key: "alimentos" },
    { nombre: "Hogar", emoji: "🏠", key: "hogar" },
    { nombre: "Deportes", emoji: "⚽", key: "deportes" },
    { nombre: "Belleza", emoji: "💄", key: "belleza" },
    { nombre: "Juguetes", emoji: "🧸", key: "juguetes" },
    { nombre: "Mascotas", emoji: "🐾", key: "mascotas" },
];

export default {
    components: { NavbarCliente, Icons },

    data() {
        return {
            productos: [],
            busqueda: "",
            loading: true,
            error: "",
            carrito: [],
            pasoCheckout: 1,
            direcciones: [],
            direccionSeleccionada: "",
            nuevaDirCheckout: "",
            errorDir: "",
            mostrarCarrito: false,
            procesando: false,
            procesando: false,
            nombre: localStorage.getItem("nombre") || "Cliente",
            categorias: [],
        };
    },

    computed: {
        productosFiltrados() {
            if (!this.busqueda) return this.productos;
            const q = this.busqueda.toLowerCase();
            return this.productos.filter(p =>
                p.nombre?.toLowerCase().includes(q) ||
                p.descripcion?.toLowerCase().includes(q)
            );
        },
        totalItems() {
            return this.carrito.reduce((s, i) => s + i.qty, 0);
        },
        totalPrecio() {
            return this.carrito.reduce((s, i) => s + i.precio * i.qty, 0);
        }
    },

    mounted() {
        this.cargarProductos();
        this.categorias = [...CATEGORIAS, ...CATEGORIAS];
        // Cargar direcciones guardadas del perfil
        const dirs = localStorage.getItem("direcciones");
        if (dirs) {
            try {
                this.direcciones = JSON.parse(dirs);
                if (this.direcciones.length > 0) {
                    this.direccionSeleccionada = this.direcciones[0];
                }
            } catch { this.direcciones = []; }
        }
        // Restaurar carrito guardado
        const carritoGuardado = localStorage.getItem("carrito");
        if (carritoGuardado) {
            try { this.carrito = JSON.parse(carritoGuardado); } catch { this.carrito = []; }
        }
    },

    methods: {
        async cargarProductos() {
            this.loading = true;
            this.error = "";
            try {
                this.productos = await getProductos();
            } catch {
                this.productos = [];
                this.error = "⚠️ Inventario temporalmente no disponible. Intenta de nuevo en unos momentos.";
            }
            this.loading = false;
        },

        enCarrito(p) {
            return this.carrito.find(i => i.id === p.id) || null;
        },
        guardarCarrito() {
            localStorage.setItem("carrito", JSON.stringify(this.carrito));
        },
        agregarCarrito(p) {
            const existe = this.carrito.find(i => i.id === p.id);
            if (existe) {
                if (existe.qty < p.stock) existe.qty++;
            } else {
                this.carrito.push({ ...p, qty: 1 });
            }
            this.guardarCarrito();
        },

        decrementar(item) {
            if (item.qty > 1) item.qty--;
            else { this.quitarCarrito(item); return; }
            this.guardarCarrito();
        },

        incrementar(item) {
            const original = this.productos.find(p => p.id === item.id);
            if (original && item.qty < original.stock) item.qty++;
            this.guardarCarrito();
        },

        quitarCarrito(item) {
            this.carrito = this.carrito.filter(i => i.id !== item.id);
            this.guardarCarrito();
        },

        async confirmarPedido() {
            // Validar dirección
            const dir = this.nuevaDirCheckout.trim() || this.direccionSeleccionada;
            if (!dir) {
                this.errorDir = "Debes ingresar o seleccionar una dirección de entrega";
                return;
            }
            this.errorDir = "";
            this.procesando = true;

            // Guardar nueva dirección si la ingresó
            if (this.nuevaDirCheckout.trim() && !this.direcciones.includes(this.nuevaDirCheckout.trim())) {
                this.direcciones.push(this.nuevaDirCheckout.trim());
                localStorage.setItem("direcciones", JSON.stringify(this.direcciones));
            }

            const clienteNombre = localStorage.getItem("correo") || localStorage.getItem("nombre") || "Cliente";
            const grupoId = Date.now().toString(); // ID único por sesión de compra
            try {
                for (const item of this.carrito) {
                    await crearPedido({
                        cliente: clienteNombre,
                        productoId: item.id,
                        nombreProducto: item.nombre,
                        cantidad: item.qty,
                        direccion: dir,
                        grupoId: grupoId
                    });
                }
                this.carrito = [];
                localStorage.removeItem("carrito");
                this.mostrarCarrito = false;
                this.pasoCheckout = 1;
                this.nuevaDirCheckout = "";
                window.$toast.mostrar("¡Pedido realizado con éxito! 🎉", "success", 4000);
                this.$router.push("/mis-pedidos");
            } catch {
                window.$toast.mostrar("Error al crear el pedido. Intenta de nuevo.", "error");
            }
            this.procesando = false;
        },

        formatPrecio(n) {
            return Number(n).toLocaleString("es-CL");
        },

        onImgError(e) {
            e.target.style.display = "none";
        }
    }
}
</script>
<style scoped>
.error-banner {
    display: flex;
    align-items: center;
    gap: 1rem;
    background: linear-gradient(135deg, #1a0a0a, #2d1010);
    border: 1px solid #ef444466;
    border-radius: 12px;
    padding: 1.2rem 1.5rem;
    margin: 1.5rem 0;
    animation: slideIn 0.4s ease;
}

.error-icon {
    font-size: 2rem;
    flex-shrink: 0;
}

.error-banner strong {
    color: #ef4444;
    font-size: 1rem;
    display: block;
    margin-bottom: 0.3rem;
}

.error-banner p {
    color: #94a3b8;
    font-size: 0.85rem;
    margin: 0;
}

.btn-retry {
    margin-left: auto;
    flex-shrink: 0;
    background: #ef444422;
    color: #ef4444;
    border: 1px solid #ef444466;
    border-radius: 8px;
    padding: 0.5rem 1rem;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 600;
    transition: all 0.2s;
}

.btn-retry:hover {
    background: #ef444433;
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateY(-8px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>