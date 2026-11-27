<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  practiceOptions,
  practiceQuestions,
  practiceRecords,
  practiceSimilar,
  practiceSummary,
  submitPracticeAnswer,
  wrongQuestionList,
  updateWrongMastery,
  deleteWrongQuestion
} from '../../api/practice'
import { TOKEN_KEY } from '../../api/http'

const hasToken = ref(Boolean(localStorage.getItem(TOKEN_KEY)))
const tab = ref('practice')
const message = ref('')

// ===== 概览 =====
const summary = ref(null)
async function loadSummary() {
  const res = await practiceSummary()
  if (res.code === 200) summary.value = res.data
}

// ===== 筛选项 =====
const options = reactive({ subjects: [], difficulties: [], chapters: {}, knowledgePoints: {} })
async function loadOptions() {
  const res = await practiceOptions()
  if (res.code === 200 && res.data) {
    options.subjects = res.data.subjects || []
    options.difficulties = res.data.difficulties || []
    options.chapters = res.data.chapters || {}
    options.knowledgePoints = res.data.knowledgePoints || {}
  }
}
const chapterList = computed(() => options.chapters[questionFilter.examSubject] || [])
const pointList = computed(() => options.knowledgePoints[questionFilter.examSubject] || [])
const wrongPointList = computed(() => options.knowledgePoints[wrongFilter.examSubject] || [])

// ===== 练习题 =====
const questionFilter = reactive({ examSubject: '', chapter: '', knowledgePoint: '', difficulty: '', keyword: '' })
const questionPage = reactive({ page: 1, size: 8, total: 0 })
const questions = ref([])
const questionLoading = ref(false)

async function loadQuestions() {
  questionLoading.value = true
  const res = await practiceQuestions({ ...questionFilter, page: questionPage.page, size: questionPage.size })
  if (res.code === 200 && res.data) {
    questions.value = res.data.records || []
    questionPage.total = res.data.total || 0
  } else {
    message.value = res.message || '加载练习题失败'
  }
  questionLoading.value = false
}
function searchQuestions() {
  questionPage.page = 1
  loadQuestions()
}
function resetQuestionFilter() {
  Object.assign(questionFilter, { examSubject: '', chapter: '', knowledgePoint: '', difficulty: '', keyword: '' })
  searchQuestions()
}
function onSubjectChange() {
  questionFilter.chapter = ''
  questionFilter.knowledgePoint = ''
}
function turnQuestionPage(delta) {
  const next = questionPage.page + delta
  if (next < 1 || (next - 1) * questionPage.size >= questionPage.total) return
  questionPage.page = next
  loadQuestions()
}

// ===== 单题练习 =====
const current = ref(null)
const selectedAnswer = ref('')
const answerResult = ref(null)
const submitting = ref(false)
const sessionAnswers = ref([])
const sessionCorrect = computed(() => sessionAnswers.value.filter((a) => a.correct).length)
const sessionAccuracy = computed(() =>
  sessionAnswers.value.length ? Math.round((sessionCorrect.value / sessionAnswers.value.length) * 100) : 0
)
const sessionWrong = computed(() => sessionAnswers.value.filter((a) => !a.correct))

function openQuestion(question) {
  current.value = question
  selectedAnswer.value = ''
  answerResult.value = null
}
function closeQuestion() {
  current.value = null
  answerResult.value = null
}
async function submitAnswer() {
  if (!selectedAnswer.value || submitting.value || !current.value) return
  submitting.value = true
  const res = await submitPracticeAnswer({ questionId: current.value.id, userAnswer: selectedAnswer.value })
  if (res.code === 200) {
    answerResult.value = res.data
    sessionAnswers.value.push({ ...res.data, knowledgePoint: current.value.knowledgePoint, examSubject: current.value.examSubject })
    if (!res.data.correct) current.value.wrong = true
    loadSummary()
  } else {
    message.value = res.message || '提交答案失败'
  }
  submitting.value = false
}
function nextQuestion() {
  const list = questions.value
  const index = list.findIndex((q) => q.id === current.value?.id)
  if (index >= 0 && index + 1 < list.length) {
    openQuestion(list[index + 1])
  } else {
    closeQuestion()
    message.value = '本页题目已练完，可翻页或调整筛选条件继续练习'
  }
}

