<template>
    <div>
        <NavbarCliente />

        <div class="container page" v-if="producto">

            <button class="btn-back" @click="$router.back()" style="display:flex;align-items:center;gap:4px">
                <Icons name="arrow-left" :size="16" color="currentColor" /> Volver
            </button>

            <div class="producto-detalle">

                <!-- GALERÍA -->
                <div class="galeria">
                    <div class="img-principal">
                        <img v-if="imagenActiva" :src="imagenActiva" :alt="producto.nombre" />
                        <div v-else class="img-placeholder">🛍️</div>
                    </div>
                    <div class="img-thumbs" v-if="imagenes.length > 1">
                        <div v-for="(img, i) in imagenes" :key="i" class="thumb"
                            :class="{ active: imagenActiva === img }" @click="imagenActiva = img">
                            <img :src="img" />
                        </div>
                    </div>
                </div>

                <!-- INFO -->
                <div class="producto-info">
                    <span class="badge badge-accent">{{ producto.stock > 0 ? 'En stock' : 'Agotado' }}</span>
                    <h1>{{ producto.nombre }}</h1>
                    <p class="descripcion">{{ producto.descripcion || 'Sin descripción disponible.' }}</p>

                    <div class="precio-section">
                        <span class="precio">${{ formatPrecio(producto.precio) }}</span>
                        <span class="stock-info">{{ producto.stock }} disponibles</span>
                    </div>

                    <div class="cantidad-section">
                        <label>Cantidad</label>
                        <div class="cantidad-control">
                            <button @click="cantidad > 1 ? cantidad-- : null" class="qty-btn">−</button>
                            <span>{{ cantidad }}</span>
                            <button @click="cantidad < producto.stock ? cantidad++ : null" class="qty-btn">+</button>
                        </div>
                    </div>

                    <div class="acciones">
                        <button class="btn btn-primary" :disabled="producto.stock === 0" @click="abrirConfirmacion">
                            ⚡ Comprar ahora
                        </button>
                    </div>

                    <div v-if="mensaje" class="mensaje-exito">
                        ✅ {{ mensaje }}
                    </div>
                </div>
            </div>
        </div>

        <div v-else-if="loading" class="container page">
            <div class="skeleton-detalle"></div>
        </div>

        <!-- MODAL CONFIRMACIÓN DE COMPRA -->
        <div v-if="mostrarConfirmacion" class="modal-overlay" @click.self="mostrarConfirmacion = false">
            <div class="modal checkout-modal">
                <div class="modal-header">
                    <h3>📋 Confirmar pedido</h3>
                    <button @click="mostrarConfirmacion = false" class="btn-close">✕</button>
                </div>
                <div class="modal-body">

                    <div class="checkout-seccion">
                        <h4 class="checkout-label">Producto</h4>
                        <div class="checkout-item">
                            <span class="checkout-item-nombre">{{ producto.nombre }}</span>
                            <span class="checkout-item-det">x{{ cantidad }}</span>
                            <span class="checkout-item-precio">${{ formatPrecio(producto.precio * cantidad) }}</span>
                        </div>
                        <div class="checkout-total">
                            <span>Total</span>
                            <span>${{ formatPrecio(producto.precio * cantidad) }}</span>
                        </div>
                    </div>

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
                    <button class="btn btn-primary w-full" @click="comprarAhora" :disabled="comprando">
                        {{ comprando ? 'Procesando...' : '✅ Confirmar compra' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import Icons from "../../components/Icons.vue";
import NavbarCliente from "../../components/NavbarCliente.vue";
import { getProductos, crearPedido } from "../../services/api";
import "@/assets/styles/productoview.css";
import "@/assets/styles/inicioview.css";

export default {
    components: { NavbarCliente, Icons },
    data() {
        return {
            producto: null,
            imagenActiva: null,
            cantidad: 1,
            loading: true,
            comprando: false,
            mensaje: "",
            mostrarConfirmacion: false,
            direcciones: [],
            direccionSeleccionada: "",
            nuevaDirCheckout: "",
            errorDir: ""
        };
    },
    computed: {
        imagenes() {
            if (!this.producto) return [];
            return [this.producto.imagen1, this.producto.imagen2, this.producto.imagen3]
                .filter(Boolean);
        }
    },
    async mounted() {
        const id = this.$route.params.id;
        try {
            const productos = await getProductos();
            this.producto = productos.find(p => p.id == id);
            if (this.imagenes.length) this.imagenActiva = this.imagenes[0];
        } catch {
            this.producto = null;
        }
        // Cargar direcciones guardadas
        const dirs = localStorage.getItem("direcciones");
        if (dirs) {
            try {
                this.direcciones = JSON.parse(dirs);
                if (this.direcciones.length > 0) this.direccionSeleccionada = this.direcciones[0];
            } catch { this.direcciones = []; }
        }
        this.loading = false;
    },
    methods: {
        abrirConfirmacion() {
            this.errorDir = "";
            this.mostrarConfirmacion = true;
        },
        async comprarAhora() {
            const dir = this.nuevaDirCheckout.trim() || this.direccionSeleccionada;
            if (!dir) {
                this.errorDir = "Debes ingresar o seleccionar una dirección de entrega";
                return;
            }
            this.errorDir = "";
            this.comprando = true;

            if (this.nuevaDirCheckout.trim() && !this.direcciones.includes(this.nuevaDirCheckout.trim())) {
                this.direcciones.push(this.nuevaDirCheckout.trim());
                localStorage.setItem("direcciones", JSON.stringify(this.direcciones));
            }

            try {
                await crearPedido({
                    cliente: localStorage.getItem("correo") || localStorage.getItem("nombre") || "Cliente",
                    productoId: this.producto.id,
                    nombreProducto: this.producto.nombre,
                    cantidad: this.cantidad,
                    direccion: dir,
                    grupoId: Date.now().toString()
                });
                this.mostrarConfirmacion = false;
                window.$toast.mostrar("¡Pedido realizado con éxito! 🎉", "success", 4000);
                setTimeout(() => this.$router.push("/mis-pedidos"), 1000);
            } catch {
                window.$toast.mostrar("Error al crear el pedido. Intenta de nuevo.", "error");
            }
            this.comprando = false;
        },
        formatPrecio(n) {
            return Number(n).toLocaleString("es-CL");
        }
    }
}
</script>