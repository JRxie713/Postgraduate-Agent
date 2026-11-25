<script setup>
import { computed, onMounted, ref } from 'vue'
import { createUser, currentUser, deleteUser, logout, updateUser, userList } from '../api/auth'
import {
  createPlan,
  createPlanTask,
  createPolicy,
  deletePlan,
  deletePlanTask,
  myPlans,
  policyList,
  updateTaskStatus
} from '../api/study'
import { REFRESH_KEY, TOKEN_KEY } from '../api/http'
import FileTransferView from './FileTransferView.vue'

const emit = defineEmits(['logout'])

const tab = ref('plan')
const user = ref(null)
const message = ref('')

// ===== 学习计划 =====
const plans = ref([])
const planLoading = ref(false)
const newPlan = ref({ planCycle: '本周', startDate: '', endDate: '' })
const taskDrafts = ref({})
const saving = ref(false)

// ===== 政策资讯 =====
const policies = ref([])
const policyLoading = ref(false)
const policyType = ref('')
const policyTypes = ['', '招生', '考试', '政策', '院校']

// ===== 用户管理（管理员） =====
const users = ref([])
const userLoading = ref(false)
const userTotal = ref(0)
const userPage = ref(1)
const userPageSize = 10
const userKeyword = ref('')
const userRoleFilter = ref('')
const creatingUser = ref(false)
const newUser = ref({ userName: '', pwd: '', realName: '', userRole: 'TEACHER', phone: '', email: '' })

// ===== 资讯发布（教师 / 管理员） =====
const policyForm = ref({ title: '', noticeType: '招生', noticeSource: '研招网', effectTime: '', subTags: '' })
const publishing = ref(false)

const roleText = (role) => ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[role] || role || '学生')
const isAdmin = computed(() => !!user.value && user.value.userRole === 'ADMIN')
const canPublishPolicy = computed(() => !!user.value && ['ADMIN', 'TEACHER'].includes(user.value.userRole))

const todayText = computed(() => new Date().toLocaleDateString('zh-CN'))

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function toDateTime(value) {
  return value ? `${value}T00:00:00` : null
}

function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '未设置'
}

async function loadUser() {
  const data = await currentUser()
  if (data.code === 200) {
    user.value = data.data
  } else {
    message.value = data.message || '获取用户信息失败'
  }
}

async function loadPlans() {
  planLoading.value = true
  const data = await myPlans()
  if (data.code === 200) {
    plans.value = data.data || []
    for (const plan of plans.value) {
      if (!taskDrafts.value[plan.id]) {
        taskDrafts.value[plan.id] = { taskName: '', taskSubject: '政治', dueDate: '' }
      }
    }
  } else {
    message.value = data.message || '加载学习计划失败'
  }
  planLoading.value = false
}

async function submitPlan() {
  if (saving.value) return
  message.value = ''
  saving.value = true
  try {
    const data = await createPlan({
      planCycle: newPlan.value.planCycle,
      startDate: toDateTime(newPlan.value.startDate),
      endDate: toDateTime(newPlan.value.endDate)
    })
    if (data.code === 200) {
      message.value = '学习计划创建成功，可以开始添加任务了'
      await loadPlans()
    } else {
      message.value = data.message || '创建失败'
    }
  } finally {
    saving.value = false
  }
}

async function addTask(planId) {
  const draft = taskDrafts.value[planId]
  if (!draft || !draft.taskName.trim()) {
    message.value = '请填写任务名称'
    return
  }
  const data = await createPlanTask({
    planId,
    taskName: draft.taskName.trim(),
    taskSubject: draft.taskSubject,
    dueDate: toDateTime(draft.dueDate)
  })
  if (data.code === 200) {
    draft.taskName = ''
    draft.dueDate = ''
    await loadPlans()
  } else {
    message.value = data.message || '添加任务失败'
  }
}

async function toggleTask(task) {
  const next = task.taskStatus === 'DONE' ? 'TODO' : 'DONE'
  const data = await updateTaskStatus(task.id, next)
  if (data.code === 200) {
    await loadPlans()
  } else {
    message.value = data.message || '更新任务状态失败'
  }
}

async function removeTask(task) {
  const data = await deletePlanTask(task.id)
  if (data.code === 200) {
    await loadPlans()
  } else {
    message.value = data.message || '删除任务失败'
  }
}

