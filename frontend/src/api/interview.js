import { request } from './http'

// ===== 复试 AI 训练 =====
export const startInterview = (payload) =>
  request('/interview/sessions', { method: 'POST', body: payload })

export const listInterviewSessions = (userId) =>
  request(`/interview/sessions?userId=${encodeURIComponent(userId)}`)

export const getInterviewSession = (id) => request(`/interview/sessions/${id}`)

// 超时未作答提醒（与后端 Quartz 任务同一阈值）
export const interviewReminders = (userId) =>
  request(`/interview/reminders?userId=${encodeURIComponent(userId)}`)

export const getCurrentQuestion = (id) => request(`/interview/sessions/${id}/question`)

// 已作答的历史轮次（回看上一题评价）
export const listInterviewTurns = (id) => request(`/interview/sessions/${id}/turns`)

export const submitInterviewAnswer = (id, payload) =>
  request(`/interview/sessions/${id}/answers`, { method: 'POST', body: payload })

export const generateFollowUp = (id, roundIndex) =>
  request(`/interview/sessions/${id}/follow-up`, {
    method: 'POST',
    body: { sessionId: id, roundIndex }
  })

export const finishInterview = (id) =>
  request(`/interview/sessions/${id}/finish`, { method: 'POST' })

// 放弃进行中的面试（不再提醒，不生成报告）
export const abandonInterview = (id) =>
  request(`/interview/sessions/${id}/abandon`, { method: 'POST' })

// 心跳：点"继续作答"后刷新活跃时间，超时提醒重新计时
export const heartbeatInterview = (id) =>
  request(`/interview/sessions/${id}/heartbeat`, { method: 'POST' })

export const getInterviewReport = (id) => request(`/interview/sessions/${id}/report`)

export const reviewEnglishIntro = (introduction, userId) =>
  request('/interview/english-introduction/review', {
    method: 'POST',
    body: { introduction, userId }
  })

// 自我介绍批改历史
export const listEnglishReviews = (userId) =>
  request(`/interview/english-introductions?userId=${encodeURIComponent(userId)}`)
