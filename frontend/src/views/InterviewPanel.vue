<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  abandonInterview,
  finishInterview,
  generateFollowUp,
  getCurrentQuestion,
  getInterviewReport,
  getInterviewSession,
  interviewReminders,
  listInterviewSessions,
  reviewEnglishIntro,
  startInterview,
  submitInterviewAnswer
} from '../api/interview'

const props = defineProps({
  user: { type: Object, default: null }
})

const subTab = ref('train')
const message = ref('')
const busy = ref(false)

const majors = ['计算机', '软件工程', '电子信息', '数学', '经济学', '管理学', '其他']
const trainingTypes = [
  { value: 'comprehensive', label: '综合面试' },
  { value: 'professional', label: '专业面试' },
  { value: 'pressure', label: '压力面试' },
  { value: 'english', label: '英语面试' }
]
const difficulties = [
  { value: 'easy', label: '简单' },
  { value: 'medium', label: '中等' },
  { value: 'hard', label: '困难' }
]

const setup = ref({
  major: '计算机',
  trainingType: 'professional',
  difficulty: 'medium',
  pressureMode: false
})

const session = ref(null)
const turn = ref(null)
const answer = ref('')
const report = ref(null)
const history = ref([])
const historyLoading = ref(false)

const englishText = ref('')
const englishResult = ref(null)

// 超时未作答提醒（每 15 秒轮询一次，对应后端 Quartz 定时任务）
const reminders = ref([])
let reminderTimer = null

async function pollReminders() {
  if (!props.user || props.user.id == null) return
  const data = await interviewReminders(props.user.id)
  if (data.code === 200) {
    reminders.value = data.data || []
  }
  await checkCurrentSession()
}

// 正在作答的会话若被后端自动放弃，立刻退回方向/难度选择页
async function checkCurrentSession() {
  if (!session.value || session.value.id == null) return
  if (session.value.status !== 'ongoing') return
  const detail = await getInterviewSession(session.value.id)
  if (detail.code !== 200 || !detail.data) return
  if (detail.data.status !== 'abandoned') {
    session.value = detail.data
    return
  }
  const abandonedId = session.value.id
  reminders.value = reminders.value.filter((item) => item.sessionId !== abandonedId)
  resetInterview()
  subTab.value = 'train'
  message.value = '该场面试超过设定时间未作答，已自动放弃，请重新选择方向与难度'
  await loadHistory()
}

onMounted(() => {
  pollReminders()
  reminderTimer = setInterval(pollReminders, 15000)
})

onUnmounted(() => {
  if (reminderTimer) clearInterval(reminderTimer)
})

async function resumeReminder(item) {
  await openHistory({ id: item.sessionId, status: 'ongoing' })
  await pollReminders()
}

// 放弃进行中的面试
async function giveUp(sessionId) {
  if (!window.confirm('确定放弃这场面试吗？放弃后不会再提醒，也不会生成报告')) return
  const data = await abandonInterview(sessionId)
  if (data.code !== 200) {
    message.value = data.message || '放弃失败'
    return
  }
  message.value = '已放弃该场面试'
  reminders.value = reminders.value.filter((item) => item.sessionId !== sessionId)
  if (session.value && session.value.id === sessionId) {
    session.value = null
    turn.value = null
    subTab.value = 'train'
  }
  await loadHistory()
  await pollReminders()
}

const typeText = (value) => {
  const hit = trainingTypes.find((item) => item.value === value)
  return hit ? hit.label : value || '—'
}
const difficultyText = (value) => {
  const hit = difficulties.find((item) => item.value === value)
  return hit ? hit.label : value || '—'
}
const statusText = (value) => ({ ongoing: '进行中', finished: '已结束', abandoned: '已放弃' }[value] || value || '—')
const scoreOf = (value) => (value == null ? '—' : Math.round(value * 10) / 10)
const answered = computed(() => !!turn.value && !!turn.value.studentAnswer)

const scoreRows = computed(() => {
  if (!report.value || !report.value.score) return []
  const s = report.value.score
  return [
    { label: '表达能力', value: s.expression },
    { label: '专业知识', value: s.professional },
    { label: '逻辑思维', value: s.logic },
    { label: '英语能力', value: s.english },
    { label: '应变能力', value: s.adaptability }
  ]
})

