<template>
    <div class="login-page">
        <div class="login-box">
            <div class="login-logo">⚡</div>
            <h1 class="login-title">SmartLogix</h1>
            <p class="login-sub">Sistema de Gestión Logística</p>

            <div class="login-divider"></div>

            <!-- Inicio — selección -->
            <div v-if="modo === 'inicio'">
                <button class="btn-microsoft" @click="login" :disabled="loading">
                    <svg width="20" height="20" viewBox="0 0 21 21">
                        <rect x="1" y="1" width="9" height="9" fill="#F25022"/>
                        <rect x="11" y="1" width="9" height="9" fill="#7FBA00"/>
                        <rect x="1" y="11" width="9" height="9" fill="#00A4EF"/>
                        <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
                    </svg>
                    <span>{{ loading === 'login' ? 'Redirigiendo...' : 'Iniciar sesión con Microsoft' }}</span>
                </button>

                <div class="separador"><span>o</span></div>

                <button class="btn-cliente" @click="modo = 'login-local'">👤 Iniciar sesión como Cliente</button>
                <button class="btn-registro" @click="modo = 'registro'">📝 Registrarse</button>

                <p class="login-note">
                    Admin: <strong>Microsoft Entra ID</strong><br>
                    Cliente: Microsoft o registro local
                </p>
            </div>

            <!-- LOGIN LOCAL -->
            <div v-if="modo === 'login-local'">
                <h3 class="form-title">Iniciar sesión</h3>
                <div class="form-group">
                    <input v-model="correo" type="email" placeholder="Correo electrónico" @keyup.enter="loginLocal"/>
                </div>
                <div class="form-group">
                    <input v-model="password" type="password" placeholder="Contraseña" @keyup.enter="loginLocal"/>
                </div>
                <p class="error-msg" v-if="error">{{ error }}</p>
                <button class="btn-submit" @click="loginLocal" :disabled="loadingLocal">
                    {{ loadingLocal ? 'Entrando...' : 'Iniciar sesión' }}
                </button>
                <button class="btn-back" @click="reset()">← Volver</button>
                <p class="link-change" @click="modo = 'registro'">¿No tienes cuenta? <strong>Regístrate</strong></p>
            </div>

            <!-- REGISTRO LOCAL -->
            <div v-if="modo === 'registro'">
                <h3 class="form-title">Crear cuenta</h3>
                <div class="form-group">
                    <input v-model="reg.nombre" type="text" placeholder="Nombre completo"/>
                </div>
                <div class="form-group">
                    <input v-model="reg.correo" type="email" placeholder="Correo electrónico"/>
                </div>
                <div class="form-group">
                    <input v-model="reg.password" type="password" placeholder="Contraseña"/>
                </div>
                <div class="form-group">
                    <input v-model="reg.telefono" type="text" placeholder="Teléfono (opcional)"/>
                </div>
                <div class="form-group">
                    <input v-model="reg.direccion" type="text" placeholder="Dirección (opcional)"/>
                </div>
                <p class="error-msg" v-if="error">{{ error }}</p>
                <p class="success-msg" v-if="successMsg">{{ successMsg }}</p>
                <button class="btn-submit" @click="registrar" :disabled="loadingLocal">
                    {{ loadingLocal ? 'Registrando...' : 'Crear cuenta' }}
                </button>
                <button class="btn-back" @click="reset()">← Volver</button>
                <p class="link-change" @click="modo = 'login-local'">¿Ya tienes cuenta? <strong>Inicia sesión</strong></p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { msalAdmin, loginRequestAdmin } from '../msal.js';
import { login, register } from '../services/api.js';

const router        = useRouter();
const loading        = ref(null);
const loadingLocal   = ref(false);
const modo           = ref('inicio');
const correo         = ref('');
const password        = ref('');
const error           = ref('');
const successMsg      = ref('');

const reg = reactive({ nombre:'', correo:'', password:'', telefono:'', direccion:'' });

onMounted(() => {
    const token = localStorage.getItem('azure_token') || localStorage.getItem('token');
    const rol   = localStorage.getItem('rol');
    if (token && rol === 'ADMIN')   router.push('/admin');
    if (token && rol === 'CLIENTE') router.push('/inicio');
});

function reset() {
    modo.value = 'inicio';
    error.value = '';
    successMsg.value = '';
    correo.value = '';
    password.value = '';
}

async function login() {
    loading.value = 'login';
    try {
        await msalAdmin.loginRedirect({ ...loginRequestAdmin, prompt: 'select_account' });
    } catch (e) {
        console.error('Login error:', e);
        loading.value = null;
    }
}

