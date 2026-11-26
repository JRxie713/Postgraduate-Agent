<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import SessionList from './SessionList.vue'
import MessageList from './MessageList.vue'
import MessageComposer from './MessageComposer.vue'
import MemberPanel from './MemberPanel.vue'
import {
  chat,
  connect,
  loadContacts,
  loadMembers,
  loadMore,
  openSession,
  recall,
  sendFile,
  sendText,
  sendTyping,
  startPrivateSession,
  createGroupSession,
  addGroupMembers
} from '../../chat/store'

const props = defineProps({
  user: { type: Object, default: null }
})

const showNew = ref(false)
const mode = ref('private')
const peerId = ref(null)
const groupName = ref('')
const groupMembers = ref([])
const creating = ref(false)
const showMembers = ref(false)

const currentSession = computed(() =>
  chat.sessions.find((session) => session.id === chat.currentSessionId) || null)
const currentMessages = computed(() => chat.messages[chat.currentSessionId] || [])
const currentMembers = computed(() => chat.members[chat.currentSessionId] || [])
const isGroup = computed(() => currentSession.value?.type === 'GROUP')
const isOwner = computed(() =>
  isGroup.value && currentSession.value && currentSession.value.ownerId === chat.userId)
const statusText = computed(() => {
  if (chat.connected) return '已连接'
  return chat.connecting ? '连接中…' : '未连接'
})

watch(() => props.user && props.user.id, (id) => {
  if (id) connect(id)
})

onMounted(() => {
  if (props.user && props.user.id) connect(props.user.id)
  loadContacts()
})

async function selectSession(sessionId) {
  showNew.value = false
  showMembers.value = false
  await openSession(sessionId)
}

async function createPrivate() {
  if (!peerId.value || creating.value) return
  creating.value = true
  const ok = await startPrivateSession(Number(peerId.value))
  creating.value = false
  if (ok) {
    showNew.value = false
    peerId.value = null
  }
}

async function createGroup() {
  if (!groupName.value.trim() || creating.value) return
  creating.value = true
  const ok = await createGroupSession(groupName.value.trim(), groupMembers.value)
  creating.value = false
  if (ok) {
    showNew.value = false
    groupName.value = ''
    groupMembers.value = []
  }
}

function onSendText(text) {
  sendText(text)
}

async function onSendFile(file) {
  await sendFile(file)
}

async function onRecall(messageId) {
  if (!window.confirm('确定撤回这条消息？')) return
  await recall(messageId)
}

async function toggleMembers() {
  showMembers.value = !showMembers.value
  if (showMembers.value && currentSession.value) {
    await loadMembers(currentSession.value.id)
  }
}

async function onAddMembers(memberIds) {
  if (!currentSession.value) return
  await addGroupMembers(currentSession.value.id, memberIds)
}
</script>