const turnScores = computed(() => {
  if (!turn.value) return []
  return [
    { label: '表达', value: turn.value.aiExpression },
    { label: '专业', value: turn.value.aiProfessional },
    { label: '逻辑', value: turn.value.aiLogic },
    { label: '应变', value: turn.value.aiAdaptability }
  ]
})

async function beginInterview() {
  if (!props.user || props.user.id == null) {
    message.value = '登录状态已失效，请重新登录'
    return
  }
  busy.value = true
  message.value = ''
  try {
    const data = await startInterview({
      userId: props.user.id,
      major: setup.value.major,
      trainingType: setup.value.trainingType,
      difficulty: setup.value.difficulty,
      pressureMode: setup.value.pressureMode
    })
    if (data.code === 200) {
      session.value = data.data
      report.value = null
      turn.value = null
      answer.value = ''
      await loadQuestion()
    } else {
      message.value = data.message || '开始面试失败'
    }
  } finally {
    busy.value = false
  }
}

async function loadQuestion() {
  if (!session.value) return
  const data = await getCurrentQuestion(session.value.id)
  if (data.code === 200) {
    turn.value = data.data
    answer.value = ''
  } else {
    message.value = data.message || '获取题目失败'
  }
}

async function submitAnswer() {
  if (!session.value || !turn.value) return
  if (!answer.value.trim()) {
    message.value = '请先填写你的回答'
    return
  }
  busy.value = true
  message.value = ''
  try {
    const data = await submitInterviewAnswer(session.value.id, {
      sessionId: session.value.id,
      roundIndex: turn.value.roundIndex,
      answer: answer.value.trim()
    })
    if (data.code === 200) {
      turn.value = data.data
      session.value.currentRound = data.data.roundIndex
    } else {
      message.value = data.message || '提交回答失败'
    }
  } finally {
    busy.value = false
  }
}

async function askFollowUp() {
  if (!session.value || !turn.value) return
  busy.value = true
  message.value = ''
  try {
    const data = await generateFollowUp(session.value.id, turn.value.roundIndex)
    if (data.code === 200) {
      turn.value = data.data
      answer.value = ''
      session.value.totalRounds = (session.value.totalRounds || 0) + 1
      message.value = '面试官抛出了一个新的追问，请继续作答'
    } else {
      message.value = data.message || '生成追问失败'
    }
  } finally {
    busy.value = false
  }
}

async function nextQuestion() {
  busy.value = true
  message.value = ''
  try {
    await loadQuestion()
  } finally {
    busy.value = false
  }
}

async function endInterview() {
  if (!session.value) return
  busy.value = true
  message.value = ''
  try {
    const data = await finishInterview(session.value.id)
    if (data.code === 200) {
      session.value.status = 'finished'
      await loadReport(session.value.id)
      message.value = '面试已结束，报告生成完毕'
    } else {
      message.value = data.message || '生成报告失败'
    }
  } finally {
    busy.value = false
  }
}

async function loadReport(id) {
  const data = await getInterviewReport(id)
  if (data.code === 200) {
    report.value = data.data
  } else {
    message.value = data.message || '加载报告失败'
  }
}

function resetInterview() {
  session.value = null
  turn.value = null
  report.value = null
  answer.value = ''
  message.value = ''
}

async function loadHistory() {
  if (!props.user || props.user.id == null) return
  historyLoading.value = true
  try {
    const data = await listInterviewSessions(props.user.id)
    history.value = data.code === 200 ? data.data || [] : []
    if (data.code !== 200) message.value = data.message || '加载历史记录失败'
  } finally {
    historyLoading.value = false
  }
}

async function openHistory(item) {
  busy.value = true
  message.value = ''
  try {
    const detail = await getInterviewSession(item.id)
    if (detail.code !== 200) {
      message.value = detail.message || '加载会话失败'
      return
    }
    session.value = detail.data
    report.value = null
    turn.value = null
    subTab.value = 'train'
    if (item.status === 'finished') {
      await loadReport(item.id)
    } else {
      await loadQuestion()
    }
  } finally {
    busy.value = false
  }
}