async function removePlan(plan) {
  const data = await deletePlan(plan.id)
  if (data.code === 200) {
    await loadPlans()
  } else {
    message.value = data.message || '删除计划失败'
  }
}

async function loadPolicies() {
  policyLoading.value = true
  const data = await policyList({ page: 1, size: 20, noticeType: policyType.value })
  if (data.code === 200) {
    policies.value = (data.data && data.data.records) || []
  } else {
    message.value = data.message || '加载资讯失败'
  }
  policyLoading.value = false
}

function switchPolicyType(type) {
  policyType.value = type
  loadPolicies()
}

async function submitPolicy() {
  if (publishing.value) return
  if (!policyForm.value.title.trim()) {
    message.value = '请填写资讯标题'
    return
  }
  message.value = ''
  publishing.value = true
  try {
    const data = await createPolicy({
      title: policyForm.value.title.trim(),
      noticeType: policyForm.value.noticeType,
      noticeSource: policyForm.value.noticeSource,
      effectTime: policyForm.value.effectTime ? `${policyForm.value.effectTime}T00:00:00` : null,
      subTags: policyForm.value.subTags
    })
    if (data.code === 200) {
      message.value = '资讯发布成功'
      policyForm.value.title = ''
      policyForm.value.subTags = ''
      await loadPolicies()
    } else {
      message.value = data.message || '发布失败'
    }
  } finally {
    publishing.value = false
  }
}

async function loadUsers() {
  if (!isAdmin.value) return
  userLoading.value = true
  const data = await userList({
    page: userPage.value,
    size: userPageSize,
    keyword: userKeyword.value,
    role: userRoleFilter.value
  })
  if (data.code === 200 && data.data) {
    users.value = data.data.records || []
    userTotal.value = data.data.total || 0
  } else {
    message.value = data.message || '加载用户列表失败'
  }
  userLoading.value = false
}

function switchUserRole(role) {
  userRoleFilter.value = role
  userPage.value = 1
  loadUsers()
}

async function submitNewUser() {
  if (creatingUser.value) return
  if (!newUser.value.userName.trim() || !newUser.value.pwd) {
    message.value = '请填写账号和初始密码'
    return
  }
  message.value = ''
  creatingUser.value = true
  try {
    const data = await createUser({
      userName: newUser.value.userName.trim(),
      pwd: newUser.value.pwd,
      realName: newUser.value.realName,
      userRole: newUser.value.userRole,
      phone: newUser.value.phone,
      email: newUser.value.email
    })
    if (data.code === 200) {
      message.value = `账号 ${data.data.userName} 创建成功，角色：${roleText(data.data.userRole)}`
      newUser.value = { userName: '', pwd: '', realName: '', userRole: 'TEACHER', phone: '', email: '' }
      userPage.value = 1
      await loadUsers()
    } else {
      message.value = data.message || '创建账号失败'
    }
  } finally {
    creatingUser.value = false
  }
}

async function changeUserRole(item, event) {
  const role = event.target.value
  const data = await updateUser(item.id, { userRole: role })
  if (data.code === 200) {
    message.value = `已将 ${item.userName} 的角色改为 ${roleText(role)}`
    await loadUsers()
  } else {
    message.value = data.message || '修改角色失败'
    await loadUsers()
  }
}

async function toggleUserStatus(item) {
  const next = item.userStatus === 'NORMAL' ? 'DISABLED' : 'NORMAL'
  const data = await updateUser(item.id, { userStatus: next })
  if (data.code === 200) {
    message.value = next === 'DISABLED' ? `已禁用 ${item.userName}` : `已启用 ${item.userName}`
    await loadUsers()
  } else {
    message.value = data.message || '修改状态失败'
  }
}

async function removeUser(item) {
  const data = await deleteUser(item.id)
  if (data.code === 200) {
    message.value = `已删除账号 ${item.userName}`
    await loadUsers()
  } else {
    message.value = data.message || '删除账号失败'
  }
}

function turnUserPage(delta) {
  const next = userPage.value + delta
  if (next < 1 || (next - 1) * userPageSize >= userTotal.value) return
  userPage.value = next
  loadUsers()
}

async function doLogout() {
  await logout()
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  emit('logout')
}

onMounted(async () => {
  await Promise.all([loadUser(), loadPlans(), loadPolicies()])
  if (isAdmin.value) {
    await loadUsers()
  }
})
</script>

