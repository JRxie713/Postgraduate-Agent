<script setup>
import { onMounted, ref } from 'vue'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import { currentUser } from './api/auth'
import { REFRESH_KEY, TOKEN_KEY } from './api/http'

const logged = ref(Boolean(localStorage.getItem(TOKEN_KEY)))
function onLogin() { logged.value = true }
function onLogout() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(REFRESH_KEY); logged.value = false }
onMounted(async () => { if (!logged.value) return; const result = await currentUser(); if (result.code !== 200) onLogout() })
</script>

<template>
  <LoginView v-if="!logged" @success="onLogin" />
  <HomeView v-else @logout="onLogout" />
</template>
