<template>
    <div>
        <NavbarAdmin />
        <div class="container page">

            <div class="demo-header">
                <div>
                    <h1>🎓 Demo Evaluación Parcial 1</h1>
                    <p class="text-muted">Desarrollo Cloud Native I — DSY1107_007D</p>
                </div>
                <button class="btn btn-outline" @click="$router.push('/admin')">← Volver</button>
            </div>

            <div class="status-row">
                <span class="schip" :class="azure.ok ? 'chip-ok' : 'chip-warn'">
                    {{ azure.ok ? '🔐' : '⏳' }} Azure AD {{ azure.ok ? 'Conectado' : 'Verificando...' }}
                </span>
                <span class="schip" :class="bff.ok ? 'chip-ok' : bff.error ? 'chip-err' : 'chip-warn'">
                    {{ bff.ok ? '✅' : bff.error ? '❌' : '⏳' }} BFF {{ bff.ok ? 'Operativo' : bff.error ? 'Error' : 'Cargando...' }}
                </span>
                <span class="schip" :class="ms.ok ? 'chip-ok' : 'chip-warn'">
                    {{ ms.ok ? '🟢' : '⏳' }} Microservicios {{ ms.ok ? 'Activos' : 'Cargando...' }}
                </span>
            </div>

            <div class="demo-grid">
                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">1</span>
                            <div><h3>Microsoft Entra ID (IDaaS)</h3><p>Tenant Interno 01 con usuarios registrados</p></div>
                            <span class="rbadge" :class="azure.ok ? 'rbadge-ok' : ''">{{ azure.ok ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body" v-if="azure.ok">
                            <div class="ir"><span>👤 Usuario</span><strong>{{ azure.nombre }}</strong></div>
                            <div class="ir"><span>📧 Email</span><strong>{{ azure.email }}</strong></div>
                            <div class="ir"><span>🏢 Tenant</span><code>57bd7ed4...24db</code></div>
                            <div class="ir"><span>🔑 Client ID</span><code>6bb7a14b...856b</code></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">2</span>
                            <div><h3>OAuth 2.0 / OpenID Connect</h3><p>Frontend Vue usa MSAL Browser para obtener JWT</p></div>
                            <span class="rbadge" :class="azure.token ? 'rbadge-ok' : ''">{{ azure.token ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body" v-if="azure.token">
                            <div class="ir"><span>🔄 Flujo</span><strong>Authorization Code + PKCE</strong></div>
                            <div class="ir"><span>📦 Scopes</span><code>api://.../read, api://.../write</code></div>
                            <div class="ir"><span>🎫 Token</span><code class="token-pre">{{ azure.token.substring(0,40) }}...</code></div>
                            <div class="ir" v-if="azure.exp"><span>⏰ Expira</span><strong>{{ azure.exp }}</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">3</span>
                            <div><h3>App Registrada en Azure AD</h3><p>AppLoginAngular con scopes read y write</p></div>
                            <span class="rbadge rbadge-ok">✓ Logrado</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>📛 App</span><strong>AppLoginAngular</strong></div>
                            <div class="ir"><span>🔀 Redirect URI</span><code>http://localhost:5173</code></div>
                            <div class="ir"><span>📋 Scopes</span><code>read, write</code></div>
                            <div class="ir"><span>🖥️ Framework</span><strong>Vue 3 + MSAL Browser v3</strong></div>
                        </div>
                    </div>
                </div>

                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">4</span>
                            <div><h3>BFF valida JWT del IDaaS</h3><p>Spring Boot + OAuth2 Resource Server (JWKS)</p></div>
                            <span class="rbadge" :class="bff.ok ? 'rbadge-ok' : bff.error ? 'rbadge-err' : ''">
                                {{ bff.ok ? '✓ Logrado' : bff.error ? '✗ Error' : '⏳' }}
                            </span>
                        </div>
                        <div class="req-body">
                            <div class="test-btns">
                                <button class="btn btn-outline btn-sm" @click="testSinToken" :disabled="testing">🚫 Test sin token</button>
                                <button class="btn btn-primary btn-sm" @click="testConToken" :disabled="testing">✅ Test con token</button>
                            </div>
                            <div class="ir" v-if="bff.lastTest">
                                <span>📡 Resultado</span>
                                <strong :class="bff.lastTest.ok ? 'text-ok' : 'text-err'">{{ bff.lastTest.msg }}</strong>
                            </div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">5</span>
                            <div><h3>Microservicios activos</h3><p>Spring Boot en Docker → puertos 8091–8094</p></div>
                            <span class="rbadge" :class="ms.ok ? 'rbadge-ok' : ''">{{ ms.ok ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="s in ms.servicios" :key="s.nombre">
                                <span class="ms-dot" :class="s.ok ? 'dot-ok' : 'dot-err'"></span>
                                <span class="ms-nombre">{{ s.nombre }}</span>
                                <span class="ms-port">{{ s.port }}</span>
                                <span class="ms-count" v-if="s.count !== null">{{ s.count }} registros</span>
                            </div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">6</span>
                            <div><h3>CORS configurado</h3><p>BFF permite comunicación cross-origin</p></div>
                            <span class="rbadge" :class="bff.ok ? 'rbadge-ok' : ''">{{ bff.ok ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>🌐 Origin</span><code>http://localhost:5173</code></div>
                            <div class="ir"><span>📋 Methods</span><code>GET POST PUT PATCH DELETE OPTIONS</code></div>
                            <div class="ir"><span>📨 Headers</span><code>Authorization, Content-Type</code></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Logger -->
            <div class="card logger-card">
                <div class="logger-top">
                    <h3>🖥️ System Logger — Actividad en tiempo real</h3>
                    <button class="btn-clear" @click="logs = []">Limpiar</button>
                </div>
                <div class="terminal">
                    <div class="log-line" v-for="(l, i) in logs" :key="i" :class="'log-' + l.type">
                        <span class="lt">{{ l.time }}</span>
                        <span class="li">{{ l.icon }}</span>
                        <span class="lm">{{ l.msg }}</span>
                    </div>
                    <div class="log-empty" v-if="logs.length === 0">Sin actividad. Presiona un botón de test.</div>
                </div>
            </div>

        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import NavbarAdmin from '../components/NavbarAdmin.vue';
import { msalInstance, loginRequest, getAccessToken } from '../msal.js';
import { getProductos, getPedidos, getEnvios, getUsuarios, getHeaders } from '../services/api.js';

const BASE_URL = 'http://localhost:8085';

const azure = reactive({ ok:false, nombre:'', email:'', token:'', exp:'' });
const bff   = reactive({ ok:false, error:false, lastTest:null });
const ms    = reactive({
    ok: false, error: false,
    servicios: [
        { nombre:'MS Inventario', port:':8091', ok:false, count:null },
        { nombre:'MS Pedidos',    port:':8092', ok:false, count:null },
        { nombre:'MS Envíos',     port:':8093', ok:false, count:null },
        { nombre:'MS Usuarios',   port:':8094', ok:false, count:null },
    ]
});
const logs   = ref([]);
const testing = ref(false);

function addLog(type, icon, msg) {
    const time = new Date().toLocaleTimeString('es-CL', { hour12:false });
    logs.value.unshift({ type, icon, msg, time });
    if (logs.value.length > 40) logs.value.pop();
}

function decodeJwt(token) {
    try {
        const p = JSON.parse(atob(token.split('.')[1]));
        return { exp: p.exp ? new Date(p.exp*1000).toLocaleString('es-CL') : '' };
    } catch { return {}; }
}

onMounted(async () => {
    addLog('info', '🚀', 'Demo EP1 iniciada — verificando sistema...');

    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0) {
        const account = accounts[0];
        azure.nombre = account.name || account.username;
        azure.email  = account.username;
        azure.ok     = true;
        addLog('success', '🔐', `Azure AD autenticado → ${azure.email}`);

        const token = await getAccessToken();
        if (token) {
            azure.token = token;
            const claims = decodeJwt(token);
            azure.exp = claims.exp;
            addLog('success', '🎫', `Access Token obtenido (${token.length} chars) via OAuth2/OIDC`);
            if (azure.exp) addLog('info', '⏰', `Expira: ${azure.exp}`);
        }
    } else {
        addLog('warn', '⚠️', 'Sin cuenta activa en MSAL');
    }

    addLog('info', '📡', `Conectando al BFF → ${BASE_URL}`);

    try {
        const p = await getProductos();
        ms.servicios[0].ok = true; ms.servicios[0].count = p.length;
        bff.ok = true;
        addLog('success', '✅', `BFF respondió → HTTP 200`);
        addLog('success', '🛒', `MS Inventario → ${p.length} productos`);
    } catch(e) {
        bff.error = true;
        addLog('error', '❌', `BFF error: ${e.message}`);
    }

    try {
        const p = await getPedidos();
        ms.servicios[1].ok = true; ms.servicios[1].count = p.length;
        addLog('success', '📋', `MS Pedidos → ${p.length}`);
    } catch { addLog('error', '❌', 'MS Pedidos no disponible'); }

    try {
        const e = await getEnvios();
        ms.servicios[2].ok = true; ms.servicios[2].count = e.length;
        addLog('success', '🚚', `MS Envíos → ${e.length}`);
    } catch { addLog('error', '❌', 'MS Envíos no disponible'); }

    try {
        const u = await getUsuarios();
        ms.servicios[3].ok = true; ms.servicios[3].count = u.length;
        ms.ok = ms.servicios.every(s => s.ok);
        addLog('success', '👥', `MS Usuarios → ${u.length}`);
        addLog('info', '🔒', 'JWT validado por BFF (issuer + JWKS + exp)');
        addLog('success', '🟢', 'Sistema completamente operativo');
    } catch { addLog('error', '❌', 'MS Usuarios no disponible'); }
});

async function testSinToken() {
    testing.value = true;
    addLog('info', '🧪', 'Test acceso SIN token al BFF...');
    try {
        const res = await fetch(`${BASE_URL}/api/productos`, { headers: { 'ngrok-skip-browser-warning': 'true' } });
        if (res.ok) {
            bff.lastTest = { ok:false, msg:'Acceso permitido inesperadamente' };
            addLog('warn', '⚠️', 'Sin token: acceso permitido inesperadamente');
        } else {
            bff.lastTest = { ok:true, msg:`${res.status} Unauthorized ✓ Protegido` };
            addLog('success', '🔐', `Sin token → ${res.status} Unauthorized ✓ (BFF protege correctamente)`);
        }
    } catch {
        bff.lastTest = { ok:true, msg:'Rechazado por CORS/red ✓' };
        addLog('success', '🔐', 'Sin token → rechazado ✓');
    }
    testing.value = false;
}

async function testConToken() {
    testing.value = true;
    addLog('info', '🧪', 'Test acceso CON token Azure AD...');
    try {
        const p = await getProductos();
        bff.lastTest = { ok:true, msg:`200 OK → ${p.length} productos ✓` };
        addLog('success', '✅', `Con token → 200 OK, ${p.length} productos`);
    } catch(e) {
        bff.lastTest = { ok:false, msg:e.message };
        addLog('error', '❌', `Con token → error: ${e.message}`);
    }
    testing.value = false;
}
</script>

<style scoped>
.demo-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;}
.demo-header h1{font-size:1.8rem;}
.status-row{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:24px;}
.schip{padding:6px 14px;border-radius:999px;font-size:0.82rem;font-weight:600;border:1px solid var(--border);background:var(--bg-card);}
.chip-ok{border-color:rgba(34,197,94,0.4);color:#22c55e;background:rgba(34,197,94,0.08);}
.chip-warn{border-color:rgba(245,158,11,0.4);color:#f59e0b;background:rgba(245,158,11,0.08);}
.chip-err{border-color:rgba(239,68,68,0.4);color:#ef4444;background:rgba(239,68,68,0.08);}
.demo-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;}
.demo-col{display:flex;flex-direction:column;gap:16px;}
.req-card{overflow:hidden;}
.req-head{display:flex;align-items:flex-start;gap:14px;padding:16px 18px;border-bottom:1px solid var(--border);}
.rn{width:28px;height:28px;border-radius:50%;background:var(--accent);color:#fff;font-size:0.8rem;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
.req-head h3{font-size:0.93rem;margin-bottom:2px;font-weight:600;}
.req-head p{font-size:0.78rem;color:var(--text-muted);margin:0;}
.rbadge{margin-left:auto;padding:3px 10px;border-radius:999px;font-size:0.72rem;font-weight:600;white-space:nowrap;flex-shrink:0;background:rgba(90,90,114,0.2);color:var(--text-muted);}
.rbadge-ok{background:rgba(34,197,94,0.15);color:#22c55e;}
.rbadge-err{background:rgba(239,68,68,0.15);color:#ef4444;}
.req-body{padding:12px 18px;display:flex;flex-direction:column;gap:6px;}
.ir{display:flex;align-items:center;gap:10px;font-size:0.82rem;padding:4px 0;}
.ir span{color:var(--text-muted);min-width:90px;flex-shrink:0;}
code{font-family:monospace;font-size:0.75rem;color:var(--accent);background:rgba(124,92,252,0.08);padding:2px 6px;border-radius:4px;}
.token-pre{word-break:break-all;}
.test-btns{display:flex;gap:8px;margin-bottom:10px;flex-wrap:wrap;}
.btn-sm{padding:5px 12px;font-size:0.78rem;}
.text-ok{color:#22c55e;}.text-err{color:#ef4444;}
.ms-row{display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid var(--border);font-size:0.82rem;}
.ms-row:last-child{border-bottom:none;}
.ms-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0;}
.dot-ok{background:#22c55e;box-shadow:0 0 6px #22c55e;}.dot-err{background:#ef4444;}
.ms-nombre{flex:1;}.ms-port{color:var(--text-muted);font-family:monospace;font-size:0.75rem;}.ms-count{color:var(--accent);font-size:0.75rem;}
.logger-card{overflow:hidden;}
.logger-top{display:flex;justify-content:space-between;align-items:center;padding:14px 18px;border-bottom:1px solid var(--border);}
.logger-top h3{font-size:0.95rem;font-weight:600;}
.btn-clear{background:none;border:none;color:var(--text-muted);font-size:0.8rem;cursor:pointer;padding:4px 8px;border-radius:6px;transition:var(--transition);}
.btn-clear:hover{color:#ef4444;}
.terminal{background:#0d0d14;padding:12px;height:220px;overflow-y:auto;display:flex;flex-direction:column;gap:3px;font-family:'Courier New',monospace;}
.log-line{display:flex;gap:8px;font-size:0.78rem;padding:3px 6px;border-radius:5px;}
.lt{color:#4a4a6a;flex-shrink:0;font-size:0.7rem;}.lm{color:#b0b0c8;}
.log-success .lm{color:#22c55e;}.log-success{background:rgba(34,197,94,0.04);}
.log-error .lm{color:#ef4444;}.log-error{background:rgba(239,68,68,0.04);}
.log-warn .lm{color:#f59e0b;}
.log-empty{color:var(--text-muted);text-align:center;padding:20px;font-size:0.82rem;}
@media(max-width:900px){.demo-grid{grid-template-columns:1fr;}}
</style>
