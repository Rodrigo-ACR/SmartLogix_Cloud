<template>

    <div class="login-container">

        <div class="login">

            <h2>Iniciar sesión</h2>

            <input
                v-model="username"
                placeholder="Usuario"
            />

            <input
                v-model="password"
                type="password"
                placeholder="Contraseña"
            />

            <button @click="enviarLogin">
                Entrar
            </button>

        </div>

    </div>

</template>

<script>
export default {

    data() {
        return {
            username: "",
            password: ""
        }
    },

    methods: {

        enviarLogin() {

            this.$emit("login", {
                username: this.username,
                password: this.password
            });
        }
    }
}
</script>