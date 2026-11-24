import { reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import * as api from '../api/chat'

/**
 * 聊天全局状态（不引入 Pinia，沿用项目现有的 reactive 单例方式）
 *
 * 服务端以数据库为准：消息落库后经 Redis 广播到所有实例；
 * 客户端按消息 id 去重排序，断线重连后用 afterId 补拉。
 */
export const chat = reactive({
  userId: null,
  connected: false,
  connecting: false,
  error: '',
  sessions: [],
  currentSessionId: null,
  messages: {},
  messagesVersion: {},   // 会话消息重新加载的版本号，驱动列表定位到最新一条
  hasMore: {},
  loadingMessages: false,
  loadingMore: false,
  members: {},
  contacts: [],
  typing: {},
  readCursor: {},
  groupRead: {},
  totalUnread: 0,
  uploading: false
})

let client = null
let reconnectTimer = null
let manualClose = false
let userQueueSubscribed = false
const sessionSubs = new Map()
const fileUrlCache = new Map()
const typingTimers = {}
const lastReadSent = {}
let lastTypingAt = 0
let visibilityBound = false

export function connect(userId) {
  if (!userId) return
  chat.userId = userId
  manualClose = false
  bindVisibility()
  if (client || chat.connecting || reconnectTimer) return
  openClient()
}

/**
 * 票据是一次性的，因此重连必须重新申请票据；这里关闭 stompjs 自带的自动重连，
 * 改为每次重连都创建新连接。
 */
async function openClient() {
  if (!chat.userId || manualClose) return
  chat.connecting = true
  const res = await api.getWsTicket()
  if (!res || res.code !== 200) {
    chat.connecting = false
    chat.error = res && res.message ? res.message : '获取聊天连接票据失败'
    scheduleReconnect()
    return
  }

  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const brokerURL = `${protocol}://${location.host}${res.data.wsPath || '/ws'}?ticket=${encodeURIComponent(res.data.ticket)}`

  const current = new Client({
    brokerURL,
    reconnectDelay: 0,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {}
  })
  client = current

  current.onConnect = () => {
    chat.connected = true
    chat.connecting = false
    chat.error = ''
    userQueueSubscribed = false
    sessionSubs.clear()
    subscribeUserQueues()
    loadSessions()
    if (chat.currentSessionId) resyncSession(chat.currentSessionId)
  }

  current.onWebSocketClose = () => {
    chat.connected = false
    chat.connecting = false
    userQueueSubscribed = false
    sessionSubs.clear()
    if (client === current) client = null
    scheduleReconnect()
  }

  current.onStompError = (frame) => {
    chat.error = (frame && frame.headers && frame.headers.message) || '聊天连接异常'
    chat.connected = false
    chat.connecting = false
    if (client === current) client = null
    try {
      current.deactivate()
    } catch (e) {
      // 忽略关闭异常
    }
    scheduleReconnect()
  }

  current.onDisconnect = () => {
    chat.connected = false
  }

  try {
    current.activate()
  } catch (e) {
    chat.connecting = false
    if (client === current) client = null
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (manualClose || !chat.userId || reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    openClient()
  }, 4000)
}

export function disconnect() {
  manualClose = true
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  const current = client
  client = null
  if (current) {
    try {
      current.deactivate()
    } catch (e) {
      // 忽略关闭异常
    }
  }
  chat.connected = false
  chat.connecting = false
  chat.userId = null
  chat.currentSessionId = null
  chat.sessions = []
  chat.messages = {}
  chat.hasMore = {}
  chat.members = {}
  chat.typing = {}
  chat.readCursor = {}
  chat.groupRead = {}
  chat.totalUnread = 0
  sessionSubs.clear()
  userQueueSubscribed = false
}

export async function loadSessions() {
  const res = await api.listSessions()
  if (res.code !== 200) {
    chat.error = res.message || '加载会话失败'
    return
  }
  chat.sessions = res.data || []
  sortSessions()
  recomputeUnread()
  chat.sessions.forEach((session) => ensureSessionSubscription(session.id))
}

export async function openSession(sessionId) {
  if (!sessionId) return
  chat.currentSessionId = sessionId
  const session = chat.sessions.find((item) => item.id === sessionId)
  if (session) session.unreadCount = 0
  recomputeUnread()
  ensureSessionSubscription(sessionId)

  await loadMembers(sessionId)

  chat.loadingMessages = true
  const res = await api.listMessages(sessionId, { size: 30 })
  chat.loadingMessages = false
  if (res.code === 200) {
    chat.messages[sessionId] = res.data || []
    chat.hasMore[sessionId] = (res.data || []).length >= 30
    // 在消息赋值之后再递增版本，确保监听方拿到的已是新列表
    chat.messagesVersion[sessionId] = (chat.messagesVersion[sessionId] || 0) + 1
    if (chat.currentSessionId === sessionId) markCurrentRead(true)
  } else {
    chat.error = res.message || '加载消息失败'
  }
}

export async function loadMore() {
  const sessionId = chat.currentSessionId
  const list = chat.messages[sessionId]
  if (!sessionId || !list || !list.length || chat.loadingMore) return
  chat.loadingMore = true
  const res = await api.listMessages(sessionId, { beforeId: list[0].id, size: 30 })
  chat.loadingMore = false
  if (res.code === 200 && res.data && res.data.length) {
    chat.messages[sessionId] = mergeMessages(res.data, list)
    chat.hasMore[sessionId] = res.data.length >= 30
  } else {
    chat.hasMore[sessionId] = false
  }
}

export async function loadMembers(sessionId) {
  const res = await api.listMembers(sessionId)
  if (res.code === 200) {
    chat.members[sessionId] = res.data || []
  }
}

export async function loadContacts() {
  const res = await api.listContacts()
  if (res.code === 200) {
    chat.contacts = res.data || []
  }
}

export function sendText(text) {
  const sessionId = chat.currentSessionId
  const content = text == null ? '' : text.trim()
  if (!sessionId || !content) return false
  if (!client || !chat.connected) {
    chat.error = '聊天连接已断开，正在重连'
    return false
  }
  client.publish({
    destination: '/app/chat.send',
    headers: { 'content-type': 'text/plain;charset=UTF-8' },
    body: JSON.stringify({
      sessionId,
      clientMsgId: uuid(),
      msgType: 'TEXT',
      content
    })
  })
  return true
}

export async function sendFile(file) {
  const sessionId = chat.currentSessionId
  if (!sessionId || !file) return false
  if (!client || !chat.connected) {
    chat.error = '聊天连接已断开，正在重连'
    return false
  }
  const isImage = file.type && file.type.startsWith('image/')
  const limit = isImage ? 10 * 1024 * 1024 : 100 * 1024 * 1024
  if (file.size > limit) {
    chat.error = isImage ? '图片不能超过 10MB' : '文件不能超过 100MB'
    return false
  }

  chat.uploading = true
  const res = await api.uploadChatFile(sessionId, file)
  chat.uploading = false
  if (res.code !== 200 || !res.data) {
    chat.error = res.message || '文件上传失败'
    return false
  }
  client.publish({
    destination: '/app/chat.send',
    headers: { 'content-type': 'text/plain;charset=UTF-8' },
    body: JSON.stringify({
      sessionId,
      clientMsgId: uuid(),
      msgType: isImage ? 'IMAGE' : 'FILE',
      fileId: res.data.id
    })
  })
  return true
}

export async function recall(messageId) {
  const res = await api.recallMessage(messageId)
  if (res.code !== 200) {
    chat.error = res.message || '撤回失败'
    return false
  }
  applyRecall(res.data)
  return true
}

export function sendTyping() {
  const sessionId = chat.currentSessionId
  if (!sessionId || !client || !chat.connected) return
  const now = Date.now()
  if (now - lastTypingAt < 2500) return
  lastTypingAt = now
  client.publish({
    destination: '/app/chat.typing',
    headers: { 'content-type': 'text/plain;charset=UTF-8' },
    body: JSON.stringify({ sessionId })
  })
}

export async function startPrivateSession(peerId) {
  const res = await api.createPrivateSession(peerId)
  if (res.code !== 200 || !res.data) {
    chat.error = res.message || '创建会话失败'
    return false
  }
  await loadSessions()
  await openSession(res.data.id)
  return true
}

export async function createGroupSession(name, memberIds) {
  const res = await api.createGroupSession({ name, memberIds })
  if (res.code !== 200 || !res.data) {
    chat.error = res.message || '创建群聊失败'
    return false
  }
  await loadSessions()
  await openSession(res.data.id)
  return true
}

export async function addGroupMembers(sessionId, memberIds) {
  const res = await api.addMembers(sessionId, memberIds)
  if (res.code !== 200) {
    chat.error = res.message || '添加成员失败'
    return false
  }
  await loadMembers(sessionId)
  await loadSessions()
  return true
}

export async function getFileUrl(fileId) {
  if (!fileId) return ''
  // 下载地址有效期 10 分钟，本地缓存 8 分钟后重新换取
  const cached = fileUrlCache.get(fileId)
  if (cached && Date.now() - cached.at < 8 * 60 * 1000) return cached.promise
  const promise = api.getFileUrl(fileId).then((res) => {
    if (res.code === 200 && res.data) return res.data.url
    fileUrlCache.delete(fileId)
    return ''
  })
  fileUrlCache.set(fileId, { promise, at: Date.now() })
  return promise
}

export function readLabel(message) {
  if (!message || message.senderId !== chat.userId) return ''
  const session = chat.sessions.find((item) => item.id === message.sessionId)
  if (session && session.type === 'GROUP') {
    const map = chat.groupRead[message.sessionId] || {}
    let count = 0
    Object.keys(map).forEach((key) => {
      if (Number(key) >= Number(message.id)) count = Math.max(count, map[key])
    })
    return count > 1 ? `已读 ${count}/${session.memberCount || ''}` : '已送达'
  }
  return (chat.readCursor[message.sessionId] || 0) >= message.id ? '已读' : '已送达'
}

export function typingName(sessionId) {
  const item = chat.typing[sessionId]
  return item && item.name ? item.name : ''
}

export function previewOf(message) {
  if (!message) return ''
  if (message.status === 'RECALLED') return '[消息已撤回]'
  if (message.msgType === 'IMAGE') return '[图片]'
  if (message.msgType === 'FILE') return `[文件] ${message.fileName || ''}`
  const text = message.content || ''
  return text.length > 40 ? `${text.slice(0, 40)}…` : text
}

export function markCurrentRead(force = false) {
  const sessionId = chat.currentSessionId
  const list = chat.messages[sessionId]
  if (!sessionId || !list || !list.length) return
  const last = list[list.length - 1]
  if (!force && (lastReadSent[sessionId] || 0) >= last.id) return
  lastReadSent[sessionId] = last.id
  const session = chat.sessions.find((item) => item.id === sessionId)
  if (session) session.unreadCount = 0
  recomputeUnread()
  api.markRead(sessionId, last.id).then((res) => {
    if (res.code !== 200) delete lastReadSent[sessionId]
  })
}

// ============================== 内部实现 ==============================

function subscribeUserQueues() {
  if (!client || userQueueSubscribed) return
  userQueueSubscribed = true
  client.subscribe('/user/queue/inbox', (frame) => handleInbox(parse(frame.body)))
  client.subscribe('/user/queue/ack', (frame) => handleAck(parse(frame.body)))
}

function ensureSessionSubscription(sessionId) {
  if (!client || !client.connected || sessionSubs.has(sessionId)) return
  const sub = client.subscribe(`/topic/session/${sessionId}`, (frame) => {
    handleSessionEvent(parse(frame.body))
  })
  sessionSubs.set(sessionId, sub)
}

function handleSessionEvent(event) {
  if (!event) return
  if (event.event === 'MESSAGE') {
    onMessage(event.message)
  } else if (event.event === 'READ') {
    onRead(event.read)
  } else if (event.event === 'RECALL') {
    applyRecall(event.recall)
  } else if (event.event === 'TYPING') {
    onTyping(event.typing)
  }
}

function handleInbox(event) {
  if (!event) return
  if (event.event === 'SESSION_CREATED' && event.session) {
    const session = event.session
    if (!chat.sessions.some((item) => item.id === session.id)) {
      chat.sessions.unshift(session)
      sortSessions()
      recomputeUnread()
      ensureSessionSubscription(session.id)
    }
  }
}

function handleAck(ack) {
  if (!ack) return
  if (!ack.ok) {
    chat.error = ack.message || '消息发送失败'
  }
}

function onMessage(message) {
  if (!message) return
  const session = chat.sessions.find((item) => item.id === message.sessionId)
  if (!session) {
    loadSessions()
    return
  }
  appendMessage(message.sessionId, message)
  session.lastMsgId = message.id
  session.lastMsgPreview = previewOf(message)
  session.lastMsgTime = message.createTime
  sortSessions()
  delete chat.typing[message.sessionId]

  if (message.senderId !== chat.userId) {
    if (message.sessionId === chat.currentSessionId && !document.hidden) {
      markCurrentRead()
    } else {
      session.unreadCount = (session.unreadCount || 0) + 1
      recomputeUnread()
    }
  }
}

function onRead(read) {
  if (!read || read.userId === chat.userId) return
  const session = chat.sessions.find((item) => item.id === read.sessionId)
  if (session && session.type === 'GROUP') {
    const map = chat.groupRead[read.sessionId] || {}
    map[read.lastReadMsgId] = read.readCount || 0
    chat.groupRead[read.sessionId] = map
  } else {
    const current = chat.readCursor[read.sessionId] || 0
    chat.readCursor[read.sessionId] = Math.max(current, read.lastReadMsgId || 0)
  }
}

function onTyping(typing) {
  if (!typing || typing.userId === chat.userId) return
  chat.typing[typing.sessionId] = { name: typing.name, at: Date.now() }
  if (typingTimers[typing.sessionId]) clearTimeout(typingTimers[typing.sessionId])
  typingTimers[typing.sessionId] = setTimeout(() => {
    delete chat.typing[typing.sessionId]
  }, 3000)
}

function applyRecall(recall) {
  if (!recall) return
  const list = chat.messages[recall.sessionId]
  if (!list) return
  const index = list.findIndex((item) => item.id === recall.messageId)
  if (index >= 0) {
    const target = list[index]
    list[index] = {
      ...target,
      status: 'RECALLED',
      content: null,
      fileId: null,
      fileName: null,
      fileSize: null,
      contentType: null
    }
    const session = chat.sessions.find((item) => item.id === recall.sessionId)
    if (session && session.lastMsgId === recall.messageId) {
      session.lastMsgPreview = '[消息已撤回]'
    }
  }
}

function appendMessage(sessionId, message) {
  const list = chat.messages[sessionId] || []
  const index = list.findIndex((item) => item.id === message.id)
  if (index >= 0) list[index] = message
  else list.push(message)
  list.sort((a, b) => a.id - b.id)
  chat.messages[sessionId] = list
}

function mergeMessages(older, current) {
  const map = new Map()
  older.concat(current).forEach((item) => map.set(item.id, item))
  return Array.from(map.values()).sort((a, b) => a.id - b.id)
}

async function resyncSession(sessionId) {
  const list = chat.messages[sessionId] || []
  const afterId = list.length ? list[list.length - 1].id : undefined
  const res = await api.listMessages(sessionId, { afterId, size: 100 })
  if (res.code === 200 && res.data && res.data.length) {
    res.data.forEach((item) => appendMessage(sessionId, item))
    markCurrentRead(true)
  }
}

function sortSessions() {
  chat.sessions.sort((a, b) =>
    String(b.lastMsgTime || '').localeCompare(String(a.lastMsgTime || '')))
}

function recomputeUnread() {
  chat.totalUnread = chat.sessions.reduce((sum, session) => {
    if (session.id === chat.currentSessionId) return sum
    return sum + (session.unreadCount || 0)
  }, 0)
}

function parse(body) {
  try {
    return JSON.parse(body)
  } catch (e) {
    return null
  }
}

function uuid() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `${Date.now()}${Math.random().toString(16).slice(2, 10)}`
}

function bindVisibility() {
  if (visibilityBound) return
  visibilityBound = true
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) markCurrentRead()
  })
}
