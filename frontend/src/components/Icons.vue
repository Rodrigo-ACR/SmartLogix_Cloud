<template>
    <span class="icon-wrap" :style="{ width: size + 'px', height: size + 'px' }">

        <!-- Flecha diagonal (original) -->
        <svg v-if="name === 'arrow'" :viewBox="'0 0 91 91'" xmlns="http://www.w3.org/2000/svg" :width="size"
            :height="size">
            <path :fill="color"
                d="M5.1,72c0.9,0.5,2,0.7,3.2,0.5c10.2-1.7,20.3-3.5,30.4-5.3c0.7,5.2,1.3,10.4,1.9,15.6c0.4,3.6,5.1,6.7,8,3.3C61,71.9,74.9,59.3,88.8,46.5c2.4-2.3,1.7-6.4-1-8.1C72.7,29.7,58.6,19.7,47,6.6C44,3.3,38.9,6.1,38.7,10c-0.3,6.2-0.7,12.3-1,18.5c-5.2-0.4-10.5-0.7-15.7-1.1c-6-0.4-13-1.9-18.7,0.3c-1.6,0.6-2.1,2.8-0.5,3.8c5.3,3.2,12.4,3,18.4,3.7c6.9,0.8,13.8,1.5,20.7,2.3c2.5,0.3,4.1-2.2,4.4-4.3c0.4-4,0.8-8,1.2-12.1c8.8,8.6,18.8,16,29.3,22.6c-9.7,8.9-19.2,18.2-28,28c-0.4-3.3-0.8-6.6-1.1-10c-0.4-3.3-2.7-5.1-6-4.6c-10.2,1.7-20.4,3.4-30.6,5.1c-0.6-5.6-2-11.1-2.8-16.7c-0.6-4.5-7-2.8-7.4,1c-0.8,7.1,0.2,13.9,0.8,21C1.7,70,3.3,71.5,5.1,72z" />
        </svg>

        <!-- Flecha izquierda (paginación) -->
        <svg v-else-if="name === 'arrow-left'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
            :width="size" :height="size">
            <path d="M14.5 17L9.5 12L12 9.5L14.5 7" :stroke="color" stroke-width="1.5" stroke-linecap="round"
                stroke-linejoin="round" />
            <path d="M14.5 17L9.5 12L12 9.5" :stroke="color" stroke-width="1.5" stroke-linecap="round"
                stroke-linejoin="round" />
        </svg>

        <!-- Flecha derecha (paginación) -->
        <svg v-else-if="name === 'arrow-right'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
            :width="size" :height="size">
            <path d="M9.5 7L14.5 12L12 14.5L9.5 17" :stroke="color" stroke-width="1.5" stroke-linecap="round"
                stroke-linejoin="round" />
            <path d="M9.5 7L14.5 12L12 14.5" :stroke="color" stroke-width="1.5" stroke-linecap="round"
                stroke-linejoin="round" />
        </svg>

        <!-- Icono salir/logout -->
        <svg v-else-if="name === 'logout'" viewBox="0 0 512.175 512.175" xmlns="http://www.w3.org/2000/svg"
            :width="size" :height="size">
            <g transform="translate(1 1)">
                <path :fill="color"
                    d="M383.175,341.945c-2.743,0-4.571-0.914-6.4-2.743c-3.657-3.657-3.657-9.143,0-12.8l109.714-109.714c3.657-3.657,9.143-3.657,12.8,0c3.657,3.657,3.657,9.143,0,12.8L389.575,339.202C387.747,341.03,385.918,341.945,383.175,341.945z" />
                <path :fill="color"
                    d="M492.889,232.23c-2.743,0-4.571-0.914-6.4-2.743L376.775,119.773c-3.657-3.657-3.657-9.143,0-12.8c3.657-3.657,9.143-3.657,12.8,0l109.714,109.714c3.657,3.657,3.657,9.143,0,12.8C497.461,231.316,495.632,232.23,492.889,232.23z" />
                <path :fill="color"
                    d="M492.889,232.23H319.175c-5.486,0-9.143-3.657-9.143-9.143c0-5.486,3.657-9.143,9.143-9.143h173.714c5.486,0,9.143,3.657,9.143,9.143C502.032,228.573,498.375,232.23,492.889,232.23z" />
                <path :fill="color"
                    d="M319.175,442.516H209.461c-5.486,0-9.143-3.657-9.143-9.143V83.202L5.575,21.945c-4.571-1.829-7.314-6.4-6.4-10.971s4.571-7.314,9.143-7.314h310.857c5.486,0,9.143,3.657,9.143,9.143v420.571C328.318,438.859,324.661,442.516,319.175,442.516z" />
                <path :fill="color"
                    d="M209.461,506.516c-0.914,0-1.829,0-2.743,0l-201.143-64c-3.657-1.829-6.4-5.486-6.4-9.143V12.802c0-2.743,1.829-5.486,3.657-7.314c2.743-1.829,5.486-1.829,8.229-1.829l201.143,64c3.657,1.829,6.4,5.486,6.4,9.143v420.571c0,2.743-1.829,5.486-3.657,7.314C213.118,505.602,211.289,506.516,209.461,506.516z" />
            </g>
        </svg>

    </span>
</template>

<script>
export default {
    name: "Icons",
    props: {
        name: { type: String, default: "arrow" },
        size: { type: Number, default: 18 },
        color: { type: String, default: "#7c5cfc" }
    }
}
</script>

<style scoped>
.icon-wrap {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
</style>