async function submitEnglish() {
  if (!englishText.value.trim()) {
    message.value = '请先输入英文自我介绍'
    return
  }
  busy.value = true
  message.value = ''
  try {
    const data = await reviewEnglishIntro(englishText.value.trim())
    if (data.code === 200) {
      englishResult.value = data.data
    } else {
      message.value = data.message || '批改失败'
    }
  } finally {
    busy.value = false
  }
}

function switchSub(name) {
  subTab.value = name
  message.value = ''
  if (name === 'history') loadHistory()
}
</script>

<template>
  <section class="interview">
    <div class="panel-head">
      <h2>复试 AI 训练</h2>
      <span class="hint">题库出题 · 大模型点评 · 五维报告 · Quartz 定时提醒</span>
    </div>

    <div class="chips">
      <button :class="['chip', { active: subTab === 'train' }]" type="button" @click="switchSub('train')">模拟面试</button>
      <button :class="['chip', { active: subTab === 'history' }]" type="button" @click="switchSub('history')">历史记录</button>
      <button :class="['chip', { active: subTab === 'english' }]" type="button" @click="switchSub('english')">英语批改</button>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <!-- 超时未作答提醒（后端 Quartz 任务扫描结果） -->
    <div v-if="reminders.length" class="reminder">
      <p v-for="item in reminders" :key="item.sessionId" class="reminder-row">
        <span class="reminder-text">{{ item.message }}</span>
        <button class="reminder-btn" type="button" @click="resumeReminder(item)">继续作答</button>
        <button class="reminder-btn ghost" type="button" @click="giveUp(item.sessionId)">放弃</button>
      </p>
    </div>

    <!-- ===== 模拟面试 ===== -->
    <template v-if="subTab === 'train'">
      <!-- 未开始：配置 -->
      <div v-if="!session" class="card setup">
        <div class="setup-grid">
          <label>
            <span>目标专业</span>
            <select v-model="setup.major">
              <option v-for="m in majors" :key="m" :value="m">{{ m }}</option>
            </select>
          </label>
          <label>
            <span>训练类型</span>
            <select v-model="setup.trainingType">
              <option v-for="t in trainingTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </label>
          <label>
            <span>难度</span>
            <select v-model="setup.difficulty">
              <option v-for="d in difficulties" :key="d.value" :value="d.value">{{ d.label }}</option>
            </select>
          </label>
          <label class="check">
            <input v-model="setup.pressureMode" type="checkbox" />
            <span>开启压力追问</span>
          </label>
        </div>
        <button class="primary" type="button" :disabled="busy" @click="beginInterview">
          {{ busy ? '准备中…' : '开始模拟面试' }}
        </button>
      </div>

      <!-- 进行中 -->
      <div v-else-if="session.status === 'ongoing'" class="card">
        <div class="session-bar">
          <span class="tag">{{ session.major }}</span>
          <span class="tag">{{ typeText(session.trainingType) }}</span>
          <span class="tag">{{ difficultyText(session.difficulty) }}</span>
          <span v-if="session.pressureMode" class="tag danger">压力模式</span>
          <span class="rounds">第 {{ turn ? turn.roundIndex : session.currentRound }} / {{ session.totalRounds }} 轮</span>
        </div>

        <div v-if="turn" class="question">
          <p class="q-label">
            面试官提问
            <span v-if="turn.isFollowup" class="badge">追问</span>
          </p>
          <p class="q-text">{{ turn.question }}</p>
        </div>

        <textarea
          v-if="turn && !answered"
          v-model="answer"
          class="answer"
          rows="6"
          placeholder="请组织语言作答：先给结论，再分点展开，最后总结"
        ></textarea>

        <div v-if="turn && answered" class="feedback">
          <div class="fb-head">
            <span>你的回答</span>
            <span :class="['badge', turn.ruleBased ? 'warn' : 'ok']">
              {{ turn.ruleBased ? '规则化点评' : '大模型点评' }}
            </span>
          </div>
          <p class="fb-answer">{{ turn.studentAnswer }}</p>
          <div class="scores">
            <div v-for="s in turnScores" :key="s.label" class="score-item">
              <span>{{ s.label }}</span>
              <strong>{{ scoreOf(s.value) }}</strong>
            </div>
          </div>
          <p class="fb-text">{{ turn.aiFeedback }}</p>
        </div>

        <div class="actions">
          <template v-if="turn && !answered">
            <button class="primary" type="button" :disabled="busy" @click="submitAnswer">
              {{ busy ? '评分中…' : '提交回答' }}
            </button>
          </template>
          <template v-else-if="turn">
            <button class="ghost" type="button" :disabled="busy" @click="nextQuestion">下一题</button>
            <button class="ghost" type="button" :disabled="busy" @click="askFollowUp">让面试官追问</button>
            <button class="primary" type="button" :disabled="busy" @click="endInterview">结束并生成报告</button>
          </template>
        </div>
      </div>

      <!-- 已结束：报告 -->
      <div v-else class="card">
        <div v-if="report" class="report">
          <div class="report-head">
            <div>
              <h3>面试报告</h3>
              <p class="report-sub">
                {{ session.major }} · {{ typeText(session.trainingType) }} · {{ difficultyText(session.difficulty) }}
                <span :class="['badge', report.ruleBased ? 'warn' : 'ok']">
                  {{ report.ruleBased ? '规则化兜底' : '大模型生成' }}
                </span>
              </p>
            </div>
            <div class="total">
              <strong>{{ scoreOf(report.totalScore) }}</strong>
              <span>总分</span>
            </div>
          </div>

          <div class="dims">
            <div v-for="row in scoreRows" :key="row.label" class="dim">
              <span class="dim-label">{{ row.label }}</span>
              <div class="bar"><div class="bar-in" :style="{ width: (row.value || 0) + '%' }"></div></div>
              <span class="dim-value">{{ scoreOf(row.value) }}</span>
            </div>
          </div>

          <p class="summary">{{ report.summary }}</p>

          <div class="report-cols">
            <div>
              <h4>主要问题</h4>
              <ul><li v-for="(item, i) in report.mainIssues || []" :key="'i' + i">{{ item }}</li></ul>
            </div>
            <div>
              <h4>改进建议</h4>
              <ul><li v-for="(item, i) in report.suggestions || []" :key="'s' + i">{{ item }}</li></ul>
            </div>
          </div>
        </div>
        <p v-else class="empty">报告生成中…</p>

        <button class="primary" type="button" @click="resetInterview">再来一场</button>
      </div>
    </template>

    <!-- ===== 历史记录 ===== -->
    <template v-else-if="subTab === 'history'">
      <p v-if="historyLoading" class="empty">加载中…</p>
      <p v-else-if="!history.length" class="empty">还没有面试记录，先去「模拟面试」开一场吧</p>
      <div v-else class="history">
        <div v-for="item in history" :key="item.id" class="history-row">
          <button class="h-open" type="button" @click="openHistory(item)">
            <span class="h-main">{{ item.major }} · {{ typeText(item.trainingType) }}</span>
            <span class="h-meta">{{ difficultyText(item.difficulty) }} · {{ statusText(item.status) }}</span>
            <span class="h-score">{{ item.score == null ? '—' : scoreOf(item.score) }}</span>
            <span class="h-time">{{ String(item.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
          </button>
          <button
            v-if="item.status === 'ongoing'"
            class="h-abandon"
            type="button"
            @click="giveUp(item.id)"
          >
            放弃
          </button>
        </div>
      </div>
    </template>

    <!-- ===== 英语批改 ===== -->
    <template v-else>
      <div class="card">
        <label class="block-label">英文自我介绍（120-150 词效果最佳）</label>
        <textarea
          v-model="englishText"
          class="answer"
          rows="8"
          placeholder="Good morning professors. I am ..."
        ></textarea>
        <div class="actions">
          <button class="primary" type="button" :disabled="busy" @click="submitEnglish">
            {{ busy ? '批改中…' : '提交批改' }}
          </button>
        </div>
      </div>

      <div v-if="englishResult" class="card">
        <div class="report-head">
          <div>
            <h3>批改结果</h3>
            <p class="report-sub">
              <span :class="['badge', englishResult.ruleBased ? 'warn' : 'ok']">
                {{ englishResult.ruleBased ? '规则化兜底' : '大模型点评' }}
              </span>
            </p>
          </div>
          <div class="total">
            <strong>{{ scoreOf(englishResult.totalScore) }}</strong>
            <span>总分</span>
          </div>
        </div>

        <div class="dims">
          <div class="dim">
            <span class="dim-label">词汇丰富度</span>
            <div class="bar"><div class="bar-in" :style="{ width: (englishResult.vocabularyScore || 0) + '%' }"></div></div>
            <span class="dim-value">{{ scoreOf(englishResult.vocabularyScore) }}</span>
          </div>
          <div class="dim">
            <span class="dim-label">语法准确度</span>
            <div class="bar"><div class="bar-in" :style="{ width: (englishResult.grammarScore || 0) + '%' }"></div></div>
            <span class="dim-value">{{ scoreOf(englishResult.grammarScore) }}</span>
          </div>
          <div class="dim">
            <span class="dim-label">结构清晰度</span>
            <div class="bar"><div class="bar-in" :style="{ width: (englishResult.structureScore || 0) + '%' }"></div></div>
            <span class="dim-value">{{ scoreOf(englishResult.structureScore) }}</span>
          </div>
        </div>

        <p class="summary">{{ englishResult.overallComment }}</p>

        <div class="report-cols">
          <div>
            <h4>语法 / 用词问题</h4>
            <ul>
              <li v-for="(item, i) in englishResult.grammarMistakes || []" :key="'g' + i">{{ item }}</li>
              <li v-if="!(englishResult.grammarMistakes || []).length" class="muted">未发现明显问题</li>
            </ul>
          </div>
          <div>
            <h4>改进建议</h4>
            <ul><li v-for="(item, i) in englishResult.suggestions || []" :key="'es' + i">{{ item }}</li></ul>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.interview { display: grid; gap: 14px; }
.panel-head { display: flex; align-items: baseline; justify-content: space-between; }
.panel-head h2 { margin: 0; font-size: 18px; color: #16233a; }
.hint { font-size: 12px; color: #7a869a; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  padding: 6px 14px; border: 1px solid #dde4ee; border-radius: 999px;
  background: #ffffff; color: #46526b; font-size: 12px; cursor: pointer;
}
.chip.active { border-color: #2f6fe4; background: #eef4ff; color: #1c4fb8; }
.message {
  margin: 0; padding: 10px 12px; border-radius: 9px; background: #ffffff;
  border: 1px solid #e8edf5; font-size: 13px; color: #46526b;
}
.card { padding: 18px; background: #ffffff; border: 1px solid #e8edf5; border-radius: 14px; }
.reminder {
  display: grid; gap: 8px; padding: 12px 14px; border-radius: 12px;
  background: #fffaeb; border: 1px solid #fec84b;
}
.reminder-row { margin: 0; display: flex; align-items: center; gap: 12px; }
.reminder-text { font-size: 13px; color: #b54708; line-height: 1.6; }
.reminder-btn {
  margin-left: auto; flex: none; padding: 6px 14px; border: none; border-radius: 8px;
  background: #dc6803; color: #fff; font-size: 12px; cursor: pointer;
}
.reminder-btn:hover { background: #b54708; }
.reminder-btn.ghost { background: #fff; color: #b54708; border: 1px solid #fec84b; }
.setup-grid { display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-end; }
.setup-grid label { display: grid; gap: 6px; font-size: 12px; color: #6b7688; }
.setup-grid label.check { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #46526b; }
.setup-grid select {
  padding: 8px 10px; border: 1px solid #dde4ee; border-radius: 8px;
  font-size: 13px; color: #16233a; background: #fff;
}
.primary {
  margin-top: 16px; padding: 9px 18px; border: none; border-radius: 8px;
  background: #2f6fe4; color: #fff; font-size: 13px; cursor: pointer;
}
.primary:disabled { opacity: 0.6; cursor: not-allowed; }
.ghost {
  padding: 9px 16px; border: 1px solid #dde4ee; border-radius: 8px;
  background: #fff; color: #1c4fb8; font-size: 13px; cursor: pointer;
}
.session-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.tag { padding: 2px 10px; border-radius: 999px; background: #e8f0ff; color: #1c4fb8; font-size: 12px; }
.tag.danger { background: #fef3f2; color: #b42318; }
.rounds { margin-left: auto; font-size: 12px; color: #7a869a; }
.question { padding: 14px 16px; background: #f7f9fc; border-radius: 12px; }
.q-label { margin: 0 0 8px; font-size: 12px; color: #7a869a; }
.q-text { margin: 0; font-size: 15px; line-height: 1.7; color: #16233a; }
.badge { margin-left: 8px; padding: 1px 8px; border-radius: 999px; font-size: 11px; }
.badge.ok { background: #ecfdf3; color: #067647; }
.badge.warn { background: #fffaeb; color: #b54708; }
.answer {
  width: 100%; margin-top: 14px; padding: 12px 14px; box-sizing: border-box;
  border: 1px solid #dde4ee; border-radius: 10px; font-size: 13px; line-height: 1.7;
  color: #16233a; resize: vertical; font-family: inherit;
}
.feedback { margin-top: 14px; padding: 14px 16px; border: 1px solid #eef1f6; border-radius: 12px; }
.fb-head { display: flex; justify-content: space-between; font-size: 12px; color: #7a869a; }
.fb-answer { margin: 8px 0 12px; font-size: 13px; color: #46526b; line-height: 1.7; white-space: pre-wrap; }
.scores { display: flex; gap: 10px; flex-wrap: wrap; }
.score-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 8px 16px; border-radius: 10px; background: #eef4ff;
}
.score-item span { font-size: 11px; color: #5a6b85; }
.score-item strong { font-size: 16px; color: #2f6fe4; }
.fb-text { margin: 12px 0 0; font-size: 13px; line-height: 1.8; color: #16233a; white-space: pre-wrap; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.report-head { display: flex; justify-content: space-between; align-items: flex-start; }
.report-head h3 { margin: 0 0 6px; font-size: 16px; color: #16233a; }
.report-sub { margin: 0; font-size: 12px; color: #7a869a; }
.total { text-align: right; }
.total strong { display: block; font-size: 30px; color: #2f6fe4; line-height: 1; }
.total span { font-size: 12px; color: #7a869a; }
.dims { display: grid; gap: 10px; margin: 18px 0; }
.dim { display: grid; grid-template-columns: 70px 1fr 46px; align-items: center; gap: 12px; }
.dim-label { font-size: 12px; color: #5a6b85; }
.bar { height: 8px; background: #eef1f6; border-radius: 999px; overflow: hidden; }
.bar-in { height: 100%; background: linear-gradient(90deg, #4c8bf5, #2f6fe4); }
.dim-value { font-size: 13px; color: #2f6fe4; text-align: right; }
.summary {
  margin: 0 0 16px; padding: 14px 16px; border-radius: 12px;
  background: #f7f9fc; font-size: 13px; line-height: 1.8; color: #16233a;
}
.report-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.report-cols h4 { margin: 0 0 8px; font-size: 13px; color: #16233a; }
.report-cols ul { margin: 0; padding-left: 18px; display: grid; gap: 6px; }
.report-cols li { font-size: 13px; line-height: 1.7; color: #46526b; }
.muted { color: #97a1b5; }
.history { display: grid; gap: 10px; }
.history-row {
  display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 8px;
  padding: 6px 10px 6px 6px; background: #fff; border: 1px solid #e8edf5; border-radius: 12px;
}
.history-row:hover { border-color: #2f6fe4; }
.h-open {
  display: grid; grid-template-columns: 1.6fr 1.4fr 60px 130px; align-items: center; gap: 12px;
  padding: 6px 10px; background: none; border: none; text-align: left; cursor: pointer;
  font-size: 13px; color: #16233a;
}
.h-abandon {
  padding: 6px 12px; border: 1px solid #f2c7c3; border-radius: 8px; background: #fff;
  color: #b42318; font-size: 12px; cursor: pointer; white-space: nowrap;
}
.h-abandon:hover { background: #fef3f2; }
.h-main { font-weight: 600; }
.h-meta { font-size: 12px; color: #7a869a; }
.h-score { color: #2f6fe4; text-align: right; }
.h-time { font-size: 12px; color: #97a1b5; text-align: right; }
.block-label { display: block; margin-bottom: 8px; font-size: 12px; color: #6b7688; }
.empty {
  margin: 0; padding: 18px; background: #fff; border: 1px dashed #dde4ee;
  border-radius: 12px; font-size: 13px; color: #7a869a;
}
@media (max-width: 860px) {
  .report-cols { grid-template-columns: 1fr; }
  .h-open { grid-template-columns: 1fr 1fr; }
}
</style>
