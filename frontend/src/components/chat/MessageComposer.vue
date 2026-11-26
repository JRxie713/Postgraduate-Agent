<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  uploading: { type: Boolean, default: false }
})
const emit = defineEmits(['send-text', 'send-file', 'typing'])

const text = ref('')
const imageInput = ref(null)
const fileInput = ref(null)

function submit() {
  const value = text.value.trim()
  if (!value || props.disabled) return
  emit('send-text', value)
  text.value = ''
}

function onEnter(event) {
  if (event.isComposing) return
  event.preventDefault()
  submit()
}

function pickImage(event) {
  const file = event.target.files && event.target.files[0]
  if (file) emit('send-file', file)
  event.target.value = ''
}

function pickFile(event) {
  const file = event.target.files && event.target.files[0]
  if (file) emit('send-file', file)
  event.target.value = ''
}
</script>

<template>
  <div class="composer">
    <div class="tools">
      <button type="button" class="tool" title="发送图片" @click="imageInput.click()">图片</button>
      <button type="button" class="tool" title="发送文件" @click="fileInput.click()">文件</button>
      <span v-if="uploading" class="uploading">正在上传…</span>
    </div>
    <textarea
      v-model="text"
      class="input"
      rows="2"
      placeholder="输入消息，Enter 发送，Shift + Enter 换行"
      :disabled="disabled"
      @input="emit('typing')"
      @keydown.enter.exact="onEnter"
    ></textarea>
    <button class="send" type="button" :disabled="disabled || !text.trim()" @click="submit">发送</button>
    <input ref="imageInput" type="file" accept="image/*" hidden @change="pickImage" />
    <input ref="fileInput" type="file" hidden @change="pickFile" />
  </div>
</template>

<style scoped>
.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  gap: 6px 10px;
  padding: 10px 14px 12px;
  border-top: 1px solid #edf1f7;
  background: #fff;
}
.tools {
  grid-column: 1 / 2;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tool {
  border: 1px solid #e4eaf4;
  border-radius: 8px;
  background: #f8faff;
  padding: 3px 9px;
  color: #5c6a80;
  font-size: 11.5px;
  cursor: pointer;
}
.tool:hover {
  border-color: #c9d8f2;
  color: #2f6fe4;
}
.uploading {
  font-size: 11.5px;
  color: #2f6fe4;
}
.input {
  grid-column: 1 / 2;
  width: 100%;
  box-sizing: border-box;
  resize: none;
  border: 1px solid #e4eaf4;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.5;
  color: #1f2937;
  background: #fafcff;
  outline: none;
  font-family: inherit;
}
.input:focus {
  border-color: #9fc0f7;
  background: #fff;
}
.send {
  grid-column: 2 / 3;
  grid-row: 1 / 3;
  align-self: end;
  min-width: 74px;
  height: 60px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f6fe4, #4a8cf5);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.send:disabled {
  background: #cfd9e8;
  cursor: not-allowed;
}
</style>
