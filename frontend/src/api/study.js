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
