<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import MessageBubble from './MessageBubble.vue'
import { readLabel, typingName } from '../../chat/store'

const props = defineProps({
  sessionId: { type: Number, default: null },
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  messagesVersion: { type: Number, default: 0 },
  hasMore: { type: Boolean, default: false },
  isGroup: { type: Boolean, default: false },
  currentUserId: { type: Number, default: null }
})
const emit = defineEmits(['load-more', 'recall'])

const listEl = ref(null)
const typing = computed(() => typingName(props.sessionId))

function scrollToBottom() {
  nextTick(() => {
    const el = listEl.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

onMounted(scrollToBottom)
watch(() => props.sessionId, scrollToBottom)
// 打开会话时消息是异步加载的：用消息版本号作为「新列表已就位」的明确信号，
// 在赋值 messages 之后递增，强制定位到最新一条。相比监听 loading，不受批量更新合并影响。
watch(() => props.messagesVersion, scrollToBottom)
watch(() => props.messages.length, (next, prev) => {
  const el = listEl.value
  if (!el) return
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 180
  const last = props.messages[props.messages.length - 1]
  if ((next > prev && nearBottom) || (last && last.senderId === props.currentUserId)) {
    scrollToBottom()
  }
})
</script>

<template>
  <div ref="listEl" class="message-list">
    <button v-if="hasMore" class="load-more" type="button" @click="emit('load-more')">
      加载更早的消息
    </button>
    <p v-if="loading" class="hint">正在加载消息…</p>
    <p v-else-if="!messages.length" class="empty">还没有消息，发一句打个招呼吧</p>

    <MessageBubble
      v-for="message in messages"
      :key="message.id"
      :message="message"
      :mine="message.senderId === currentUserId"
      :show-sender="isGroup"
      :read-text="readLabel(message)"
      @recall="emit('recall', $event)"
    />

    <p v-if="typing" class="typing">{{ typing }} 正在输入…</p>
  </div>
</template>

<style scoped>
.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 18px 8px;
  background: #fafcff;
}
.load-more {
  display: block;
  margin: 0 auto 14px;
  padding: 5px 12px;
  border: 1px solid #e2e8f2;
  border-radius: 12px;
  background: #fff;
  color: #6d7a90;
  font-size: 11.5px;
  cursor: pointer;
}
.hint,
.empty {
  margin: 60px 0;
  text-align: center;
  color: #9aa5b8;
  font-size: 12.5px;
}
.typing {
  margin: 2px 0 10px 40px;
  color: #8b96ab;
  font-size: 11.5px;
}
</style>
