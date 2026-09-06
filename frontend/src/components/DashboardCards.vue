<template>
    <div class="cards">
        <div class="card" @click="$emit('cargar', 'productos')">
            <h3>📦 Productos</h3>
            <p>{{ stats.productos }}</p>
        </div>

        <div class="card" v-if="rol === 'ADMIN'" @click="$emit('cargar', 'pedidos')">
            <h3>🧾 Pedidos</h3>
            <p>{{ stats.pedidos }}</p>
        </div>

        <div class="card" @click="$emit('cargar', 'envios')">
            <h3>🚚 Envíos</h3>
            <p>{{ stats.envios }}</p>
        </div>
    </div>
</template>

<script>
export default {
    props: ["stats", "rol"]
}
</script>