// ===== 专项练习 =====
const special = ref(null)
async function openSimilar(questionId) {
  special.value = { records: [], loading: true }
  const res = await practiceSimilar(questionId, 6)
  special.value = { records: res.code === 200 ? res.data || [] : [], loading: false, empty: res.code === 200 && !(res.data || []).length }
}
function closeSimilar() {
  special.value = null
}

// ===== 错题本 =====
const wrongFilter = reactive({ examSubject: '', knowledgePoint: '', masteryLevel: '' })
const wrongPage = reactive({ page: 1, size: 8, total: 0 })
const wrongList = ref([])
const wrongLoading = ref(false)
const wrongDetail = ref(null)
const masteryLevels = [
  { value: '', label: '全部状态' },
  { value: 'UNMASTERED', label: '仍不会' },
  { value: 'REVIEWING', label: '复习中' },
  { value: 'MASTERED', label: '已掌握' }
]
const masteryText = (level) => ({ MASTERED: '已掌握', UNMASTERED: '仍不会', REVIEWING: '复习中' }[level] || '仍不会')

async function loadWrong() {
  wrongLoading.value = true
  const res = await wrongQuestionList({ ...wrongFilter, page: wrongPage.page, size: wrongPage.size })
  if (res.code === 200 && res.data) {
    wrongList.value = res.data.records || []
    wrongPage.total = res.data.total || 0
  } else {
    message.value = res.message || '加载错题本失败'
  }
  wrongLoading.value = false
}
function searchWrong() {
  wrongPage.page = 1
  loadWrong()
}
function resetWrongFilter() {
  Object.assign(wrongFilter, { examSubject: '', knowledgePoint: '', masteryLevel: '' })
  searchWrong()
}
function turnWrongPage(delta) {
  const next = wrongPage.page + delta
  if (next < 1 || (next - 1) * wrongPage.size >= wrongPage.total) return
  wrongPage.page = next
  loadWrong()
}
function openWrongDetail(item) {
  wrongDetail.value = item
}
async function markMastery(level) {
  if (!wrongDetail.value) return
  const res = await updateWrongMastery(wrongDetail.value.id, level)
  if (res.code === 200) {
    wrongDetail.value = res.data
    message.value = level === 'MASTERED' ? '已标记为已掌握，可在筛选中查看' : '已更新掌握状态'
    loadWrong()
    loadSummary()
  } else {
    message.value = res.message || '更新失败'
  }
}
async function removeWrong() {
  if (!wrongDetail.value) return
  const res = await deleteWrongQuestion(wrongDetail.value.id)
  if (res.code === 200) {
    wrongDetail.value = null
    message.value = '已从错题本移除'
    loadWrong()
    loadSummary()
  } else {
    message.value = res.message || '删除失败'
  }
}

// ===== 练习记录 =====
const recordPage = reactive({ page: 1, size: 10, total: 0 })
const records = ref([])
const onlyWrong = ref(false)
const recordLoading = ref(false)
async function loadRecords() {
  recordLoading.value = true
  const res = await practiceRecords({ page: recordPage.page, size: recordPage.size, onlyWrong: onlyWrong.value ? true : '' })
  if (res.code === 200 && res.data) {
    records.value = res.data.records || []
    recordPage.total = res.data.total || 0
  } else {
    message.value = res.message || '加载练习记录失败'
  }
  recordLoading.value = false
}
function turnRecordPage(delta) {
  const next = recordPage.page + delta
  if (next < 1 || (next - 1) * recordPage.size >= recordPage.total) return
  recordPage.page = next
  loadRecords()
}

