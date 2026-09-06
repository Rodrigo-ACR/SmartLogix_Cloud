<template>
    <transition name="splash-fade">
        <div v-if="visible" class="splash">
            <div class="splash-content">
                <div class="splash-logo">
                    <span class="splash-bolt">⚡</span>
                    <span class="splash-nombre">SmartLogix</span>
                </div>
                <p class="splash-sub">Plataforma inteligente de gestión logística</p>
                <div class="splash-bar">
                    <div class="splash-bar-fill"></div>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
export default {
    name: "SplashScreen",
    data() {
        return { visible: true };
    },
    mounted() {
        setTimeout(() => { this.visible = false; }, 2200);
    }
}
</script>

<style scoped>
.splash {
    position: fixed;
    inset: 0;
    background: #0a0a0f;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
}

.splash-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
}

.splash-logo {
    display: flex;
    align-items: center;
    gap: 12px;
    animation: splashPop 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.splash-bolt {
    font-size: clamp(1.8rem, 4vw, 3rem);
    animation: boltSpin 0.6s ease 0.2s both;
}

.splash-nombre {
    font-family: 'Syne', sans-serif;
    font-size: clamp(1.6rem, 5vw, 2.8rem);
    font-weight: 700;
    color: #f0f0f8;
    letter-spacing: -0.02em;
}

.splash-sub {
    font-size: 0.95rem;
    color: #5a5a72;
    animation: fadeUp 0.5s ease 0.5s both;
}

.splash-bar {
    width: clamp(120px, 40vw, 200px);
    height: 3px;
    background: #1e1e2a;
    border-radius: 99px;
    overflow: hidden;
    animation: fadeUp 0.5s ease 0.6s both;
}

.splash-bar-fill {
    height: 100%;
    width: 0%;
    background: linear-gradient(90deg, #7c5cfc, #00ff88);
    border-radius: 99px;
    animation: barLoad 1.4s ease 0.7s forwards;
}

@keyframes splashPop {
    from {
        opacity: 0;
        transform: scale(0.8);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}

@keyframes boltSpin {
    from {
        transform: rotate(-30deg) scale(0.5);
        opacity: 0;
    }

    to {
        transform: rotate(0deg) scale(1);
        opacity: 1;
    }
}

@keyframes fadeUp {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes barLoad {
    from {
        width: 0%;
    }

    to {
        width: 100%;
    }
}

/* Salida suave */
.splash-fade-leave-active {
    transition: opacity 0.5s ease;
}

.splash-fade-leave-to {
    opacity: 0;
}
</style>