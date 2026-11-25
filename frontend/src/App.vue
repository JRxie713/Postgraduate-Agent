<script setup>
import { defineAsyncComponent, ref } from 'vue'
import LoginView from './views/LoginView.vue'
import { TOKEN_KEY, REFRESH_KEY } from './api/http'

// 首页依赖较多，拆成异步 chunk，首屏只加载登录页
const HomeView = defineAsyncComponent(() => import('./views/HomeView.vue'))

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
  <Suspense v-else>
    <HomeView @logout="onLogout" />
    <template #fallback>
      <div class="boot-loading">页面加载中…</div>
    </template>
  </Suspense>
</template>

<style>
.boot-loading {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
  color: #5b6678;
  font-size: 14px;
  letter-spacing: 1px;
}
</style>