<template>
  <div class="chat-page">
    <div :class="['chat-shell', { 'with-members': showMembers && isGroup }]">
      <aside class="session-pane">
        <div class="pane-head">
          <div class="title">
            <h2>实时消息</h2>
            <span :class="['status', chat.connected ? 'on' : 'off']">
              <i></i>{{ statusText }}
            </span>
          </div>
          <button class="new-btn" type="button" title="发起新会话" @click="showNew = !showNew">+</button>
        </div>

        <div v-if="showNew" class="new-panel">
          <div class="tabs">
            <button type="button" :class="{ active: mode === 'private' }" @click="mode = 'private'">私聊</button>
            <button type="button" :class="{ active: mode === 'group' }" @click="mode = 'group'">自习室</button>
          </div>
          <template v-if="mode === 'private'">
            <select v-model="peerId" class="control">
              <option :value="null">选择联系人</option>
              <option v-for="contact in chat.contacts" :key="contact.id" :value="contact.id">
                {{ contact.name }}（{{ contact.userRole === 'TEACHER' ? '导师' : '学生' }}）
              </option>
            </select>
            <button class="primary" type="button" :disabled="!peerId || creating" @click="createPrivate">
              开始聊天
            </button>
          </template>
          <template v-else>
            <input v-model="groupName" class="control" maxlength="64" placeholder="自习室名称" />
            <div class="pick-list">
              <label v-for="contact in chat.contacts" :key="contact.id">
                <input v-model="groupMembers" type="checkbox" :value="contact.id" />
                <span>{{ contact.name }}</span>
              </label>
              <p v-if="!chat.contacts.length" class="hint">暂无可邀请的联系人</p>
            </div>
            <button class="primary" type="button" :disabled="!groupName.trim() || creating" @click="createGroup">
              创建自习室
            </button>
          </template>
        </div>

        <SessionList :sessions="chat.sessions" :current-id="chat.currentSessionId" @select="selectSession" />
      </aside>

      <section class="message-pane">
        <header class="msg-head">
          <div class="who">
            <h3>{{ currentSession ? currentSession.name : '选择会话' }}</h3>
            <span v-if="currentSession">
              {{ isGroup ? `${currentSession.memberCount} 人` : (currentSession.peerRole === 'TEACHER' ? '导师' : '学生') }}
            </span>
          </div>
          <button v-if="isGroup" class="members-btn" type="button" @click="toggleMembers">
            {{ showMembers ? '收起成员' : '群成员' }}
          </button>
        </header>

        <div v-if="!currentSession" class="welcome">
          <div class="welcome-mark">CHAT</div>
          <h3>实时消息</h3>
          <p>与导师一对一交流，或创建自习室和同学一起学习。</p>
        </div>

        <template v-else>
          <MessageList
            :session-id="currentSession.id"
            :messages="currentMessages"
            :loading="chat.loadingMessages"
            :messages-version="chat.messagesVersion[currentSession.id] || 0"
            :has-more="!!chat.hasMore[currentSession.id]"
            :is-group="isGroup"
            :current-user-id="chat.userId"
            @load-more="loadMore"
            @recall="onRecall"
          />
          <MessageComposer
            :disabled="!chat.connected"
            :uploading="chat.uploading"
            @send-text="onSendText"
            @send-file="onSendFile"
            @typing="sendTyping"
          />
        </template>
      </section>

      <MemberPanel
        v-if="showMembers && isGroup"
        :members="currentMembers"
        :contacts="chat.contacts"
        :owner-id="currentSession ? currentSession.ownerId : null"
        :is-owner="isOwner"
        @add="onAddMembers"
      />
    </div>

    <div v-if="chat.error" class="error-bar">
      <span>{{ chat.error }}</span>
      <button type="button" @click="chat.error = ''">×</button>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  position: relative;
}
.chat-shell {
  display: grid;
  grid-template-columns: 258px minmax(0, 1fr);
  height: clamp(520px, 68vh, 680px);
  border: 1px solid #e6ebf4;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 28px rgba(21, 42, 88, 0.06);
}
.chat-shell.with-members {
  grid-template-columns: 240px minmax(0, 1fr) 218px;
}
.session-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-right: 1px solid #eef2f8;
  background: #fbfcff;
}
.pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 13px 12px 9px;
}
.title h2 {
  margin: 0 0 2px;
  font-size: 14px;
  color: #1b2434;
}
.status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  color: #98a3b8;
}
.status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c4ccda;
}
.status.on i {
  background: #22b573;
}
.status.off i {
  background: #e0a23a;
}
.new-btn {
  flex: none;
  width: 28px;
  height: 28px;
  border: 1px solid #dbe5f5;
  border-radius: 9px;
  background: #fff;
  color: #2f6fe4;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
.new-btn:hover {
  background: #eef4ff;
}
.new-panel {
  margin: 0 8px 8px;
  padding: 9px;
  border: 1px solid #e6edf9;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(31, 66, 135, 0.06);
}
.tabs {
  display: flex;
  gap: 5px;
  margin-bottom: 7px;
}
.tabs button {
  flex: 1;
  padding: 4px;
  border: 0;
  border-radius: 6px;
  background: #f2f5fb;
  color: #6d7a90;
  font-size: 11.5px;
  cursor: pointer;
}
.tabs button.active {
  background: #e6efff;
  color: #2f6fe4;
  font-weight: 600;
}
.control {
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 6px;
  padding: 6px 8px;
  border: 1px solid #e2e8f2;
  border-radius: 7px;
  background: #fafcff;
  color: #374151;
  font-size: 12px;
  outline: none;
}
.control:focus {
  border-color: #9fc0f7;
}
.pick-list {
  max-height: 130px;
  overflow-y: auto;
  margin-bottom: 6px;
}
.pick-list label {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 2px 1px;
  font-size: 11.5px;
  color: #4b5563;
}
.hint {
  margin: 4px 0;
  font-size: 11px;
  color: #9aa5b8;
}
.primary {
  width: 100%;
  padding: 6px;
  border: 0;
  border-radius: 7px;
  background: #2f6fe4;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}
.primary:disabled {
  background: #cfd9e8;
  cursor: not-allowed;
}
.message-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.msg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 18px;
  border-bottom: 1px solid #eef2f8;
  background: #fff;
}
.who h3 {
  margin: 0 0 1px;
  font-size: 14px;
  color: #1b2434;
}
.who span {
  font-size: 11px;
  color: #8b96ab;
}
.members-btn {
  padding: 4px 9px;
  border: 1px solid #e2e8f2;
  border-radius: 7px;
  background: #f8faff;
  color: #5c6a80;
  font-size: 11.5px;
  cursor: pointer;
}
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #fafcff;
}
.welcome-mark {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2f6fe4, #7a5af8);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.welcome h3 {
  margin: 6px 0 0;
  font-size: 15px;
  color: #1b2434;
}
.welcome p {
  margin: 0;
  font-size: 12.5px;
  color: #8b96ab;
}
.error-bar {
  position: absolute;
  left: 50%;
  bottom: 14px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 90%;
  padding: 7px 12px;
  border-radius: 9px;
  background: #fdecee;
  color: #b4232f;
  font-size: 12px;
  box-shadow: 0 6px 18px rgba(180, 35, 47, 0.14);
}
.error-bar button {
  border: 0;
  background: transparent;
  color: #b4232f;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}
@media (max-width: 980px) {
  .chat-shell {
    grid-template-columns: 210px minmax(0, 1fr);
  }
  .chat-shell.with-members {
    grid-template-columns: 210px minmax(0, 1fr);
    position: relative;
  }
  .chat-shell.with-members :deep(.member-panel) {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    width: 210px;
    z-index: 2;
    box-shadow: -8px 0 20px rgba(21, 42, 88, 0.12);
  }
}
</style>
