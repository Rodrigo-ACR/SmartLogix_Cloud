<template>
    <div v-if="visible" class="confirm-overlay" @click.self="cancelar">
        <div class="confirm-box">
            <div class="confirm-icon">{{ icono }}</div>
            <h3>{{ titulo }}</h3>
            <p>{{ mensaje }}</p>
            <div class="confirm-actions">
                <button class="btn btn-outline btn-sm" @click="cancelar">Cancelar</button>
                <button class="btn btn-sm" :class="'btn-' + tipo" @click="confirmar">{{ textoConfirmar }}</button>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: "ConfirmModal",
    data() {
        return {
            visible: false,
            titulo: "",
            mensaje: "",
            icono: "🗑️",
            tipo: "danger",
            textoConfirmar: "Eliminar",
            resolve: null
        };
    },
    methods: {
        abrir({ titulo, mensaje, icono = "🗑️", tipo = "danger", textoConfirmar = "Confirmar" }) {
            this.titulo = titulo;
            this.mensaje = mensaje;
            this.icono = icono;
            this.tipo = tipo;
            this.textoConfirmar = textoConfirmar;
            this.visible = true;
            return new Promise(res => { this.resolve = res; });
        },
        confirmar() { this.visible = false; this.resolve(true); },
        cancelar() { this.visible = false; this.resolve(false); }
    }
}
</script>

<style scoped>
.confirm-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9998;
    backdrop-filter: blur(4px);
}

.confirm-box {
    background: var(--bg-card, #1e1e2a);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 2rem;
    width: 100%;
    max-width: 380px;
    text-align: center;
    animation: popIn 0.25s ease;
}

.confirm-icon {
    font-size: 2.5rem;
    margin-bottom: 1rem;
}

.confirm-box h3 {
    font-size: 1.2rem;
    margin-bottom: 0.5rem;
    color: var(--text-primary);
}

.confirm-box p {
    color: var(--text-secondary);
    font-size: 0.9rem;
    line-height: 1.5;
    margin-bottom: 1.5rem;
}

.confirm-actions {
    display: flex;
    gap: 10px;
    justify-content: center;
}

.btn-danger {
    background: var(--danger);
    color: white;
}

.btn-warning {
    background: var(--warning);
    color: white;
}

.btn-accent {
    background: var(--accent);
    color: white;
}

@keyframes popIn {
    from {
        opacity: 0;
        transform: scale(0.9);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}
</style>