<template>
  <div class="home">
    <header class="topbar">
      <div class="brand-mini">考研 AI 导学系统</div>
      <div class="topbar-right">
        <span v-if="user" class="user-line">
          {{ user.realName || user.userName }}
          <span class="tag">{{ roleText(user.userRole) }}</span>
        </span>
        <button class="ghost" type="button" @click="doLogout">退出登录</button>
      </div>
    </header>

    <div class="layout">
      <aside class="side">
        <p class="eyebrow">{{ todayText }}</p>
        <h1 class="hello">{{ greeting }}，{{ user ? user.realName || user.userName : '同学' }}</h1>
        <p class="sub">今天也把计划往前推一点。</p>

        <nav class="nav">
          <button :class="['nav-item', { active: tab === 'plan' }]" type="button" @click="tab = 'plan'">
            <span class="nav-title">学习计划</span>
            <span class="nav-desc">拆任务、看进度</span>
          </button>
          <button :class="['nav-item', { active: tab === 'policy' }]" type="button" @click="tab = 'policy'">
            <span class="nav-title">政策资讯</span>
            <span class="nav-desc">招生 / 考试 / 院校</span>
          </button>
          <button :class="['nav-item', { active: tab === 'file' }]" type="button" @click="tab = 'file'">
            <span class="nav-title">文件传输</span>
            <span class="nav-desc">分片 / 批量 / 续传</span>
          </button>
          <button
            v-if="isAdmin"
            :class="['nav-item', { active: tab === 'user' }]"
            type="button"
            @click="tab = 'user'"
          >
            <span class="nav-title">用户管理</span>
            <span class="nav-desc">教师 / 管理员账号</span>
          </button>
        </nav>
      </aside>

      <main class="panel">
        <p v-if="message" class="message">{{ message }}</p>

        <!-- 学习计划 -->
        <section v-if="tab === 'plan'">
          <div class="panel-head">
            <h2>学习计划</h2>
            <span class="hint">勾选任务即可自动统计完成率</span>
          </div>

          <form class="create-plan" @submit.prevent="submitPlan">
            <label>
              <span>周期</span>
              <select v-model="newPlan.planCycle">
                <option value="本周">本周</option>
                <option value="本月">本月</option>
                <option value="百日冲刺">百日冲刺</option>
              </select>
            </label>
            <label>
              <span>开始</span>
              <input v-model="newPlan.startDate" type="date" />
            </label>
            <label>
              <span>结束</span>
              <input v-model="newPlan.endDate" type="date" />
            </label>
            <button type="submit" :disabled="saving">新建计划</button>
          </form>

          <p v-if="planLoading" class="empty">加载中…</p>
          <p v-else-if="plans.length === 0" class="empty">
            还没有学习计划，先在上方新建一个，比如「本周」+ 今天到周日。
          </p>

          <article v-for="plan in plans" :key="plan.id" class="plan-card">
            <header class="plan-head">
              <div>
                <h3>{{ plan.planCycle }}</h3>
                <p class="plan-time">
                  {{ formatDate(plan.startDate) }} ~ {{ formatDate(plan.endDate) }}
                  <span class="status">{{ plan.planStatus === 'COMPLETED' ? '已完成' : '进行中' }}</span>
                </p>
              </div>
              <div class="plan-actions">
                <strong>{{ plan.completionRate || 0 }}%</strong>
                <button class="link danger" type="button" @click="removePlan(plan)">删除计划</button>
              </div>
            </header>

            <div class="progress">
              <div class="progress-bar" :style="{ width: (plan.completionRate || 0) + '%' }"></div>
            </div>
            <p class="plan-count">已完成 {{ plan.taskDone }} / {{ plan.taskTotal }} 项任务</p>

            <ul class="tasks">
              <li v-for="task in plan.tasks" :key="task.id" :class="{ done: task.taskStatus === 'DONE' }">
                <label class="check">
                  <input type="checkbox" :checked="task.taskStatus === 'DONE'" @change="toggleTask(task)" />
                  <span class="task-name">{{ task.taskName }}</span>
                </label>
                <span class="task-meta">{{ task.taskSubject }} · 截止 {{ formatDate(task.dueDate) }}</span>
                <button class="link" type="button" @click="removeTask(task)">删除</button>
              </li>
            </ul>

            <div class="add-task">
              <input v-model="taskDrafts[plan.id].taskName" type="text" placeholder="新任务，如：高数第三章习题" />
              <select v-model="taskDrafts[plan.id].taskSubject">
                <option>政治</option>
                <option>英语</option>
                <option>数学</option>
                <option>专业课</option>
                <option>综合</option>
              </select>
              <input v-model="taskDrafts[plan.id].dueDate" type="date" />
              <button type="button" @click="addTask(plan.id)">添加任务</button>
            </div>
          </article>
        </section>

        <!-- 政策资讯 -->
        <section v-else-if="tab === 'policy'">
          <div class="panel-head">
            <h2>政策资讯</h2>
            <span class="hint">研招网与院校官方信息汇总</span>
          </div>

          <form v-if="canPublishPolicy" class="create-plan publish-form" @submit.prevent="submitPolicy">
            <label>
              <span>标题</span>
              <input v-model="policyForm.title" type="text" placeholder="资讯标题" />
            </label>
            <label>
              <span>类型</span>
              <select v-model="policyForm.noticeType">
                <option>招生</option>
                <option>考试</option>
                <option>政策</option>
                <option>院校</option>
              </select>
            </label>
            <label>
              <span>来源</span>
              <input v-model="policyForm.noticeSource" type="text" />
            </label>
            <label>
              <span>生效日期</span>
              <input v-model="policyForm.effectTime" type="date" />
            </label>
            <label>
              <span>标签</span>
              <input v-model="policyForm.subTags" type="text" placeholder="多个用 | 分隔" />
            </label>
            <button type="submit" :disabled="publishing">{{ publishing ? '发布中…' : '发布资讯' }}</button>
          </form>
          <p v-if="canPublishPolicy" class="hint publish-hint">
            当前身份：{{ roleText(user.userRole) }}，可发布 / 编辑资讯；删除资讯仅管理员可操作。
          </p>

          <div class="chips">
            <button
              v-for="type in policyTypes"
              :key="type || 'all'"
              :class="['chip', { active: policyType === type }]"
              type="button"
              @click="switchPolicyType(type)"
            >
              {{ type || '全部' }}
            </button>
          </div>

          <p v-if="policyLoading" class="empty">加载中…</p>
          <p v-else-if="policies.length === 0" class="empty">暂无该类型的资讯</p>

          <article v-for="item in policies" :key="item.id" class="policy-card">
            <div class="policy-top">
              <span class="type-tag">{{ item.noticeType }}</span>
              <span class="source">{{ item.noticeSource }}</span>
              <span class="date">{{ formatDate(item.effectTime) }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p v-if="item.subTags" class="tags">
              <span v-for="tag in item.subTags.split('|')" :key="tag"># {{ tag }}</span>
            </p>
          </article>
        </section>

        <!-- 文件传输演示 -->
        <section v-else-if="tab === 'file'">
          <div class="panel-head">
            <h2>文件传输</h2>
            <span class="hint">大文件分片上传、小文件批量上传、断点续传、进度条与并发下载</span>
          </div>
          <FileTransferView />
        </section>

        <!-- 用户管理（仅管理员） -->
        <section v-else-if="tab === 'user' && isAdmin">
          <div class="panel-head">
            <h2>用户管理</h2>
            <span class="hint">公开注册只能产生学生，教师 / 管理员由这里创建</span>
          </div>

          <form class="create-plan user-form" @submit.prevent="submitNewUser">
            <label>
              <span>账号</span>
              <input v-model="newUser.userName" type="text" placeholder="登录账号" />
            </label>
            <label>
              <span>初始密码</span>
              <input v-model="newUser.pwd" type="password" placeholder="至少 6 位" />
            </label>
            <label>
              <span>姓名</span>
              <input v-model="newUser.realName" type="text" placeholder="真实姓名" />
            </label>
            <label>
              <span>角色</span>
              <select v-model="newUser.userRole">
                <option value="TEACHER">教师</option>
                <option value="ADMIN">管理员</option>
                <option value="STUDENT">学生</option>
              </select>
            </label>
            <label>
              <span>手机号</span>
              <input v-model="newUser.phone" type="tel" maxlength="11" placeholder="选填" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="newUser.email" type="email" placeholder="选填" />
            </label>
            <button type="submit" :disabled="creatingUser">{{ creatingUser ? '创建中…' : '创建账号' }}</button>
          </form>

          <div class="chips user-filter">
            <input v-model="userKeyword" class="search-input" type="text" placeholder="搜索账号 / 姓名 / 手机 / 邮箱" @keyup.enter="loadUsers" />
            <button class="chip" type="button" @click="loadUsers">搜索</button>
            <button :class="['chip', { active: userRoleFilter === '' }]" type="button" @click="switchUserRole('')">全部角色</button>
            <button :class="['chip', { active: userRoleFilter === 'STUDENT' }]" type="button" @click="switchUserRole('STUDENT')">学生</button>
            <button :class="['chip', { active: userRoleFilter === 'TEACHER' }]" type="button" @click="switchUserRole('TEACHER')">教师</button>
            <button :class="['chip', { active: userRoleFilter === 'ADMIN' }]" type="button" @click="switchUserRole('ADMIN')">管理员</button>
          </div>

          <p v-if="userLoading" class="empty">加载中…</p>
          <p v-else-if="users.length === 0" class="empty">没有符合条件的账号</p>

          <div v-else class="user-table">
            <div class="user-row user-head">
              <span>账号 / 姓名</span>
              <span>联系方式</span>
              <span>角色</span>
              <span>状态</span>
              <span>操作</span>
            </div>
            <div v-for="item in users" :key="item.id" class="user-row">
              <span>
                <b>{{ item.userName }}</b>
                <em>{{ item.realName || '未填写' }}</em>
              </span>
              <span class="contact">
                {{ item.phone || '未绑定手机' }}<br />
                {{ item.email || '未绑定邮箱' }}
              </span>
              <span>
                <select :value="item.userRole" @change="changeUserRole(item, $event)">
                  <option value="STUDENT">学生</option>
                  <option value="TEACHER">教师</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </span>
              <span>
                <span :class="['status', item.userStatus === 'NORMAL' ? 'on' : 'off']">
                  {{ item.userStatus === 'NORMAL' ? '正常' : '已禁用' }}
                </span>
              </span>
              <span class="row-actions">
                <button class="link" type="button" @click="toggleUserStatus(item)">
                  {{ item.userStatus === 'NORMAL' ? '禁用' : '启用' }}
                </button>
                <button class="link danger" type="button" @click="removeUser(item)">删除</button>
              </span>
            </div>
          </div>

          <div class="pager">
            <button class="chip" type="button" :disabled="userPage <= 1" @click="turnUserPage(-1)">上一页</button>
            <span>第 {{ userPage }} 页 / 共 {{ userTotal }} 个账号</span>
            <button class="chip" type="button" :disabled="userPage * userPageSize >= userTotal" @click="turnUserPage(1)">下一页</button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: #f4f7fb;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: #ffffff;
  border-bottom: 1px solid #e8edf5;
}
.brand-mini {
  font-weight: 600;
  color: #1c3f7a;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.user-line {
  font-size: 13px;
  color: #46526b;
}
.tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: #e8f0ff;
  color: #1c4fb8;
  font-size: 12px;
}
.ghost {
  padding: 7px 14px;
  border: 1px solid #dde4ee;
  border-radius: 8px;
  background: #ffffff;
  color: #46526b;
  font-size: 13px;
  cursor: pointer;
}
.ghost:hover {
  border-color: #2f6fe4;
  color: #2f6fe4;
}
.layout {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 56px;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
}
.side {
  padding: 22px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 14px;
  height: fit-content;
}
.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 1px;
  color: #2f6fe4;
}
.hello {
  margin: 0;
  font-size: 20px;
  color: #16233a;
}
.sub {
  margin: 8px 0 20px;
  font-size: 13px;
  color: #7a869a;
}
.nav {
  display: grid;
  gap: 10px;
}
.nav-item {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fbfcfe;
  text-align: left;
  cursor: pointer;
}
.nav-item.active {
  border-color: #2f6fe4;
  background: #eef4ff;
}
.nav-title {
  font-size: 14px;
  color: #16233a;
}
.nav-desc {
  font-size: 12px;
  color: #7a869a;
}
.panel {
  min-height: 420px;
}
.message {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  font-size: 13px;
  color: #46526b;
}
.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}
.panel-head h2 {
  margin: 0;
  font-size: 18px;
  color: #16233a;
}
.hint {
  font-size: 12px;
  color: #7a869a;
}
.create-plan {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.create-plan label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #6b7688;
}
.create-plan input,
.create-plan select {
  padding: 8px 10px;
  border: 1px solid #dde4ee;
  border-radius: 8px;
  font-size: 13px;
  color: #16233a;
}
.create-plan button {
  padding: 9px 16px;
  border: none;
  border-radius: 8px;
  background: #2f6fe4;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
}
.create-plan button:disabled {
  opacity: 0.6;
}
.empty {
  margin: 22px 0 0;
  padding: 18px;
  background: #ffffff;
  border: 1px dashed #dde4ee;
  border-radius: 12px;
  font-size: 13px;
  color: #7a869a;
}
.plan-card {
  margin-top: 18px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 14px;
}
.plan-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.plan-head h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #16233a;
}
.plan-time {
  margin: 0;
  font-size: 12px;
  color: #7a869a;
}
.status {
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #e8f0ff;
  color: #1c4fb8;
  font-size: 11px;
}
.plan-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.plan-actions strong {
  font-size: 20px;
  color: #2f6fe4;
}
.progress {
  height: 8px;
  margin: 14px 0 6px;
  background: #eef1f6;
  border-radius: 999px;
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #4c8bf5, #2f6fe4);
  transition: width 0.25s ease;
}
.plan-count {
  margin: 0 0 10px;
  font-size: 12px;
  color: #7a869a;
}
.tasks {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.tasks li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  font-size: 13px;
}
.tasks li.done .task-name {
  text-decoration: line-through;
  color: #a3adbd;
}
.check {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.task-name {
  color: #16233a;
}
.task-meta {
  margin-left: auto;
  font-size: 12px;
  color: #7a869a;
}
.link {
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: #7a869a;
  cursor: pointer;
}
.link.danger {
  color: #d0453b;
}
.add-task {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
.add-task input[type='text'] {
  flex: 1;
  min-width: 180px;
}
.add-task input,
.add-task select {
  padding: 8px 10px;
  border: 1px solid #dde4ee;
  border-radius: 8px;
  font-size: 13px;
}
.add-task button {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  background: #eef4ff;
  color: #1c4fb8;
  font-size: 13px;
  cursor: pointer;
}
.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.chip {
  padding: 6px 14px;
  border: 1px solid #dde4ee;
  border-radius: 999px;
  background: #ffffff;
  color: #46526b;
  font-size: 12px;
  cursor: pointer;
}
.chip.active {
  border-color: #2f6fe4;
  background: #eef4ff;
  color: #1c4fb8;
}
.policy-card {
  margin-bottom: 12px;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.policy-top {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #7a869a;
}
.type-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: #e8f0ff;
  color: #1c4fb8;
}
.date {
  margin-left: auto;
}
.policy-card h3 {
  margin: 8px 0 6px;
  font-size: 15px;
  color: #16233a;
  line-height: 1.5;
}
.tags {
  margin: 0;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #2f6fe4;
}
.publish-form {
  margin-bottom: 12px;
}
.publish-form label:first-child input {
  min-width: 220px;
}
.publish-hint {
  display: block;
  margin-bottom: 14px;
}
.user-form {
  margin-bottom: 6px;
}
.user-filter {
  margin: 16px 0;
  align-items: center;
}
.search-input {
  padding: 7px 12px;
  border: 1px solid #dde4ee;
  border-radius: 999px;
  font-size: 12px;
  min-width: 220px;
}
.user-table {
  margin-top: 6px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  overflow: hidden;
}
.user-row {
  display: grid;
  grid-template-columns: 1.4fr 1.6fr 110px 80px 120px;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #eef1f6;
  font-size: 13px;
  color: #16233a;
}
.user-row:first-child {
  border-top: none;
}
.user-head {
  background: #f7f9fc;
  font-size: 12px;
  color: #7a869a;
}
.user-row b {
  display: block;
  font-weight: 600;
}
.user-row em {
  font-style: normal;
  font-size: 12px;
  color: #7a869a;
}
.user-row .contact {
  font-size: 12px;
  line-height: 1.7;
  color: #7a869a;
}
.user-row select {
  padding: 6px 8px;
  border: 1px solid #dde4ee;
  border-radius: 7px;
  font-size: 12px;
  background: #ffffff;
}
.status {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
}
.status.on {
  background: #ecfdf3;
  color: #067647;
}
.status.off {
  background: #fef3f2;
  color: #b42318;
}
.row-actions {
  display: flex;
  gap: 12px;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
  font-size: 12px;
  color: #7a869a;
}
.pager .chip:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
@media (max-width: 860px) {
  .user-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
  .user-head {
    display: none;
  }
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
