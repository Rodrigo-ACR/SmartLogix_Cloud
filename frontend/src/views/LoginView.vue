<template>
    <div class="login-page">
        <div class="login-box">
            <div class="login-logo">⚡</div>
            <h1 class="login-title">SmartLogix</h1>
            <p class="login-sub">Sistema de Gestión Logística</p>

            <div class="login-divider"></div>

            <button class="btn-microsoft" @click="login" :disabled="loading">
                <svg width="20" height="20" viewBox="0 0 21 21">
                    <rect x="1" y="1" width="9" height="9" fill="#F25022"/>
                    <rect x="11" y="1" width="9" height="9" fill="#7FBA00"/>
                    <rect x="1" y="11" width="9" height="9" fill="#00A4EF"/>
                    <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
                </svg>
                <span>{{ loading ? 'Redirigiendo...' : 'Iniciar sesión con Microsoft' }}</span>
            </button>

            <p class="login-note">
                Autenticación vía <strong>Microsoft Entra ID</strong><br>
                OAuth 2.0 / OpenID Connect · Tenant SmartLogix
            </p>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { msalAdmin, loginRequestAdmin } from '../msal.js';

const router  = useRouter();
const loading = ref(false);

onMounted(() => {
    const token = localStorage.getItem('azure_token');
    const rol   = localStorage.getItem('rol');
    if (token && rol === 'ADMIN')   router.push('/admin');
    if (token && rol === 'CLIENTE') router.push('/inicio');
});

async function login() {
    loading.value = true;
    try {
        await msalAdmin.loginRedirect(loginRequestAdmin);
    } catch (e) {
        console.error('Login error:', e);
        loading.value = false;
    }
}
</script>

<style scoped>
.login-page {
    min-height: 100vh;
    display: flex; align-items: center; justify-content: center;
    background: var(--bg-primary); padding: 24px;
}
.login-box {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 20px; padding: 44px 36px;
    width: 100%; max-width: 380px; text-align: center;
    box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}
.login-logo { font-size: 52px; margin-bottom: 12px; }
.login-title { font-size: 1.9rem; margin-bottom: 4px; }
.login-sub { color: var(--text-muted); font-size: 0.9rem; }
.login-divider { height: 1px; background: var(--border); margin: 28px 0; }

.btn-microsoft {
    width: 100%; display: flex; align-items: center; justify-content: center; gap: 12px;
    background: #fff; color: #1a1a2e; border: none;
    border-radius: var(--radius); padding: 13px 20px;
    font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: var(--transition);
}
.btn-microsoft:hover { background: #f0f0f0; transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.btn-microsoft:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.login-note { margin-top: 20px; font-size: 0.78rem; color: var(--text-muted); line-height: 1.6; }
.login-note strong { color: var(--accent); }
</style>