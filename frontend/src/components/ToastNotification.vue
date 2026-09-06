<template>
    <div class="toast-container">
        <transition-group name="toast">
            <div v-for="t in toasts" :key="t.id" class="toast" :class="'toast-' + t.tipo">
                <span class="toast-icon">{{ iconos[t.tipo] }}</span>
                <span class="toast-msg">{{ t.mensaje }}</span>
                <button class="toast-close" @click="quitar(t.id)">✕</button>
            </div>
        </transition-group>
    </div>
</template>

<script>
export default {
    name: "ToastNotification",
    data() {
        return {
            toasts: [],
            iconos: { success: "✅", error: "❌", warning: "⚠️", info: "ℹ️" }
        };
    },
    methods: {
        mostrar(mensaje, tipo = "success", duracion = 3000) {
            const id = Date.now();
            this.toasts.push({ id, mensaje, tipo });
            setTimeout(() => this.quitar(id), duracion);
        },
        quitar(id) {
            this.toasts = this.toasts.filter(t => t.id !== id);
        }
    }
}
</script>

<style scoped>
.toast-container {
    position: fixed;
    bottom: 24px;
    right: 24px;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    gap: 10px;
    pointer-events: none;
}
.toast {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 0.9rem;
    font-weight: 500;
    min-width: 260px;
    max-width: 360px;
    pointer-events: all;
    backdrop-filter: blur(12px);
    border: 1px solid transparent;
    animation: slideUp 0.3s ease;
}
.toast-success { background: rgba(34,197,94,0.15); border-color: rgba(34,197,94,0.3); color: #22c55e; }
.toast-error   { background: rgba(239,68,68,0.15);  border-color: rgba(239,68,68,0.3);  color: #ef4444; }
.toast-warning { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.3); color: #f59e0b; }
.toast-info    { background: rgba(124,92,252,0.15); border-color: rgba(124,92,252,0.3); color: #7c5cfc; }
.toast-icon { font-size: 1rem; flex-shrink: 0; }
.toast-msg  { flex: 1; color: var(--text-primary); }
.toast-close { background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 0 4px; font-size: 0.8rem; }
.toast-close:hover { color: var(--text-primary); }
@keyframes slideUp {
    from { opacity: 0; transform: translateY(16px); }
    to   { opacity: 1; transform: translateY(0); }
}
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-leave-to { opacity: 0; transform: translateX(20px); }
</style>