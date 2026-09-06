<template>
    <div class="tabla">
        <table v-if="datos.length > 0">
            <thead>
                <tr>
                    <th v-for="(v, k) in datos[0]" :key="k">
                        {{ k }}
                    </th>
                    <th>Acciones</th>
                </tr>
            </thead>

            <tbody>
                <tr v-for="item in datos" :key="item.id">
                    <td v-for="(v, k) in item" :key="k">
                        <img v-if="k === 'imagenUrl'" :src="v" width="60" />
                        <span v-else>
                            {{ v }}
                        </span>
                    </td>

                    <td>
                        <button @click="$emit('editar', item)">
                            ✏️
                        </button>

                        <button @click="$emit('eliminar', item.id)">
                            🗑️
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>

        <p v-else>
            Selecciona una opción
        </p>
    </div>
</template>

<script>
export default {
    props: ["datos"]
}
</script>