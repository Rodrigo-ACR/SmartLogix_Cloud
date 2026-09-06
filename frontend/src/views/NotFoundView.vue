<template>
    <div class="notfound-page">

        <!-- Navbar mínimo -->
        <nav class="nf-nav">
            <div class="nf-nav-inner" @click="irInicio" style="cursor:pointer">
                <span>⚡</span>
                <span class="nf-brand">SmartLogix</span>
            </div>
        </nav>

        <div class="nf-content">

            <!-- Cara animada -->
            <div class="face-wrap">
                <main class="my-custom-face-container">
                    <svg class="face" viewBox="0 0 320 380">
                        <g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                            stroke-width="25">
                            <g class="face__eyes" transform="translate(0,112.5)">
                                <g transform="translate(15,0)">
                                    <polyline class="face__eye-lid" points="37,0 0,120 75,120"></polyline>
                                    <polyline class="face__pupil" points="55,120 55,155" stroke-dasharray="35 35">
                                    </polyline>
                                </g>
                                <g transform="translate(230,0)">
                                    <polyline class="face__eye-lid" points="37,0 0,120 75,120"></polyline>
                                    <polyline class="face__pupil" points="55,120 55,155" stroke-dasharray="35 35">
                                    </polyline>
                                </g>
                            </g>
                            <rect class="face__nose" x="132.5" y="112.5" width="55" height="155" rx="4" ry="4"></rect>
                            <g transform="translate(65,334)" stroke-dasharray="102 102">
                                <path class="face__mouth-left" d="M 0 30 C 0 30 40 0 95 0"></path>
                                <path class="face__mouth-right" d="M 95 0 C 150 0 190 30 190 30"></path>
                            </g>
                        </g>
                    </svg>
                </main>
            </div>

            <!-- Texto -->
            <div class="nf-text">
                <div class="nf-code">404</div>
                <h1>Página no encontrada</h1>
                <p>La ruta que buscas no existe o fue movida.<br>No te preocupes, puedes volver al inicio.</p>

                <div class="nf-actions">
                    <button class="nf-btn-primary" @click="irInicio">
                        ⚡ Ir al inicio
                    </button>
                    <button class="nf-btn-secondary" @click="$router.go(-1)">
                        <Icons name="arrow" :size="16" style="transform:rotate(180deg);margin-right:4px" /> Volver atrás
                    </button>
                </div>
            </div>

        </div>
    </div>
</template>

<script>
import Icons from "../components/Icons.vue";

export default {
    name: "NotFoundView",
    components: { Icons },
    methods: {
        irInicio() {
            const rol = localStorage.getItem("rol");
            if (rol === "ADMIN") this.$router.push("/admin");
            else if (rol === "CLIENTE") this.$router.push("/inicio");
            else this.$router.push("/login");
        }
    }
}
</script>

<style scoped>
.notfound-page {
    min-height: 100vh;
    background: var(--bg-primary);
    display: flex;
    flex-direction: column;
}

/* Navbar */
.nf-nav {
    padding: 1rem 2rem;
    border-bottom: 1px solid var(--border);
    background: rgba(10, 10, 15, 0.8);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    position: sticky;
    top: 0;
    z-index: 50;
}

.nf-nav-inner {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 1.2rem;
    font-weight: 700;
    color: var(--text-primary);
}

.nf-brand {
    color: var(--text-primary);
}

/* Layout principal */
.nf-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4rem;
    padding: 2rem;
    flex-wrap: wrap;
}

/* Cara animada */
.face-wrap {
    flex-shrink: 0;
}

.my-custom-face-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 320px;
    color: var(--accent);
}

.my-custom-face-container .face {
    width: 180px;
}

.my-custom-face-container .face__eyes,
.my-custom-face-container .face__eye-lid,
.my-custom-face-container .face__mouth-left,
.my-custom-face-container .face__mouth-right,
.my-custom-face-container .face__nose,
.my-custom-face-container .face__pupil {
    animation: eyes 1s 0.3s forwards;
}

.my-custom-face-container .face__eye-lid,
.my-custom-face-container .face__pupil {
    animation-duration: 4s;
    animation-delay: 1.3s;
    animation-iteration-count: infinite;
}

.my-custom-face-container .face__eye-lid {
    animation-name: eye-lid;
}

.my-custom-face-container .face__mouth-left {
    animation-name: mouth-left;
}

.my-custom-face-container .face__mouth-right {
    animation-name: mouth-right;
}

.my-custom-face-container .face__nose {
    animation-name: nose;
}

.my-custom-face-container .face__pupil {
    animation-name: pupil;
}

@keyframes eye-lid {

    0%,
    40%,
    45%,
    100% {
        transform: translateY(0);
    }

    42.5% {
        transform: translateY(17.5px);
    }
}

@keyframes eyes {
    from {
        transform: translateY(112.5px);
    }

    to {
        transform: translateY(15px);
    }
}

@keyframes pupil {

    0%,
    37.5%,
    40%,
    45%,
    87.5%,
    100% {
        stroke-dashoffset: 0;
        transform: translate(0, 0);
    }

    12.5%,
    25%,
    62.5%,
    75% {
        transform: translate(-35px, 0);
    }

    42.5% {
        stroke-dashoffset: 35;
        transform: translate(0, 17.5px);
    }
}

@keyframes mouth-left {

    from,
    50% {
        stroke-dashoffset: -102;
    }

    to {
        stroke-dashoffset: 0;
    }
}

@keyframes mouth-right {

    from,
    50% {
        stroke-dashoffset: 102;
    }

    to {
        stroke-dashoffset: 0;
    }
}

@keyframes nose {
    from {
        transform: translate(0, 0);
    }

    to {
        transform: translate(0, 22.5px);
    }
}

/* Texto */
.nf-text {
    text-align: center;
    max-width: 420px;
}

.nf-code {
    font-size: 7rem;
    font-weight: 700;
    line-height: 1;
    background: linear-gradient(135deg, var(--accent), #00ff88);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 0.5rem;
}

.nf-text h1 {
    font-size: 1.8rem;
    color: var(--text-primary);
    margin-bottom: 0.8rem;
}

.nf-text p {
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 2rem;
    font-size: 1rem;
}

.nf-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
}

.nf-btn-primary {
    background: var(--accent);
    color: white;
    border: none;
    padding: 0.75rem 1.5rem;
    border-radius: var(--radius);
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition);
}

.nf-btn-primary:hover {
    background: #6d28d9;
    transform: translateY(-1px);
}

.nf-btn-secondary {
    background: transparent;
    color: var(--text-secondary);
    border: 1px solid var(--border);
    padding: 0.75rem 1.5rem;
    border-radius: var(--radius);
    font-size: 1rem;
    cursor: pointer;
    transition: var(--transition);
}

.nf-btn-secondary:hover {
    border-color: var(--accent);
    color: var(--accent);
}

@media (max-width: 600px) {
    .nf-content {
        gap: 2rem;
        padding: 1rem;
    }

    .nf-code {
        font-size: 5rem;
    }

    .nf-text h1 {
        font-size: 1.4rem;
    }

    .my-custom-face-container {
        height: 240px;
    }

    .my-custom-face-container .face {
        width: 140px;
    }
}
</style>