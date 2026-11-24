import { request } from './http'

export const getCaptcha = () => request('/user/captcha', { auth: false })

export const login = (payload) =>
  request('/user/login', { method: 'POST', body: payload, auth: false })

export const sendSmsCode = (payload) =>
  request('/user/sms-code', { method: 'POST', body: payload, auth: false })

export const loginBySms = (payload) =>
  request('/user/login/sms', { method: 'POST', body: payload, auth: false })

export const loginByEmail = (payload) =>
  request('/user/login/email', { method: 'POST', body: payload, auth: false })

export const sendEmailCode = (payload) =>
  request('/user/email-code', { method: 'POST', body: payload, auth: false })

export const register = (payload) =>
  request('/user/register', { method: 'POST', body: payload, auth: false })

export const getLoginStatus = (userName) =>
  request(`/user/login/status?userName=${encodeURIComponent(userName)}`, { auth: false })

export const currentUser = () => request('/user/current')

export const logout = () => request('/user/logout', { method: 'POST' })

export const refreshToken = (token) =>
  request('/user/refresh', { method: 'POST', body: { refreshToken: token }, auth: false })

export const statOverview = () => request('/stat/overview?range=WEEK')

// ===== 用户管理（仅管理员）=====
export const userList = ({ page = 1, size = 10, keyword = '', role = '' } = {}) => {
  const kw = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''
  const r = role ? `&role=${encodeURIComponent(role)}` : ''
  return request(`/user?page=${page}&size=${size}${kw}${r}`)
}

export const createUser = (payload) => request('/user', { method: 'POST', body: payload })

export const updateUser = (id, payload) => request(`/user/${id}`, { method: 'PUT', body: payload })

export const deleteUser = (id) => request(`/user/${id}`, { method: 'DELETE' })
