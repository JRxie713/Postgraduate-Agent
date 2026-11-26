import { request, TOKEN_KEY } from './http'

export const getWsTicket = () => request('/chat/ws-ticket', { method: 'POST' })

export const listSessions = () => request('/chat/sessions')

export const listContacts = () => request('/chat/contacts')

export const createPrivateSession = (peerId) =>
  request('/chat/sessions/private', { method: 'POST', body: { peerId } })

export const createGroupSession = (payload) =>
  request('/chat/sessions/group', { method: 'POST', body: payload })

export const getSession = (id) => request(`/chat/sessions/${id}`)

export const listMembers = (id) => request(`/chat/sessions/${id}/members`)

export const addMembers = (id, memberIds) =>
  request(`/chat/sessions/${id}/members`, { method: 'POST', body: { memberIds } })

export const listMessages = (id, { afterId, beforeId, size = 30 } = {}) => {
  const params = new URLSearchParams({ size: String(size) })
  if (afterId) params.set('afterId', String(afterId))
  if (beforeId) params.set('beforeId', String(beforeId))
  return request(`/chat/sessions/${id}/messages?${params.toString()}`)
}

export const markRead = (id, lastReadMsgId) =>
  request(`/chat/sessions/${id}/read`, { method: 'POST', body: { lastReadMsgId } })

export const recallMessage = (id) => request(`/chat/messages/${id}/recall`, { method: 'POST' })

export const getFileUrl = (id) => request(`/chat/files/${id}/url`)

/**
 * multipart 上传不能复用 JSON 请求封装（FormData 需要浏览器自动带 boundary）
 */
export async function uploadChatFile(sessionId, file) {
  const token = localStorage.getItem(TOKEN_KEY)
  const form = new FormData()
  form.append('sessionId', String(sessionId))
  form.append('file', file)

  let res
  try {
    res = await fetch('/api/chat/files', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form
    })
  } catch (e) {
    return { code: 0, message: '无法连接后端服务，请确认 8080 端口已启动' }
  }
  try {
    return await res.json()
  } catch (e) {
    return { code: res.status, message: `上传失败（HTTP ${res.status}）` }
  }
}
