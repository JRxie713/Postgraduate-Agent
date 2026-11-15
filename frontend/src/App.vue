<script setup>
import { ref } from 'vue'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import { TOKEN_KEY, REFRESH_KEY } from './api/http'

const logged = ref(Boolean(localStorage.getItem(TOKEN_KEY)))

function onLogin() {
  logged.value = true
}

function onLogout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  logged.value = false
}
</script>

<template>
  <LoginView v-if="!logged" @success="onLogin" />
  <HomeView v-else @logout="onLogout" />
</template>
