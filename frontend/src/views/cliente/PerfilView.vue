<template>
    <div>
        <NavbarCliente />
        <div class="container page">

            <div class="perfil-hero">
                <div class="perfil-avatar-wrap">
                    <div class="perfil-avatar">{{ iniciales }}</div>
                    <div class="perfil-avatar-ring"></div>
                </div>
                <div class="perfil-hero-info">
                    <h1>{{ nombre }}</h1>
                    <span class="badge badge-accent">{{ rol }}</span>
                    <p class="text-muted" style="margin-top:8px">Miembro de SmartLogix</p>
                </div>
            </div>

            <div class="perfil-grid">

                <!-- Datos personales editables -->
                <div class="card perfil-seccion">
                    <div class="seccion-header">
                        <h3>📋 Información personal</h3>
                        <button v-if="!editando" class="btn btn-outline btn-sm" @click="abrirEdicion">✏️ Editar</button>
                        <button v-else class="btn btn-outline btn-sm" @click="cancelarEdicion">Cancelar</button>
                    </div>

                    <div v-if="!editando" class="info-lista">
                        <div class="info-item">
                            <span>Nombre completo</span>
                            <strong>{{ nombre }}</strong>
                        </div>
                        <div class="info-item">
                            <span>Correo</span>
                            <strong>{{ correo }}</strong>
                        </div>
                        <div class="info-item">
                            <span>Teléfono</span>
                            <strong>{{ telefono || 'No registrado' }}</strong>
                        </div>
                        <div class="info-item">
                            <span>ID de usuario</span>
                            <strong>#{{ id }}</strong>
                        </div>
                    </div>

                    <div v-else class="edit-form">
                        <div class="edit-field">
                            <label>Nombre completo</label>
                            <input v-model="form.nombre" type="text" class="edit-input" placeholder="Tu nombre" />
                        </div>
                        <div class="edit-field">
                            <label>Teléfono</label>
                            <input v-model="form.telefono" type="text" class="edit-input"
                                placeholder="+56 9 XXXX XXXX" />
                        </div>
                        <div class="edit-field">
                            <label>Nueva contraseña <span style="color:var(--text-muted);font-size:0.8rem">(dejar vacío
                                    para no cambiar)</span></label>
                            <input v-model="form.password" type="password" class="edit-input" placeholder="••••••••" />
                        </div>
                        <button class="btn btn-primary w-full" @click="guardarPerfil" :disabled="guardando"
                            style="margin-top:8px">
                            {{ guardando ? 'Guardando...' : '💾 Guardar cambios' }}
                        </button>
                    </div>
                </div>

                <!-- Direcciones -->
                <div class="card perfil-seccion">
                    <h3>📍 Mis direcciones</h3>
                    <div class="direcciones-lista">
                        <div v-for="(dir, i) in direcciones" :key="i" class="direccion-item">
                            <span class="dir-icon">🏠</span>
                            <span class="dir-texto">{{ dir }}</span>
                            <button class="btn-remove-dir" @click="eliminarDireccion(i)">✕</button>
                        </div>
                        <div v-if="direcciones.length === 0" class="empty-dir">
                            No tienes direcciones guardadas
                        </div>
                    </div>
                    <div v-if="agregando" class="nueva-dir-form">
                        <input v-model="nuevaDireccion" type="text" placeholder="Ej: Av. Siempre Viva 123, Santiago"
                            @keyup.enter="guardarDireccion" />
                        <div class="nueva-dir-btns">
                            <button class="btn btn-outline btn-sm" @click="agregando = false">Cancelar</button>
                            <button class="btn btn-primary btn-sm" @click="guardarDireccion" :disabled="guardando">
                                {{ guardando ? 'Guardando...' : 'Guardar' }}
                            </button>
                        </div>
                    </div>
                    <button v-else class="btn btn-outline w-full" style="margin-top:16px" @click="agregando = true">
                        + Agregar dirección
                    </button>
                </div>

                <!-- Acciones rápidas -->
                <div class="card perfil-seccion">
                    <h3>⚡ Acciones rápidas</h3>
                    <div class="acciones-lista">
                        <button class="accion-btn" @click="$router.push('/inicio')">
                            <span class="accion-icon">🛍️</span>
                            <div><strong>Ver catálogo</strong>
                                <p>Explora todos los productos</p>
                            </div>
                            <Icons name="arrow" :size="18" />
                        </button>
                        <button class="accion-btn" @click="$router.push('/mis-pedidos')">
                            <span class="accion-icon">📦</span>
                            <div><strong>Mis pedidos</strong>
                                <p>Revisa el estado de tus compras</p>
                            </div>
                            <Icons name="arrow" :size="18" />
                        </button>
                        <button class="accion-btn danger" @click="logout">
                            <span class="accion-icon">🚪</span>
                            <div><strong>Cerrar sesión</strong>
                                <p>Salir de tu cuenta</p>
                            </div>
                            <Icons name="arrow" :size="18" />
                        </button>
                    </div>
                </div>

            </div>
        </div>
    </div>
