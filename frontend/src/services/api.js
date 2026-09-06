const BASE_URL = "http://localhost:8085";

export const getToken  = () => localStorage.getItem("azure_token") || localStorage.getItem("token");
export const getRol    = () => localStorage.getItem("rol");
export const getNombre = () => localStorage.getItem("nombre");
export const getId     = () => localStorage.getItem("id");

export const getHeaders = () => ({
    "Content-Type": "application/json",
    "Authorization": "Bearer " + getToken(),
    "ngrok-skip-browser-warning": "true"
});

async function fetchJSON(url, options = {}) {
    const res = await fetch(url, options);
    if (!res.ok) {
        const err = await res.json().catch(() => ({ mensaje: "Error del servidor" }));
        throw new Error(err.mensaje || `HTTP ${res.status}`);
    }
    const text = await res.text();
    return text ? JSON.parse(text) : null;
}

export async function login(correo, password) {
    const res = await fetch(`${BASE_URL}/auth/login`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ correo, password })
    });
    return res.json();
}

export async function register(data) {
    const res = await fetch(`${BASE_URL}/api/productos/usuarios/register`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });
    return res.json();
}

export async function getProductos() { return fetchJSON(`${BASE_URL}/api/productos`, { headers: getHeaders() }); }
export async function crearProducto(data) { return fetchJSON(`${BASE_URL}/api/productos`, { method:"POST", headers:getHeaders(), body:JSON.stringify(data) }); }
export async function editarProducto(id, data) { return fetchJSON(`${BASE_URL}/api/productos/${id}`, { method:"PUT", headers:getHeaders(), body:JSON.stringify(data) }); }
export async function eliminarProducto(id) {
    const res = await fetch(`${BASE_URL}/api/productos/${id}`, { method:"DELETE", headers:getHeaders() });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export async function getPedidos() { return fetchJSON(`${BASE_URL}/api/productos/pedidos`, { headers: getHeaders() }); }
export async function crearPedido(data) { return fetchJSON(`${BASE_URL}/api/productos/pedidos`, { method:"POST", headers:getHeaders(), body:JSON.stringify(data) }); }
export async function cambiarEstadoPedido(id, estado) { return fetchJSON(`${BASE_URL}/api/productos/pedidos/${id}/estado?estado=${estado}`, { method:"PATCH", headers:getHeaders() }); }

export async function getEnvios() { return fetchJSON(`${BASE_URL}/api/productos/envios`, { headers: getHeaders() }); }
export async function crearEnvio(data) { return fetchJSON(`${BASE_URL}/api/productos/envios`, { method:"POST", headers:getHeaders(), body:JSON.stringify(data) }); }
export async function cambiarEstadoEnvio(id, estado) { return fetchJSON(`${BASE_URL}/api/productos/envios/${id}/estado?estado=${estado}`, { method:"PATCH", headers:getHeaders() }); }

export async function getUsuarios() { return fetchJSON(`${BASE_URL}/api/productos/usuarios`, { headers: getHeaders() }); }
export async function actualizarUsuario(id, data) { return fetchJSON(`${BASE_URL}/api/productos/usuarios/${id}`, { method:"PUT", headers:getHeaders(), body:JSON.stringify(data) }); }
