<script setup>
const props = defineProps({
  sessions: { type: Array, default: () => [] },
  currentId: { type: [Number, String], default: null }
})
const emit = defineEmits(['select'])

const colors = ['#2f6fe4', '#7a5af8', '#0ea5a5', '#e07b39', '#d4457a', '#3b82a0']

function initial(name) {
  return (name || '聊').slice(0, 1)
}

function color(id) {
  return colors[Math.abs(Number(id) || 0) % colors.length]
}

function timeText(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  if (sameDay) return `${hh}:${mm}`
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<template>
  <div class="session-list">
    <button
      v-for="session in sessions"
      :key="session.id"
      :class="['session-item', { active: session.id === currentId }]"
      type="button"
      @click="emit('select', session.id)"
    >
      <span class="avatar" :style="{ background: color(session.peerId || session.id) }">
        {{ session.type === 'GROUP' ? '群' : initial(session.name) }}
      </span>
      <span class="body">
        <span class="line">
          <strong class="name">{{ session.name || '未命名会话' }}</strong>
          <time class="time">{{ timeText(session.lastMsgTime) }}</time>
        </span>
        <span class="line">
          <span class="preview">{{ session.lastMsgPreview || '暂无消息' }}</span>
          <span v-if="session.unreadCount" class="badge">{{ session.unreadCount > 99 ? '99+' : session.unreadCount }}</span>
        </span>
      </span>
    </button>
    <p v-if="!sessions.length" class="empty">还没有会话，点击右上角 + 发起私聊或创建自习室</p>
  </div>
</template>

<style scoped>
.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 6px;
}
.session-item {
  display: flex;
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}
.session-item:hover {
  background: #f1f5fd;
}
.session-item.active {
  background: #e8f0ff;
}
.avatar {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  justify-content: center;
}
.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.name {
  font-size: 13px;
  color: #1b2434;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  flex: none;
  font-size: 11px;
  color: #98a3b8;
}
.preview {
  font-size: 12px;
  color: #77829a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ef4a5a;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}
.empty {
  margin: 24px 12px;
  font-size: 12px;
  line-height: 1.7;
  color: #9aa5b8;
  text-align: center;
}
</style>
