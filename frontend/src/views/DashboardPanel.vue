<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { request } from '../api/http'

defineProps({
  isAdmin: { type: Boolean, default: false }
})

const dashboard = ref(null)
const dashboardError = ref('')
const updatedAt = ref('')
const dashboardLoading = ref(false)
let timer = null

async function loadDashboard() {
  // 手动点击和定时刷新共用同一把锁，避免同一时间重复请求数据大屏接口。
  if (dashboardLoading.value) return

  dashboardLoading.value = true
  try {
    // 一次刷新只请求聚合接口，避免并行请求累计触发后端的全局限流。
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
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <section class="dashboard-panel">
    <div class="panel-head">
      <div>
        <h2>{{ isAdmin ? '数据大屏' : '我的学习数据' }}</h2>
        <span class="hint">{{ isAdmin ? '平台实时指标' : '仅统计当前账号的数据' }} · {{ updatedAt || '等待首次加载' }}</span>
      </div>
      <button class="refresh-button" type="button" :disabled="dashboardLoading" @click="loadDashboard">
        {{ dashboardLoading ? '刷新中…' : '刷新数据' }}
      </button>
    </div>

    <p v-if="dashboardError" class="empty error">{{ dashboardError }}</p>
    <p v-else-if="!dashboard" class="empty">正在加载平台指标…</p>

    <template v-else>
      <div v-if="isAdmin" class="metric-grid admin-metrics">
        <article class="metric-card"><span>累计用户</span><b>{{ dashboard.overview.totalUsers }}</b></article>
        <article class="metric-card"><span>今日活跃</span><b>{{ dashboard.overview.todayActive }}</b></article>
      </div>

      <div v-else class="metric-grid student-metrics">
        <article class="metric-card"><span>今日学习时长</span><b>{{ dashboard.todayMinutes }} <small>分钟</small></b></article>
      </div>

      <div class="dashboard-grid">
        <article v-if="isAdmin" class="dashboard-card">
          <h3>近 7 日活跃趋势</h3>
          <div class="trend">
            <div v-for="point in dashboard.trend" :key="point.date" class="trend-item">
              <b :style="{ height: Math.max(8, point.active) + 'px' }"></b>
              <span>{{ point.active }}</span>
              <i>{{ point.date }}</i>
            </div>
          </div>
        </article>

        <article v-if="isAdmin" class="dashboard-card">
          <h3>各科目学习时长</h3>
          <p v-if="Object.keys(dashboard.subjects).length === 0" class="muted">暂无带科目的学习日志</p>
          <p v-for="(minutes, subject) in dashboard.subjects" :key="subject" class="subject-line">
            <span>{{ subject }}</span><b>{{ minutes }} 分钟</b>
          </p>
        </article>

        <article class="dashboard-card">
          <h3>任务完成情况</h3>
          <div class="task-summary"><span>已完成</span><b>{{ dashboard.tasks.done }}</b></div>
          <div class="task-summary"><span>待完成</span><b>{{ dashboard.tasks.todo }}</b></div>
        </article>

        <article class="dashboard-card">
          <h3>热门院校 TOP 5</h3>
          <p v-if="dashboard.schools.length === 0" class="muted">暂无院校热度数据</p>
          <ol v-else class="school-list"><li v-for="school in dashboard.schools" :key="school.name"><span>{{ school.name }}</span><b>{{ school.heat }}</b></li></ol>
        </article>

        <article v-if="isAdmin" class="dashboard-card activity-card">
          <h3>实时动态</h3>
          <p v-if="dashboard.activities.length === 0" class="muted">暂无业务动态</p>
          <p v-for="item in dashboard.activities" :key="item" class="activity-line">{{ item }}</p>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.panel-head{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:16px}.panel-head h2{margin:0 0 5px;font-size:18px;color:#16233a}.hint,.muted{font-size:12px;color:#7a869a}.refresh-button{padding:8px 14px;border:1px solid #2f6fe4;border-radius:8px;background:#2f6fe4;color:#fff;font-size:13px;cursor:pointer}.refresh-button:disabled{cursor:wait;opacity:.6}.metric-grid{display:grid;gap:12px;margin-bottom:14px}.admin-metrics{grid-template-columns:repeat(2,minmax(0,1fr));max-width:560px}.student-metrics{grid-template-columns:minmax(0,1fr);max-width:340px}.metric-card,.dashboard-card{padding:16px 18px;background:#fff;border:1px solid #e8edf5;border-radius:12px}.metric-card span{display:block;font-size:12px;color:#7a869a}.metric-card b{display:block;margin-top:12px;color:#2f6fe4;font-size:30px}.metric-card small{font-size:13px;font-weight:500}.dashboard-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.dashboard-card h3{margin:0 0 14px;font-size:15px;color:#16233a}.trend{height:160px;display:flex;align-items:end;justify-content:space-around;border-bottom:1px solid #dfe6f0}.trend-item{height:100%;display:flex;flex:1;align-items:center;justify-content:end;flex-direction:column}.trend-item b{width:22px;background:#4c8bf5;border-radius:4px 4px 0 0}.trend-item span,.trend-item i{margin-top:5px;font-size:12px;color:#2f6fe4;font-style:normal}.subject-line,.task-summary,.school-list li{display:flex;justify-content:space-between;gap:12px;margin:0;padding:9px 0;border-bottom:1px solid #eef1f6;font-size:13px;color:#46526b}.subject-line b,.task-summary b,.school-list b{color:#1c4fb8}.task-summary b{font-size:20px}.school-list{margin:0;padding-left:20px}.school-list li{padding-left:2px}.activity-card{grid-column:span 2}.activity-line{margin:0;padding:9px 0;border-bottom:1px solid #eef1f6;font-size:13px;color:#46526b;word-break:break-word}.error{color:#d0453b}@media(max-width:760px){.admin-metrics,.metric-grid,.dashboard-grid{grid-template-columns:1fr}.activity-card{grid-column:span 1}.panel-head{align-items:flex-start;flex-direction:column}}
</style>
