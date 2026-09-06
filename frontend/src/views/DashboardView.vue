<template>
    <div>

        <Navbar :token="token" :rol="rol" @logout="logout" />

        <DashboardCards :stats="stats" :rol="rol" @cargar="cargar" />

        <DataTable :datos="datos" @editar="editar" @eliminar="eliminar" />

    </div>
</template>

<script>
import Navbar from "../components/Navbar.vue";
import DashboardCards from "../components/DashboardCards.vue";
import DataTable from "../components/DataTable.vue";
import "@/assets/styles/admindashboardview.css";
import {
    getProductos,
    getPedidos,
    getEnvios,
    editarProducto,
    eliminarProducto
} from "../services/api";

export default {

    components: {
        Navbar,
        DashboardCards,
        DataTable
    },

    data() {
        return {
            token: localStorage.getItem("token"),
            rol: localStorage.getItem("rol"),
            datos: [],

            stats: {
                productos: 0,
                pedidos: 0,
                envios: 0
            }
        };
    },

    mounted() {

        if (!this.token) {
            this.$router.push("/login");
        }

        this.cargarStats();
    },

    methods: {

        logout() {

            localStorage.clear();

            this.$router.push("/login");
        },

        async cargar(tipo) {

            if (tipo === "productos") {
                this.datos = await getProductos();
            }

            if (tipo === "pedidos") {
                this.datos = await getPedidos();
            }

            if (tipo === "envios") {
                this.datos = await getEnvios();
            }
        },

        async cargarStats() {

            this.stats.productos =
                (await getProductos()).length;

            this.stats.pedidos =
                (await getPedidos()).length;

            this.stats.envios =
                (await getEnvios()).length;
        },

        async editar(item) {

            const nombre =
                prompt("Nuevo nombre", item.nombre);

            item.nombre = nombre;

            await editarProducto(item);

            this.cargar("productos");
        },

        async eliminar(id) {

            await eliminarProducto(id);

            this.cargar("productos");
        }
    }
}
</script>