async function loginLocal() {
    if (!correo.value || !password.value) { error.value = 'Ingresa correo y contraseña'; return; }
    loadingLocal.value = true;
    error.value = '';
    try {
        const res = await login(correo.value, password.value);
        if (res.token) {
            localStorage.setItem('token',  res.token);
            localStorage.setItem('rol',    res.rol);
            localStorage.setItem('nombre', res.nombre);
            localStorage.setItem('id',     res.id);
            localStorage.setItem('correo', correo.value);
            if (res.rol === 'ADMIN')   router.push('/admin');
            if (res.rol === 'CLIENTE') router.push('/inicio');
        } else {
            error.value = res.mensaje || 'Credenciales incorrectas';
        }
    } catch {
        error.value = 'Error al conectar con el servidor';
    }
    loadingLocal.value = false;
}

async function registrar() {
    if (!reg.nombre || !reg.correo || !reg.password) {
        error.value = 'Nombre, correo y contraseña son obligatorios';
        return;
    }
    loadingLocal.value = true;
    error.value = '';
    successMsg.value = '';
    try {
        const res = await register({
            nombre:    reg.nombre,
            correo:    reg.correo,
            password:  reg.password,
            telefono:  reg.telefono,
            direccion: reg.direccion,
            rol:       'CLIENTE'
        });
        if (res.id || res.correo) {
            successMsg.value = '¡Cuenta creada! Ahora inicia sesión.';
            setTimeout(() => { modo.value = 'login-local'; correo.value = reg.correo; successMsg.value = ''; }, 1500);
        } else {
            error.value = res.mensaje || 'Error al registrarse';
        }
    } catch {
        error.value = 'Error al conectar con el servidor';
    }
    loadingLocal.value = false;
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
    width: 100%; max-width: 400px; text-align: center;
    box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}
.login-logo { font-size: 52px; margin-bottom: 12px; }
.login-title { font-size: 1.9rem; margin-bottom: 4px; }
.login-sub { color: var(--text-muted); font-size: 0.9rem; }
.login-divider { height: 1px; background: var(--border); margin: 28px 0; }
.form-title { font-size: 1.1rem; margin-bottom: 16px; color: var(--text-primary); }

.btn-microsoft {
    width: 100%; display: flex; align-items: center; justify-content: center; gap: 12px;
    background: #fff; color: #1a1a2e; border: none;
    border-radius: var(--radius); padding: 13px 20px;
    font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: var(--transition);
}
.btn-microsoft:hover { background: #f0f0f0; transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.btn-microsoft:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.separador {
    display: flex; align-items: center; gap: 12px;
    margin: 16px 0; color: var(--text-muted); font-size: 0.82rem;
}
.separador::before, .separador::after { content: ''; flex: 1; height: 1px; background: var(--border); }

.btn-cliente, .btn-registro {
    width: 100%; padding: 12px 20px; margin-bottom: 10px;
    border-radius: var(--radius); font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: var(--transition);
}
.btn-cliente { background: var(--bg-secondary); color: var(--text-primary); border: 1px solid var(--border); }
.btn-cliente:hover { border-color: var(--accent); color: var(--accent); }
.btn-registro { background: transparent; color: var(--accent); border: 1px solid var(--accent); }
.btn-registro:hover { background: rgba(124,92,252,0.1); }

.form-group { margin-bottom: 10px; }
.form-group input {
    width: 100%; padding: 11px 14px;
    background: var(--bg-secondary); border: 1px solid var(--border);
    border-radius: var(--radius); color: var(--text-primary);
    font-size: 0.9rem; transition: var(--transition); text-align: left;
}
.form-group input:focus { outline: none; border-color: var(--accent); }

.btn-submit {
    width: 100%; padding: 12px; margin-top: 4px;
    background: var(--accent); color: #fff; border: none;
    border-radius: var(--radius); font-size: 0.95rem; font-weight: 600;
    cursor: pointer; transition: var(--transition);
}
.btn-submit:hover { background: var(--accent-hover); }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-back {
    background: none; border: none; color: var(--text-muted);
    font-size: 0.85rem; cursor: pointer; padding: 8px; margin-top: 6px;
    transition: var(--transition); display: block; width: 100%;
}
.btn-back:hover { color: var(--text-primary); }

.error-msg { color: var(--danger); font-size: 0.85rem; text-align: left; margin-bottom: 6px; }
.success-msg { color: #22c55e; font-size: 0.85rem; margin-bottom: 6px; }
.link-change { margin-top: 14px; font-size: 0.82rem; color: var(--text-muted); cursor: pointer; }
.link-change strong { color: var(--accent); }
.login-note { margin-top: 20px; font-size: 0.78rem; color: var(--text-muted); line-height: 1.6; }
.login-note strong { color: var(--accent); }
</style>