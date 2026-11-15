<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import { currentUser } from './api/auth'
import { request, REFRESH_KEY, TOKEN_KEY } from './api/http'

const logged = ref(Boolean(localStorage.getItem(TOKEN_KEY)))
const page = ref('home')
const dashboard = ref(null)
const dashboardError = ref('')
const updatedAt = ref('')
let timer

async function loadDashboard() {
  const paths = ['overview', 'trend?days=7', 'subject-hours', 'task-completion', 'school-hot?limit=5', 'activities?limit=10']
  const results = await Promise.all(paths.map(path => request(`/dashboard/${path}`)))
  if (results.some(item => item.code !== 200)) {
    dashboardError.value = results.find(item => item.code !== 200)?.message || '加载数据大屏失败'
    return
  }
  dashboard.value = { overview: results[0].data, trend: results[1].data, subjects: results[2].data, tasks: results[3].data, schools: results[4].data, activities: results[5].data }
  updatedAt.value = new Date().toLocaleTimeString('zh-CN')
  dashboardError.value = ''
}

function openDashboard() { page.value = 'dashboard'; loadDashboard(); if (!timer) timer = setInterval(loadDashboard, 10000) }
function closeDashboard() { page.value = 'home'; clearInterval(timer); timer = null }
async function onLogin() { logged.value = true; const result = await currentUser(); if (result.code === 200 && result.data?.userRole === 'ADMIN') openDashboard() }
function onLogout() { clearInterval(timer); timer = null; localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(REFRESH_KEY); logged.value = false; page.value = 'home' }
onMounted(async () => { if (!logged.value) return; const result = await currentUser(); if (result.code !== 200) return onLogout(); if (result.data?.userRole === 'ADMIN') openDashboard() })
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <LoginView v-if="!logged" @success="onLogin" />
  <main v-else-if="page === 'dashboard'" class="dashboard">
    <header><div><p>POSTGRADUATE AGENT · LIVE METRICS</p><h1>考研 AI 导学平台数据驾驶舱</h1></div><div class="actions"><span>MySQL 主库 · Redis 实时指标 · {{ updatedAt }}</span><button @click="loadDashboard">刷新</button><button @click="closeDashboard">返回工作台</button><button @click="onLogout">退出</button></div></header>
    <p v-if="dashboardError" class="error">{{ dashboardError }}</p><p v-else-if="!dashboard" class="loading">正在加载真实业务数据…</p>
    <template v-else>
      <section class="cards"><article><span>累计用户</span><b>{{ dashboard.overview.totalUsers }}</b></article><article><span>今日活跃</span><b>{{ dashboard.overview.todayActive }}</b></article><article><span>今日学习分钟</span><b>{{ dashboard.overview.todayMinutes }}</b></article><article><span>今日完成任务</span><b>{{ dashboard.overview.todayTasks }}</b></article></section>
      <section class="grid">
        <article><h2>近 7 日活跃趋势</h2><div class="trend"><div v-for="point in dashboard.trend" :key="point.date"><b :style="{ height: Math.max(8, point.active) + 'px' }"></b><span>{{ point.active }}</span><i>{{ point.date }}</i></div></div></article>
        <article><h2>各科目学习时长</h2><p v-if="Object.keys(dashboard.subjects).length === 0">暂无带科目的学习日志</p><p v-for="(minutes, subject) in dashboard.subjects" :key="subject">{{ subject }}：{{ minutes }} 分钟</p></article>
        <article><h2>任务完成情况</h2><p>已完成：{{ dashboard.tasks.done }}</p><p>待完成：{{ dashboard.tasks.todo }}</p></article>
        <article><h2>热门院校 TOP 5</h2><ol><li v-for="school in dashboard.schools" :key="school.name">{{ school.name }} <b>{{ school.heat }}</b></li></ol></article>
        <article class="activity"><h2>实时动态</h2><p v-if="dashboard.activities.length === 0">暂无业务动态</p><p v-for="item in dashboard.activities" :key="item">{{ item }}</p></article>
      </section>
    </template>
  </main>
  <HomeView v-else @logout="onLogout" />
</template>

<style scoped>
.dashboard{min-height:100vh;padding:28px 4vw;background:#061526;color:#eaf6ff;font-family:"Microsoft YaHei",sans-serif}.dashboard header{display:flex;justify-content:space-between;align-items:end;border-bottom:1px solid #214968;padding-bottom:18px}.dashboard header p{margin:0;color:#4ed7ff;font-size:12px;letter-spacing:1px}.dashboard h1{margin:8px 0;font-size:28px}.actions{display:flex;gap:10px;align-items:center;color:#93bad4;font-size:12px}.actions button{border:1px solid #2f7bad;border-radius:6px;padding:7px 11px;background:transparent;color:#d5f2ff;cursor:pointer}.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:24px 0}.cards article,.grid article{padding:18px;border:1px solid #1d4d70;border-radius:12px;background:#08233dcc}.cards span{display:block;color:#a1c4da;font-size:13px}.cards b{display:block;margin-top:12px;color:#58dcff;font-size:32px}.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.grid h2{margin-top:0;font-size:16px}.trend{height:180px;display:flex;align-items:end;justify-content:space-around;border-bottom:1px solid #315c78}.trend div{height:100%;display:flex;align-items:center;justify-content:end;flex-direction:column}.trend b{width:24px;background:#41cdf4;border-radius:4px 4px 0 0}.trend span,.trend i{margin-top:5px;color:#70dfff;font-size:12px;font-style:normal}.activity{grid-column:span 2}.activity p{padding:6px 0;border-bottom:1px solid #173b58;color:#b6d2e4}.grid li{display:flex;justify-content:space-between;padding:8px;border-bottom:1px solid #173b58}.grid li b{color:#56dcff}.error,.loading{padding:30px;color:#ffb1a9}@media(max-width:750px){.cards,.grid{grid-template-columns:1fr}.activity{grid-column:span 1}.dashboard header{align-items:start;flex-direction:column}.actions{flex-wrap:wrap}}
</style>
