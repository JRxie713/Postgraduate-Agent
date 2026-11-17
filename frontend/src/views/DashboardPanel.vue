<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { request } from '../api/http'

const dashboard = ref(null)
const dashboardError = ref('')
const updatedAt = ref('')
const dashboardLoading = ref(false)
let timer

async function loadDashboard() {
  // 手动点击和定时刷新共用同一把锁，避免同一时间重复请求数据大屏接口。
  if (dashboardLoading.value) return

  dashboardLoading.value = true
  try {
    // 一次刷新只请求聚合接口，避免 6 个并行请求累计触发后端的全局限流。
    const result = await request('/dashboard/summary')
    if (result.code !== 200) {
      dashboardError.value = result.message || '加载数据大屏失败'
      return
    }
    dashboard.value = result.data
    updatedAt.value = new Date().toLocaleTimeString('zh-CN')
    dashboardError.value = ''
  } catch (error) {
    dashboardError.value = '加载数据大屏失败，请稍后重试'
  } finally {
    // 请求结束后继续冷却 5 秒，限制手动刷新频率。
    window.setTimeout(() => { dashboardLoading.value = false }, 5000)
  }
}

onMounted(() => {
  loadDashboard()
  timer = window.setInterval(loadDashboard, 5000)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  timer = null
})
</script>

<template>
  <section class="dashboard">
    <header>
      <div>
        <p>POSTGRADUATE AGENT · LIVE METRICS</p>
        <h1>考研 AI 导学平台数据驾驶舱</h1>
      </div>
      <div class="actions">
        <span>MySQL 主库 · Redis 实时指标 · {{ updatedAt }}</span>
        <button :disabled="dashboardLoading" @click="loadDashboard">{{ dashboardLoading ? '刷新中…' : '刷新' }}</button>
      </div>
    </header>
    <p v-if="dashboardError" class="error">{{ dashboardError }}</p>
    <p v-else-if="!dashboard" class="loading">正在加载真实业务数据…</p>
    <template v-else>
      <section class="cards">
        <article><span>累计用户</span><b>{{ dashboard.overview.totalUsers }}</b></article>
        <article><span>今日活跃</span><b>{{ dashboard.overview.todayActive }}</b></article>
        <article><span>今日学习分钟</span><b>{{ dashboard.overview.todayMinutes }}</b></article>
        <article><span>今日完成任务</span><b>{{ dashboard.overview.todayTasks }}</b></article>
      </section>
      <section class="grid">
        <article>
          <h2>近 7 日活跃趋势</h2>
          <div class="trend">
            <div v-for="point in dashboard.trend" :key="point.date">
              <b :style="{ height: Math.max(8, point.active) + 'px' }"></b>
              <span>{{ point.active }}</span>
              <i>{{ point.date }}</i>
            </div>
          </div>
        </article>
        <article>
          <h2>各科目学习时长</h2>
          <p v-if="Object.keys(dashboard.subjects).length === 0">暂无带科目的学习日志</p>
          <p v-for="(minutes, subject) in dashboard.subjects" :key="subject">{{ subject }}：{{ minutes }} 分钟</p>
        </article>
        <article>
          <h2>任务完成情况</h2>
          <p>已完成：{{ dashboard.tasks.done }}</p>
          <p>待完成：{{ dashboard.tasks.todo }}</p>
        </article>
        <article>
          <h2>热门院校 TOP 5</h2>
          <ol><li v-for="school in dashboard.schools" :key="school.name">{{ school.name }} <b>{{ school.heat }}</b></li></ol>
        </article>
        <article class="activity">
          <h2>实时动态</h2>
          <p v-if="dashboard.activities.length === 0">暂无业务动态</p>
          <p v-for="item in dashboard.activities" :key="item">{{ item }}</p>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard{padding:24px 28px;background:#061526;color:#eaf6ff;font-family:"Microsoft YaHei",sans-serif;border-radius:14px}
.dashboard header{display:flex;justify-content:space-between;align-items:end;border-bottom:1px solid #214968;padding-bottom:18px}
.dashboard header p{margin:0;color:#4ed7ff;font-size:12px;letter-spacing:1px}
.dashboard h1{margin:8px 0;font-size:26px}
.actions{display:flex;gap:10px;align-items:center;color:#93bad4;font-size:12px}
.actions button{border:1px solid #2f7bad;border-radius:6px;padding:7px 11px;background:transparent;color:#d5f2ff;cursor:pointer}
.actions button:disabled{cursor:wait;opacity:.6}
.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:24px 0}
.cards article,.grid article{padding:18px;border:1px solid #1d4d70;border-radius:12px;background:#08233dcc}
.cards span{display:block;color:#a1c4da;font-size:13px}
.cards b{display:block;margin-top:12px;color:#58dcff;font-size:32px}
.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}
.grid h2{margin-top:0;font-size:16px}
.trend{height:180px;display:flex;align-items:end;justify-content:space-around;border-bottom:1px solid #315c78}
.trend div{height:100%;display:flex;align-items:center;justify-content:end;flex-direction:column}
.trend b{width:24px;background:#41cdf4;border-radius:4px 4px 0 0}
.trend span,.trend i{margin-top:5px;color:#70dfff;font-size:12px;font-style:normal}
.activity{grid-column:span 2}
.activity p{padding:6px 0;border-bottom:1px solid #173b58;color:#b6d2e4}
.grid li{display:flex;justify-content:space-between;padding:8px;border-bottom:1px solid #173b58}
.grid li b{color:#56dcff}
.error,.loading{padding:30px;color:#ffb1a9}
@media(max-width:750px){.cards,.grid{grid-template-columns:1fr}.activity{grid-column:span 1}.dashboard header{align-items:start;flex-direction:column}.actions{flex-wrap:wrap}}
</style>
