<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  members: { type: Array, default: () => [] },
  contacts: { type: Array, default: () => [] },
  ownerId: { type: Number, default: null },
  isOwner: { type: Boolean, default: false }
})
const emit = defineEmits(['add'])

const colors = ['#2f6fe4', '#7a5af8', '#0ea5a5', '#e07b39', '#d4457a', '#3b82a0']
const adding = ref(false)
const selected = ref([])

const available = computed(() => {
  const ids = new Set(props.members.map((member) => member.userId))
  return props.contacts.filter((contact) => !ids.has(contact.id))
})

function initial(name) {
  return (name || '?').slice(0, 1)
}

function color(id) {
  return colors[Math.abs(Number(id) || 0) % colors.length]
}

function submit() {
  if (!selected.value.length) return
  emit('add', [...selected.value])
  selected.value = []
  adding.value = false
}

function cancel() {
  selected.value = []
  adding.value = false
}
</script>

<template>
  <aside class="member-panel">
    <header class="head">
      <h3>群成员</h3>
      <span>{{ members.length }} 人</span>
    </header>
    <ul class="list">
      <li v-for="member in members" :key="member.userId">
        <span class="avatar" :style="{ background: color(member.userId) }">{{ initial(member.name) }}</span>
        <span class="name">{{ member.name }}</span>
        <em v-if="member.userId === ownerId" class="tag">群主</em>
        <em v-else-if="member.userRole === 'TEACHER'" class="tag teacher">导师</em>
      </li>
    </ul>

    <div v-if="isOwner" class="add-box">
      <button v-if="!adding" class="add-btn" type="button" @click="adding = true">+ 添加成员</button>
      <template v-else>
        <p v-if="!available.length" class="hint">没有可添加的联系人</p>
        <label v-for="contact in available" :key="contact.id" class="pick">
          <input v-model="selected" type="checkbox" :value="contact.id" />
          <span>{{ contact.name }}</span>
        </label>
        <div class="actions">
          <button type="button" :disabled="!selected.length" @click="submit">确认</button>
          <button type="button" class="ghost" @click="cancel">取消</button>
        </div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.member-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-left: 1px solid #eef2f8;
  background: #fbfcff;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 8px;
}
.head h3 {
  margin: 0;
  font-size: 13px;
  color: #1f2937;
}
.head span {
  font-size: 11px;
  color: #8b96ab;
}
.list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin: 0;
  padding: 4px 8px;
  list-style: none;
}
.list li {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 4px;
}
.avatar {
  flex: none;
  width: 27px;
  height: 27px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}
.name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #374151;
}
.tag {
  flex: none;
  padding: 1px 5px;
  border-radius: 5px;
  background: #eef2f8;
  color: #7a8699;
  font-size: 9.5px;
  font-style: normal;
}
.tag.teacher {
  background: #eef3ff;
  color: #2f6fe4;
}
.add-box {
  padding: 10px;
  border-top: 1px solid #eef2f8;
}
.add-btn {
  width: 100%;
  padding: 6px;
  border: 1px dashed #c9d6ea;
  border-radius: 8px;
  background: transparent;
  color: #5c6a80;
  font-size: 11.5px;
  cursor: pointer;
}
.pick {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 2px;
  font-size: 11.5px;
  color: #4b5563;
}
.actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
.actions button {
  flex: 1;
  padding: 5px;
  border: 0;
  border-radius: 7px;
  background: #2f6fe4;
  color: #fff;
  font-size: 11px;
  cursor: pointer;
}
.actions button:disabled {
  background: #cfd9e8;
}
.actions .ghost {
  background: #eef2f8;
  color: #5c6a80;
}
.hint {
  margin: 4px 0;
  font-size: 11px;
  color: #9aa5b8;
}
.add-box {
  max-height: 220px;
  overflow-y: auto;
}
</style>
