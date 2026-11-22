import { request } from './http'

// ===== 学习计划 =====
export const myPlans = () => request('/plan/mine')

export const createPlan = (payload) => request('/plan', { method: 'POST', body: payload })

export const deletePlan = (id) => request(`/plan/${id}`, { method: 'DELETE' })

export const createPlanTask = (payload) => request('/plan/task', { method: 'POST', body: payload })

export const updateTaskStatus = (id, taskStatus) =>
  request(`/plan/task/${id}/status`, { method: 'PUT', body: { taskStatus } })

export const deletePlanTask = (id) => request(`/plan/task/${id}`, { method: 'DELETE' })

// ===== 政策资讯 =====
export const policyList = ({ page = 1, size = 10, noticeType = '' } = {}) => {
  const type = noticeType ? `&noticeType=${encodeURIComponent(noticeType)}` : ''
  return request(`/policy?page=${page}&size=${size}${type}`)
}

export const createPolicy = (payload) => request('/policy', { method: 'POST', body: payload })

// ===== 学习打卡（Redis + Kafka 异步聚合）=====
export const checkin = (payload) => request('/study-checkin', { method: 'POST', body: payload })

export const checkinOverview = (userId) =>
  request(`/study-checkin/overview/${encodeURIComponent(userId)}`)

export const checkinRank = (type, top = 10) =>
  request(`/study-checkin/rank/${type}?top=${encodeURIComponent(top)}`)

// ===== 院校热度（Redis 去重计数 / 排行 + Kafka 异步聚合）=====
export const trackSchoolHeat = (payload) =>
  request('/school-heat', { method: 'POST', body: payload })

export const schoolHeatRank = (top = 5) =>
  request(`/school-heat/rank?top=${encodeURIComponent(top)}`)

export const schoolHeatOptions = () => request('/school-heat/options')

export const schoolHeatCards = (userId) =>
  request(`/school-heat/cards?userId=${encodeURIComponent(userId)}`)

export const schoolHeatMine = (userId) =>
  request(`/school-heat/mine?userId=${encodeURIComponent(userId)}`)
