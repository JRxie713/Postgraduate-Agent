import { request } from './http'

// ===== 复试 AI 训练 =====
export const startInterview = (payload) =>
  request('/interview/sessions', { method: 'POST', body: payload })

export const listInterviewSessions = (userId) =>
  request(`/interview/sessions?userId=${encodeURIComponent(userId)}`)

export const getInterviewSession = (id) => request(`/interview/sessions/${id}`)

export const getCurrentQuestion = (id) => request(`/interview/sessions/${id}/question`)

export const submitInterviewAnswer = (id, payload) =>
  request(`/interview/sessions/${id}/answers`, { method: 'POST', body: payload })

export const generateFollowUp = (id, roundIndex) =>
  request(`/interview/sessions/${id}/follow-up`, {
    method: 'POST',
    body: { sessionId: id, roundIndex }
  })

export const finishInterview = (id) =>
  request(`/interview/sessions/${id}/finish`, { method: 'POST' })

export const getInterviewReport = (id) => request(`/interview/sessions/${id}/report`)

export const reviewEnglishIntro = (introduction) =>
  request('/interview/english-introduction/review', { method: 'POST', body: { introduction } })
