<template>
    <nav class="navbar">

        <h2 class="logo">
            SmartLogix
        </h2>

        <div v-if="token" class="navbar-right">

            <span class="navbar-role">
                {{ rol }}
            </span>

            <button
                class="btn-danger"
                @click="$emit('logout')"
            >
                Salir
            </button>

        </div>

    </nav>
</template>

<script>
export default {
    props: ["token", "rol"]
}
</script>