function switchTab(next) {
  tab.value = next
  if (next === 'wrong') loadWrong()
  if (next === 'records') loadRecords()
}
function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}
function goSpecial(questionId) {
  wrongDetail.value = null
  current.value = null
  openSimilar(questionId)
}

onMounted(() => {
  if (!hasToken.value) return
  loadSummary()
  loadOptions()
  loadQuestions()
})
</script>

<template>
  <div class="practice">
    <header class="hero">
      <div class="hero-text">
        <p class="eyebrow">PRACTICE · 组员 2</p>
        <h1>刷题训练与错题本</h1>
        <p class="sub">练习 → 判题 → 自动收录错题 → 同知识点再练，形成完整训练闭环。</p>
      </div>
      <div class="hero-stats">
        <div class="stat"><strong>{{ summary?.total ?? 0 }}</strong><span>累计练习</span></div>
        <div class="stat"><strong>{{ summary?.accuracy ?? 0 }}%</strong><span>总正确率</span></div>
        <div class="stat"><strong>{{ summary?.todayCount ?? 0 }}</strong><span>今日练习</span></div>
        <div class="stat"><strong>{{ summary?.wrongTotal ?? 0 }}</strong><span>错题总数</span></div>
        <div class="stat"><strong>{{ summary?.masteredCount ?? 0 }}</strong><span>已掌握</span></div>
      </div>
    </header>

    <div v-if="!hasToken" class="empty">
      未检测到登录状态。请先打开 <a class="inline-link" href="/">主应用</a> 登录，再回到本页刷题。
    </div>

    <template v-else>
      <nav class="tabs">
        <button :class="{ active: tab === 'practice' }" type="button" @click="switchTab('practice')">题库练习</button>
        <button :class="{ active: tab === 'wrong' }" type="button" @click="switchTab('wrong')">错题本</button>
        <button :class="{ active: tab === 'records' }" type="button" @click="switchTab('records')">练习记录</button>
      </nav>
      <p v-if="message" class="notice" @click="message = ''">{{ message }}</p>

      <!-- 题库练习 -->
      <section v-if="tab === 'practice'">
        <div class="filter">
          <label>
            <span>科目</span>
            <select v-model="questionFilter.examSubject" @change="onSubjectChange">
              <option value="">全部科目</option>
              <option v-for="s in options.subjects" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
          <label>
            <span>章节</span>
            <select v-model="questionFilter.chapter">
              <option value="">全部章节</option>
              <option v-for="c in chapterList" :key="c" :value="c">{{ c }}</option>
            </select>
          </label>
          <label>
            <span>知识点</span>
            <select v-model="questionFilter.knowledgePoint">
              <option value="">全部知识点</option>
              <option v-for="k in pointList" :key="k" :value="k">{{ k }}</option>
            </select>
          </label>
          <label>
            <span>难度</span>
            <select v-model="questionFilter.difficulty">
              <option value="">全部难度</option>
              <option v-for="d in options.difficulties" :key="d" :value="d">{{ d }}</option>
            </select>
          </label>
          <label class="grow">
            <span>题干关键词</span>
            <input v-model="questionFilter.keyword" type="text" placeholder="如：极限 / 二分查找" @keyup.enter="searchQuestions" />
          </label>
          <button class="primary" type="button" @click="searchQuestions">筛选</button>
          <button class="ghost" type="button" @click="resetQuestionFilter">重置</button>
        </div>

        <!-- 练习结果 -->
        <section v-if="sessionAnswers.length" class="session">
          <div class="session-head">
            <h2>本轮练习结果</h2>
            <div class="session-nums">
              <span>共 {{ sessionAnswers.length }} 题</span>
              <span>答对 {{ sessionCorrect }}</span>
              <strong>正确率 {{ sessionAccuracy }}%</strong>
            </div>
          </div>
          <div v-if="sessionWrong.length" class="session-wrong">
            <p class="session-wrong-title">答错题目（已自动进入错题本）：</p>
            <ul>
              <li v-for="(item, i) in sessionWrong" :key="i">
                <span>{{ item.questionText }}</span>
                <em>你的答案 {{ item.userAnswer }} · 正确答案 {{ item.correctAnswer }}</em>
              </li>
            </ul>
          </div>
          <p v-else class="all-right">本轮全部答对，保持状态！</p>
          <button class="link" type="button" @click="sessionAnswers = []">清空本轮记录</button>
        </section>

        <p v-if="questionLoading" class="empty">加载中…</p>
        <p v-else-if="!questions.length" class="empty">
          没有符合条件的题目。请先执行数据库脚本 docs/sql/modules/02-practice.sql 初始化演示题库。
        </p>
        <div v-else class="question-grid">
          <article v-for="q in questions" :key="q.id" class="question-card">
            <div class="tags">
              <span class="tag subject">{{ q.examSubject }}</span>
              <span class="tag">{{ q.chapter }}</span>
              <span class="tag point">{{ q.knowledgePoint }}</span>
              <span class="tag difficulty">{{ q.difficulty }}</span>
              <span v-if="q.wrong" class="tag wrong-tag">错题本</span>
            </div>
            <p class="question-text">{{ q.questionText }}</p>
            <div class="question-foot">
              <span class="meta">{{ q.qType }} · {{ q.options.length }} 个选项</span>
              <div class="actions">
                <button class="ghost small" type="button" @click="openSimilar(q.id)">同类题</button>
                <button class="primary small" type="button" @click="openQuestion(q)">开始练习</button>
              </div>
            </div>
          </article>
        </div>
        <div class="pager">
          <button class="ghost" type="button" :disabled="questionPage.page <= 1" @click="turnQuestionPage(-1)">上一页</button>
          <span>第 {{ questionPage.page }} 页 · 共 {{ questionPage.total }} 题</span>
          <button class="ghost" type="button" :disabled="questionPage.page * questionPage.size >= questionPage.total" @click="turnQuestionPage(1)">下一页</button>
        </div>
      </section>

      <!-- 错题本 -->
      <section v-else-if="tab === 'wrong'">
        <div class="filter">
          <label>
            <span>科目</span>
            <select v-model="wrongFilter.examSubject">
              <option value="">全部科目</option>
              <option v-for="s in options.subjects" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
          <label>
            <span>知识点</span>
            <select v-model="wrongFilter.knowledgePoint">
              <option value="">全部知识点</option>
              <option v-for="k in (wrongFilter.examSubject ? wrongPointList : [])" :key="k" :value="k">{{ k }}</option>
            </select>
          </label>
          <label>
            <span>掌握状态</span>
            <select v-model="wrongFilter.masteryLevel">
              <option v-for="m in masteryLevels" :key="m.value" :value="m.value">{{ m.label }}</option>
            </select>
          </label>
          <button class="primary" type="button" @click="searchWrong">筛选</button>
          <button class="ghost" type="button" @click="resetWrongFilter">重置</button>
        </div>
        <p class="tip">提示：选择科目后可按该科目的知识点筛选；标记“已掌握”后可用掌握状态过滤。</p>

        <p v-if="wrongLoading" class="empty">加载中…</p>
        <p v-else-if="!wrongList.length" class="empty">错题本空空如也，去“题库练习”做几道题吧。</p>
        <div v-else class="question-grid">
          <article v-for="w in wrongList" :key="w.id" class="question-card wrong-card">
            <div class="tags">
              <span class="tag subject">{{ w.examSubject }}</span>
              <span class="tag point">{{ w.knowledgePoint }}</span>
              <span :class="['tag', 'mastery', w.masteryLevel === 'MASTERED' ? 'mastered' : 'unmastered']">{{ masteryText(w.masteryLevel) }}</span>
            </div>
            <p class="question-text">{{ w.questionText }}</p>
            <div class="question-foot">
              <span class="meta">答错 {{ w.wrongCount }} 次 · 复习 {{ w.reviewCount }} 次</span>
              <div class="actions">
                <button class="ghost small" type="button" @click="goSpecial(w.questionId)">专项练习</button>
                <button class="primary small" type="button" @click="openWrongDetail(w)">查看详情</button>
              </div>
            </div>
          </article>
        </div>
        <div class="pager">
          <button class="ghost" type="button" :disabled="wrongPage.page <= 1" @click="turnWrongPage(-1)">上一页</button>
          <span>第 {{ wrongPage.page }} 页 · 共 {{ wrongPage.total }} 道错题</span>
          <button class="ghost" type="button" :disabled="wrongPage.page * wrongPage.size >= wrongPage.total" @click="turnWrongPage(1)">下一页</button>
        </div>
      </section>

      <!-- 练习记录 -->
      <section v-else>
        <div class="filter">
          <label class="check">
            <input v-model="onlyWrong" type="checkbox" @change="recordPage.page = 1; loadRecords()" />
            <span>只看答错</span>
          </label>
          <button class="ghost" type="button" @click="loadRecords">刷新</button>
        </div>
        <p v-if="recordLoading" class="empty">加载中…</p>
        <p v-else-if="!records.length" class="empty">暂无练习记录</p>
        <div v-else class="record-table">
          <div class="record-row record-head">
            <span>题目</span><span>作答</span><span>结果</span><span>时间</span>
          </div>
          <div v-for="r in records" :key="r.id" class="record-row">
            <span>
              <b>{{ r.questionText }}</b>
              <em>{{ r.examSubject }} · {{ r.knowledgePoint }} · {{ r.difficulty }}</em>
            </span>
            <span>我的 {{ r.userAnswer }} / 正确 {{ r.correctAnswer }}</span>
            <span :class="r.correct ? 'ok' : 'fail'">{{ r.correct ? '正确' : '错误' }}</span>
            <span class="meta">{{ formatTime(r.answerTime) }}</span>
          </div>
        </div>
        <div class="pager">
          <button class="ghost" type="button" :disabled="recordPage.page <= 1" @click="turnRecordPage(-1)">上一页</button>
          <span>第 {{ recordPage.page }} 页 · 共 {{ recordPage.total }} 条</span>
          <button class="ghost" type="button" :disabled="recordPage.page * recordPage.size >= recordPage.total" @click="turnRecordPage(1)">下一页</button>
        </div>
      </section>
    </template>

    <!-- 单题练习弹窗 -->
    <div v-if="current" class="overlay" @click.self="closeQuestion">
      <div class="modal">
        <div class="tags">
          <span class="tag subject">{{ current.examSubject }}</span>
          <span class="tag">{{ current.chapter }}</span>
          <span class="tag point">{{ current.knowledgePoint }}</span>
          <span class="tag difficulty">{{ current.difficulty }}</span>
          <button class="close" type="button" @click="closeQuestion">×</button>
        </div>
        <h2 class="modal-title">{{ current.questionText }}</h2>
        <div class="option-list">
          <button
            v-for="opt in current.options"
            :key="opt.key"
            :class="['option', {
              selected: selectedAnswer === opt.key,
              right: answerResult && answerResult.correctAnswer === opt.key,
              mine: answerResult && !answerResult.correct && answerResult.userAnswer === opt.key
            }]"
            type="button"
            :disabled="!!answerResult"
            @click="selectedAnswer = opt.key"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </button>
        </div>
        <button v-if="!answerResult" class="primary submit" type="button" :disabled="!selectedAnswer || submitting" @click="submitAnswer">
          {{ submitting ? '判题中…' : '提交答案' }}
        </button>
        <div v-else :class="['result', answerResult.correct ? 'success' : 'failure']">
          <h3>{{ answerResult.correct ? '回答正确！' : '回答错误' }}</h3>
          <p>正确答案：<b>{{ answerResult.correctAnswer }}</b>&nbsp;&nbsp;你的答案：<b>{{ answerResult.userAnswer }}</b></p>
          <p v-if="answerResult.newlyAdded" class="auto">已自动加入错题本</p>
          <p v-else-if="!answerResult.correct" class="auto">错题已更新，累计答错 {{ answerResult.wrongCount }} 次</p>
          <div class="analysis">
            <h4>解析</h4>
            <p>{{ answerResult.analysis || '暂无解析' }}</p>
          </div>
          <div class="result-actions">
            <button class="ghost" type="button" @click="goSpecial(current.id)">同知识点专项练习</button>
            <button class="primary" type="button" @click="nextQuestion">下一题</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 错题详情弹窗 -->
    <div v-if="wrongDetail" class="overlay" @click.self="wrongDetail = null">
      <div class="modal">
        <div class="tags">
          <span class="tag subject">{{ wrongDetail.examSubject }}</span>
          <span class="tag point">{{ wrongDetail.knowledgePoint }}</span>
          <span :class="['tag', 'mastery', wrongDetail.masteryLevel === 'MASTERED' ? 'mastered' : 'unmastered']">{{ masteryText(wrongDetail.masteryLevel) }}</span>
          <button class="close" type="button" @click="wrongDetail = null">×</button>
        </div>
        <h2 class="modal-title">{{ wrongDetail.questionText }}</h2>
        <div class="option-list readonly">
          <div
            v-for="opt in wrongDetail.options"
            :key="opt.key"
            :class="['option', { right: opt.key === wrongDetail.refAnswer, mine: opt.key === wrongDetail.lastUserAnswer && opt.key !== wrongDetail.refAnswer }]"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </div>
        </div>
        <p class="answer-line">我的原答案：<b class="fail">{{ wrongDetail.lastUserAnswer || '—' }}</b>&nbsp;&nbsp;正确答案：<b class="ok">{{ wrongDetail.refAnswer }}</b></p>
        <p class="meta">答错 {{ wrongDetail.wrongCount }} 次 · 复习 {{ wrongDetail.reviewCount }} 次 · 收录于 {{ formatTime(wrongDetail.addTime) }}</p>
        <div class="analysis">
          <h4>解析</h4>
          <p>{{ wrongDetail.analysis || '暂无解析' }}</p>
        </div>
        <div class="result-actions">
          <button class="primary" type="button" @click="markMastery('MASTERED')">已掌握</button>
          <button class="ghost" type="button" @click="markMastery('UNMASTERED')">仍不会</button>
          <button class="ghost" type="button" @click="goSpecial(wrongDetail.questionId)">专项练习</button>
          <button class="danger" type="button" @click="removeWrong">删除错题</button>
        </div>
      </div>
    </div>

    <!-- 专项练习弹窗 -->
    <div v-if="special" class="overlay" @click.self="closeSimilar">
      <div class="modal">
        <div class="tags">
          <span class="tag point">同知识点专项练习</span>
          <button class="close" type="button" @click="closeSimilar">×</button>
        </div>
        <p v-if="special.loading" class="empty">查找同类题…</p>
        <p v-else-if="special.empty || !special.records.length" class="empty">该知识点暂无更多同类题</p>
        <div v-else class="special-list">
          <article v-for="q in special.records" :key="q.id" class="special-item">
            <div class="tags">
              <span class="tag subject">{{ q.examSubject }}</span>
              <span class="tag point">{{ q.knowledgePoint }}</span>
              <span class="tag difficulty">{{ q.difficulty }}</span>
            </div>
            <p class="question-text">{{ q.questionText }}</p>
            <button class="primary small" type="button" @click="closeSimilar(); openQuestion(q)">开始练习</button>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.practice { max-width: 1080px; margin: 0 auto; padding: 24px 20px 56px; color: #16233a; font-size: 14px; }
.hero { display: flex; flex-wrap: wrap; gap: 20px; justify-content: space-between; align-items: center; padding: 24px 28px; border-radius: 18px; background: linear-gradient(120deg, #1c3f7a, #2f6fe4 60%, #4c8bf5); color: #fff; }
.eyebrow { margin: 0 0 6px; font-size: 12px; letter-spacing: 2px; opacity: .75; }
.hero h1 { margin: 0; font-size: 24px; }
.sub { margin: 8px 0 0; font-size: 13px; opacity: .85; }
.hero-stats { display: flex; gap: 10px; flex-wrap: wrap; }
.stat { min-width: 84px; padding: 12px 14px; border-radius: 12px; background: rgba(255, 255, 255, .12); text-align: center; }
.stat strong { display: block; font-size: 22px; }
.stat span { font-size: 12px; opacity: .8; }
.tabs { display: flex; gap: 8px; margin: 20px 0 14px; }
.tabs button { padding: 9px 20px; border: 1px solid #dde4ee; border-radius: 999px; background: #fff; color: #46526b; cursor: pointer; }
.tabs button.active { border-color: #2f6fe4; background: #eef4ff; color: #1c4fb8; font-weight: 600; }
.notice { padding: 10px 14px; border-radius: 10px; background: #fff8e6; border: 1px solid #f5dfa4; color: #7a5b00; cursor: pointer; }
.filter { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; padding: 14px 16px; background: #fff; border: 1px solid #e8edf5; border-radius: 12px; }
.filter label { display: grid; gap: 5px; font-size: 12px; color: #6b7688; }
.filter label.grow { flex: 1; min-width: 180px; }
.filter input[type='text'], .filter select { padding: 8px 10px; border: 1px solid #dde4ee; border-radius: 8px; font-size: 13px; background: #fff; min-width: 120px; }
.filter .check { display: flex; flex-direction: row; align-items: center; gap: 8px; font-size: 13px; }
.primary, .ghost, .danger { padding: 9px 18px; border-radius: 8px; font-size: 13px; cursor: pointer; border: 1px solid transparent; }
.primary { background: #2f6fe4; color: #fff; }
.primary:disabled { opacity: .5; cursor: not-allowed; }
.ghost { background: #fff; border-color: #dde4ee; color: #46526b; }
.danger { background: #fff; border-color: #f3c1bc; color: #d0453b; }
.small { padding: 6px 12px; font-size: 12px; }
.tip { font-size: 12px; color: #7a869a; }
.empty { padding: 24px; border: 1px dashed #dde4ee; border-radius: 12px; background: #fff; color: #7a869a; text-align: center; }
.inline-link { color: #2f6fe4; }
.question-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px; margin-top: 14px; }
.question-card { padding: 16px; border: 1px solid #e8edf5; border-radius: 14px; background: #fff; display: flex; flex-direction: column; gap: 10px; }
.wrong-card { border-left: 3px solid #ef6a5b; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.tag { padding: 2px 9px; border-radius: 999px; font-size: 11px; background: #f1f4f9; color: #5a6b85; }
.tag.subject { background: #e8f0ff; color: #1c4fb8; }
.tag.point { background: #ecfdf3; color: #067647; }
.tag.difficulty { background: #fff4e5; color: #b54708; }
.tag.wrong-tag { background: #fef3f2; color: #b42318; }
.tag.mastery.mastered { background: #ecfdf3; color: #067647; }
.tag.mastery.unmastered { background: #fef3f2; color: #b42318; }
.question-text { margin: 0; font-size: 14px; line-height: 1.7; flex: 1; }
.question-foot { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.meta { font-size: 12px; color: #7a869a; }
.actions { display: flex; gap: 8px; }
.pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 18px; font-size: 12px; color: #7a869a; }
.session { margin-top: 14px; padding: 16px 18px; border-radius: 14px; border: 1px solid #cfe0ff; background: #f5f9ff; }
.session-head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 8px; }
.session h2 { margin: 0; font-size: 16px; }
.session-nums { display: flex; gap: 14px; font-size: 13px; color: #46526b; }
.session-nums strong { color: #2f6fe4; }
.session-wrong ul { margin: 8px 0; padding-left: 18px; }
.session-wrong li { margin-bottom: 6px; line-height: 1.6; }
.session-wrong em { display: block; font-style: normal; font-size: 12px; color: #b42318; }
.session-wrong-title { margin: 10px 0 0; font-size: 13px; }
.all-right { color: #067647; }
.link { border: none; background: none; color: #7a869a; font-size: 12px; cursor: pointer; padding: 0; }
.record-table { margin-top: 14px; background: #fff; border: 1px solid #e8edf5; border-radius: 12px; overflow: hidden; }
.record-row { display: grid; grid-template-columns: 2.2fr 1fr 70px 1fr; gap: 12px; padding: 12px 16px; border-top: 1px solid #eef1f6; align-items: center; font-size: 13px; }
.record-row:first-child { border-top: none; }
.record-head { background: #f7f9fc; font-size: 12px; color: #7a869a; }
.record-row b { display: block; font-weight: 500; }
.record-row em { display: block; font-style: normal; font-size: 12px; color: #7a869a; margin-top: 4px; }
.ok { color: #067647; }
.fail { color: #b42318; }
.overlay { position: fixed; inset: 0; background: rgba(16, 24, 40, .45); display: flex; align-items: center; justify-content: center; padding: 24px; z-index: 60; }
.modal { width: 640px; max-width: 100%; max-height: 86vh; overflow: auto; background: #fff; border-radius: 16px; padding: 22px 24px; }
.modal-title { margin: 12px 0 16px; font-size: 17px; line-height: 1.7; white-space: pre-wrap; }
.close { margin-left: auto; border: none; background: none; font-size: 22px; color: #97a1b5; cursor: pointer; line-height: 1; }
.option-list { display: grid; gap: 10px; }
.option { display: flex; gap: 12px; align-items: flex-start; padding: 12px 14px; border: 1px solid #e8edf5; border-radius: 10px; background: #fbfcfe; text-align: left; font-size: 14px; cursor: pointer; }
.option:disabled { cursor: default; }
.option.selected { border-color: #2f6fe4; background: #eef4ff; }
.option.right { border-color: #12b76a; background: #ecfdf3; }
.option.mine { border-color: #f04438; background: #fef3f2; }
.opt-key { flex: 0 0 24px; height: 24px; border-radius: 50%; background: #eef1f6; color: #46526b; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.option.selected .opt-key { background: #2f6fe4; color: #fff; }
.opt-text { line-height: 1.6; }
.readonly .option { cursor: default; }
.submit { width: 100%; margin-top: 16px; }
.result { margin-top: 16px; padding: 14px 16px; border-radius: 12px; }
.result.success { background: #ecfdf3; }
.result.failure { background: #fef3f2; }
.result h3 { margin: 0 0 8px; font-size: 16px; }
.auto { font-size: 12px; color: #b54708; }
.analysis { margin-top: 12px; padding: 12px 14px; border-radius: 10px; background: #f7f9fc; }
.analysis h4 { margin: 0 0 6px; font-size: 13px; color: #46526b; }
.analysis p { margin: 0; font-size: 13px; line-height: 1.8; color: #46526b; }
.result-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.answer-line { font-size: 14px; }
.special-list { display: grid; gap: 12px; margin-top: 12px; }
.special-item { padding: 14px; border: 1px solid #e8edf5; border-radius: 12px; display: grid; gap: 8px; }
.special-item .primary { justify-self: end; }
@media (max-width: 720px) {
  .record-row { grid-template-columns: 1fr; gap: 4px; }
  .record-head { display: none; }
}
</style>
