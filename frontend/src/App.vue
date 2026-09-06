<template>
  <SplashScreen />
  <router-view />
  <ToastNotification ref="toast" />
  <ConfirmModal ref="confirm" />
</template>

<script>
import SplashScreen from "./components/SplashScreen.vue";
import ToastNotification from "./components/ToastNotification.vue";
import ConfirmModal from "./components/ConfirmModal.vue";

export default {
  components: { SplashScreen, ToastNotification, ConfirmModal },
  mounted() {
    window.$toast = this.$refs.toast;
    window.$confirm = this.$refs.confirm;

    // Aplicar tema guardado al arrancar
    const tema = localStorage.getItem("tema") || "dark";
    const val = tema === "light" ? "light" : "";
    document.documentElement.setAttribute("data-theme", val);
    document.body.setAttribute("data-theme", val);
  }
}
</script>