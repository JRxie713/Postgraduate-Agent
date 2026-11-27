import { request } from './http'

function toQuery(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

// ===== 刷题训练 =====
export const practiceOptions = () => request('/practice/options')

export const practiceQuestions = (params = {}) => request(`/practice/questions${toQuery(params)}`)

export const practiceQuestionDetail = (id) => request(`/practice/questions/${id}`)

export const submitPracticeAnswer = (payload) =>
  request('/practice/answers', { method: 'POST', body: payload })

export const practiceRecords = (params = {}) => request(`/practice/records${toQuery(params)}`)

export const practiceSummary = () => request('/practice/summary')

export const practiceSimilar = (questionId, limit = 5) =>
  request(`/practice/similar/${questionId}${toQuery({ limit })}`)

export const practiceDaily = (params = {}) => request(`/practice/daily${toQuery(params)}`)

// ===== 错题本 =====
export const wrongQuestionList = (params = {}) => request(`/wrong-questions${toQuery(params)}`)

export const wrongQuestionDetail = (id) => request(`/wrong-questions/${id}`)

export const updateWrongMastery = (id, masteryLevel) =>
  request(`/wrong-questions/${id}/mastery`, { method: 'PUT', body: { masteryLevel } })

export const deleteWrongQuestion = (id) => request(`/wrong-questions/${id}`, { method: 'DELETE' })