</template>

<script>
import Icons from "../../components/Icons.vue";
import NavbarCliente from "../../components/NavbarCliente.vue";
import { actualizarUsuario } from "../../services/api";
import "@/assets/styles/perfilview.css";

export default {
    components: { Icons, NavbarCliente },
    data() {
        return {
            nombre: localStorage.getItem("nombre") || "Cliente",
            correo: localStorage.getItem("correo") || "",
            telefono: (() => {
                const t = localStorage.getItem("telefono") || "";
                return t.includes("@") ? "" : t;
            })(),
            rol: localStorage.getItem("rol") || "CLIENTE",
            id: localStorage.getItem("id") || "-",
            direcciones: [],
            agregando: false,
            nuevaDireccion: "",
            guardando: false,
            editando: false,
            form: { nombre: "", telefono: "", password: "" }
        };
    },
    computed: {
        iniciales() {
            return this.nombre.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2);
        }
    },
    mounted() {
        // Limpiar teléfono si tiene el correo guardado por error
        const telGuardado = localStorage.getItem("telefono") || "";
        if (telGuardado.includes("@")) {
            localStorage.removeItem("telefono");
            this.telefono = "";
        }
        const dirs = localStorage.getItem("direcciones");
        if (dirs) this.direcciones = JSON.parse(dirs);
    },
    methods: {
        abrirEdicion() {
            this.form.nombre = this.nombre;
            // Si el teléfono guardado es un email, dejarlo vacío
            const tel = this.telefono || "";
            this.form.telefono = tel.includes("@") ? "" : tel;
            this.form.password = "";
            this.editando = true;
        },
        cancelarEdicion() {
            this.editando = false;
            this.form = { nombre: "", telefono: "", password: "" };
        },
        async guardarPerfil() {
            if (!this.form.nombre.trim() && !this.form.telefono.trim() && !this.form.password.trim()) {
                window.$toast.mostrar("No hay cambios para guardar", "warning");
                return;
            }
            this.guardando = true;
            try {
                const payload = {
                    nombre: this.form.nombre.trim() || this.nombre,
                    telefono: this.form.telefono.trim() || this.telefono,
                    correo: this.correo,
                    direccion: JSON.stringify(this.direcciones)
                };
                if (this.form.password.trim()) payload.password = this.form.password.trim();

                await actualizarUsuario(this.id, payload);

                if (this.form.nombre.trim()) {
                    this.nombre = this.form.nombre.trim();
                    localStorage.setItem("nombre", this.nombre);
                }
                if (this.form.telefono.trim()) {
                    this.telefono = this.form.telefono.trim();
                    localStorage.setItem("telefono", this.telefono);
                }
                this.editando = false;
                this.form = { nombre: "", telefono: "", password: "" };
                window.$toast.mostrar("Perfil actualizado correctamente", "success");
            } catch {
                window.$toast.mostrar("Error al actualizar el perfil", "error");
            }
            this.guardando = false;
        },
        async guardarDireccion() {
            if (!this.nuevaDireccion.trim()) return;
            this.guardando = true;
            try {
                this.direcciones.push(this.nuevaDireccion.trim());
                localStorage.setItem("direcciones", JSON.stringify(this.direcciones));
                // Guardar el array completo como JSON en el campo direccion del backend
                await actualizarUsuario(this.id, {
                    nombre: this.nombre,
                    direccion: JSON.stringify(this.direcciones)
                });
                this.nuevaDireccion = "";
                this.agregando = false;
                window.$toast.mostrar("Dirección guardada", "success");
            } catch {
                window.$toast.mostrar("Error al guardar la dirección", "error");
            }
            this.guardando = false;
        },
        async eliminarDireccion(i) {
            this.direcciones.splice(i, 1);
            localStorage.setItem("direcciones", JSON.stringify(this.direcciones));
            try {
                await actualizarUsuario(this.id, {
                    nombre: this.nombre,
                    direccion: JSON.stringify(this.direcciones)
                });
            } catch { }
            window.$toast.mostrar("Dirección eliminada", "info");
        },
        logout() {
            localStorage.clear();
            this.$router.push("/login");
        }
    }
}
</script>

<style scoped>
.seccion-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.seccion-header h3 {
    margin: 0;
}

.edit-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.edit-field {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.edit-field label {
    font-size: 0.82rem;
    color: var(--text-secondary);
}

.edit-input {
    background: var(--bg-secondary);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 9px 12px;
    color: var(--text-primary);
    font-size: 0.9rem;
    transition: var(--transition);
}

.edit-input:focus {
    outline: none;
    border-color: var(--accent);
    box-shadow: 0 0 0 3px var(--accent-glow);
}
</style>