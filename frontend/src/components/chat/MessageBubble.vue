<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getFileUrl } from '../../chat/store'

const props = defineProps({
  message: { type: Object, required: true },
  mine: { type: Boolean, default: false },
  showSender: { type: Boolean, default: false },
  readText: { type: String, default: '' }
})
const emit = defineEmits(['recall'])

const colors = ['#2f6fe4', '#7a5af8', '#0ea5a5', '#e07b39', '#d4457a', '#3b82a0']
const imageUrl = ref('')
const loadingImage = ref(false)

const avatarColor = computed(() => colors[Math.abs(Number(props.message.senderId) || 0) % colors.length])
const initial = computed(() => (props.message.senderName || '?').slice(0, 1))

const recalled = computed(() => props.message.status === 'RECALLED')

function canRecall() {
  if (!props.mine || recalled.value) return false
  const created = new Date(props.message.createTime).getTime()
  return Date.now() - created <= 120 * 1000
}

async function loadImage() {
  if (!props.message.fileId || loadingImage.value) return
  loadingImage.value = true
  imageUrl.value = await getFileUrl(props.message.fileId)
  loadingImage.value = false
}

async function openFile() {
  const url = props.message.msgType === 'IMAGE' && imageUrl.value
    ? imageUrl.value
    : await getFileUrl(props.message.fileId)
  if (url) window.open(url, '_blank')
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function timeText(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  return sameDay ? `${hh}:${mm}` : `${date.getMonth() + 1}-${date.getDate()} ${hh}:${mm}`
}

onMounted(() => {
  if (props.message.msgType === 'IMAGE' && !recalled.value) loadImage()
})

watch(() => props.message.fileId, () => {
  if (props.message.msgType === 'IMAGE' && props.message.fileId && !recalled.value) loadImage()
})
</script>

<template>
  <div :class="['bubble-row', { mine }]">
    <span v-if="!mine" class="avatar" :style="{ background: avatarColor }">{{ initial }}</span>
    <div class="bubble-wrap">
      <span v-if="showSender && !mine" class="sender">{{ message.senderName }}</span>
      <div :class="['bubble', { mine, media: !recalled && message.msgType !== 'TEXT' }]">
        <span v-if="recalled" class="recalled">
          {{ mine ? '你' : (message.senderName || '对方') }}撤回了一条消息
        </span>
        <template v-else-if="message.msgType === 'IMAGE'">
          <img v-if="imageUrl" :src="imageUrl" class="image" alt="图片" @click="openFile" />
          <span v-else class="loading-media">图片加载中…</span>
        </template>
        <button v-else-if="message.msgType === 'FILE'" class="file-card" type="button" @click="openFile">
          <span class="file-icon">FILE</span>
          <span class="file-meta">
            <strong>{{ message.fileName }}</strong>
            <small>{{ formatSize(message.fileSize) }}</small>
          </span>
        </button>
        <span v-else class="text">{{ message.content }}</span>
      </div>
      <div class="meta">
        <span>{{ timeText(message.createTime) }}</span>
        <span v-if="readText" class="read">{{ readText }}</span>
        <button v-if="canRecall()" class="recall" type="button" @click="emit('recall', message.id)">撤回</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}
.bubble-row.mine {
  flex-direction: row-reverse;
}
.avatar {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-width: min(68%, 460px);
}
.mine .bubble-wrap {
  align-items: flex-end;
}
.sender {
  padding-left: 2px;
  font-size: 11px;
  color: #8b96ab;
}
.bubble {
  padding: 9px 12px;
  border-radius: 13px;
  background: #f1f4f9;
  color: #1f2937;
  font-size: 13.5px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble.mine {
  background: linear-gradient(135deg, #2f6fe4, #4a8cf5);
  color: #fff;
}
.bubble.media {
  padding: 5px;
  background: #f1f4f9;
}
.bubble.mine.media {
  background: #e8f0ff;
}
.text {
  white-space: pre-wrap;
}
.recalled {
  font-style: italic;
  color: #96a0b3;
  font-size: 12.5px;
}
.image {
  display: block;
  max-width: 230px;
  max-height: 240px;
  border-radius: 9px;
  cursor: zoom-in;
  object-fit: cover;
}
.loading-media {
  display: inline-block;
  padding: 12px 16px;
  font-size: 12px;
  color: #9aa5b8;
}
.file-card {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 190px;
  padding: 8px 10px;
  border: 0;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.file-icon {
  flex: none;
  padding: 5px 7px;
  border-radius: 7px;
  background: #e8f0ff;
  color: #2f6fe4;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.file-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.file-meta strong {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: #1f2937;
}
.file-meta small {
  font-size: 11px;
  color: #8b96ab;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 3px;
  font-size: 10.5px;
  color: #a0aabd;
}
.read {
  color: #4a8cf5;
}
.mine .read {
  color: #7a9fd8;
}
.recall {
  border: 0;
  background: transparent;
  color: #a0aabd;
  font-size: 10.5px;
  cursor: pointer;
  padding: 0;
}
.recall:hover {
  color: #ef4a5a;
}
</style>
