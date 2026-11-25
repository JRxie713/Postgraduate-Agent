<script setup>
import { computed, reactive, ref } from 'vue'
import {
  batchDownloadSmall,
  checkChunks,
  downloadRange,
  fetchFileMeta,
  mergeChunks,
  saveBlob,
  uploadChunk,
  uploadSmallFiles
} from '../api/file'

// 分片大小：2MB。后端 multipart 上限 20MB，留了足够余量
const CHUNK_SIZE = 2 * 1024 * 1024
// 同时上传的分片数：3 条并发，既快又不会把带宽打满
const UPLOAD_CONCURRENCY = 3
// 并发下载的分段数
const CONCURRENT_SEGMENTS = 4

const formatBytes = (n) => {
  if (!n) return '0 B'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  if (n < 1024 * 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
  return `${(n / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const toHex = (buffer) =>
  [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, '0')).join('')

/** 浏览器支持 Web Crypto 时算 SHA-256；不支持（非安全上下文）返回 null，后端会跳过校验 */
async function sha256Hex(data) {
  if (window.crypto && window.crypto.subtle) {
    return toHex(await window.crypto.subtle.digest('SHA-256', data))
  }
  return null
}

// ============================================================
// 1. 大文件分片上传
// ============================================================
const bigFile = ref(null)
const big = reactive({
  busy: false,
  phase: 'idle', // idle / checking / uploading / merging / done / error
  percent: 0,
  message: '',
  fileId: '',
  totalChunks: 0,
  doneChunks: 0,
  reusedChunks: 0,
  downloadUrl: '',
  demoCorrupt: false,
  chunks: []
})

const chunkStatusText = (c) => {
  if (c.status === 'done') return '已上传'
  if (c.status === 'reused') return '已存在'
  if (c.status === 'uploading') return '上传中'
  if (c.status === 'retrying') return '校验失败-重传'
  if (c.status === 'failed') return '失败'
  return '待上传'
}

function onBigFileChange(event) {
  // 上传过程中不允许换文件：分片队列、fileId 都基于已选文件，换掉会串数据
  if (big.busy) return
  bigFile.value = (event.target.files && event.target.files[0]) || null
  resetBig()
}

function resetBig() {
  big.phase = 'idle'
  big.percent = 0
  big.message = ''
  big.fileId = ''
  big.totalChunks = 0
  big.doneChunks = 0
  big.reusedChunks = 0
  big.downloadUrl = ''
  big.chunks = []
}

// 3 个分片并发上传时，进度回调一帧内会触发多次。
// 用 rAF 把同帧的多次回调合并成一次重算，避免反复全量遍历分片表。
let percentFrame = 0

function updateBigPercent() {
  if (percentFrame) return
  percentFrame = requestAnimationFrame(recalcBigPercent)
}

function recalcBigPercent() {
  percentFrame = 0
  let total = 0
  let loaded = 0
  for (const c of big.chunks) {
    total += c.size
    if (c.status === 'done' || c.status === 'reused') {
      loaded += c.size
    } else {
      loaded += Math.min(c.progress || 0, c.size)
    }
  }
  big.percent = total ? Math.round((loaded / total) * 100) : 0
  big.doneChunks = big.chunks.filter((c) => c.status === 'done' || c.status === 'reused').length
}

/** 同一个文件每次刷新页面都得到同一个 fileId，服务端才能认出「这是上次没传完的那个」 */
async function buildFileId(file) {
  const seed = `${file.name}|${file.size}|${file.lastModified}`
  const hex = await sha256Hex(new TextEncoder().encode(seed))
  return hex ? hex.slice(0, 40) : `f${file.size.toString(36)}${file.lastModified.toString(36)}`
}

async function uploadOneChunk(c) {
  const file = bigFile.value
  const start = (c.index - 1) * CHUNK_SIZE
  const blob = file.slice(start, start + c.size)
  const realHash = await sha256Hex(await blob.arrayBuffer())

  c.status = 'uploading'
  c.progress = 0
  updateBigPercent()

  for (let attempt = 0; attempt < 4; attempt += 1) {
    let hash = realHash
    if (big.demoCorrupt && c.index === 1 && attempt === 0 && hash) {
      // 故意传一个错误哈希，让服务端当场发现这是坏片并删除
      hash = '0'.repeat(64)
      big.message = '第 1 片故意传错哈希 → 服务端比对失败返回 409 并删除坏片 → 前端自动重传'
    }
    const result = await uploadChunk({
      fileId: big.fileId,
      chunkIndex: c.index,
      chunk: blob,
      chunkSha256: hash,
      onProgress: (loaded) => {
        c.progress = Math.min(loaded, c.size)
        updateBigPercent()
      }
    })
    if (result.ok) {
      c.progress = c.size
      c.status = 'done'
      updateBigPercent()
      return
    }
    c.attempts = attempt + 1
    if (result.code !== 409) {
      c.status = 'failed'
      throw new Error(`第 ${c.index} 片上传失败：${result.message}`)
    }
    // 409 = 服务端已删掉这片，直接从 0 重传
    c.status = 'retrying'
    c.progress = 0
    updateBigPercent()
  }
  c.status = 'failed'
  throw new Error(`第 ${c.index} 片连续 4 次校验失败`)
}

/** 3 个 worker 从队列里取分片并发上传 */
async function uploadChunkList(list) {
  const queue = [...list]
  const workers = []
  const count = Math.min(UPLOAD_CONCURRENCY, queue.length)
  for (let i = 0; i < count; i += 1) {
    workers.push(
      (async () => {
        while (queue.length) {
          await uploadOneChunk(queue.shift())
        }
      })()
    )
  }
  await Promise.all(workers)
}

/** 合并；如果合并阶段发现坏片，服务端已删除，重新 check 后只补传缺的片 */
async function mergeWithRepair(file, totalChunks, fileId) {
  for (let round = 0; round < 3; round += 1) {
    const result = await mergeChunks({ fileId, fileName: file.name, totalChunks })
    if (result.code === 200) {
      return result.data
    }
    const check = await checkChunks({ fileId, fileName: file.name, totalChunks })
    const uploaded = new Set((check.data && check.data.uploadedChunks) || [])
    const missing = big.chunks.filter((c) => !uploaded.has(c.index))
    if (!missing.length) {
      throw new Error(result.message || '合并失败')
    }
    big.message = `合并时发现 ${missing.length} 个坏片（已删除），正在补传…`
    for (const c of missing) {
      c.status = 'pending'
      c.progress = 0
    }
    updateBigPercent()
    await uploadChunkList(missing)
  }
  throw new Error('多次合并仍失败，请重试')
}

async function startBigUpload() {
  const file = bigFile.value
  if (!file) {
    big.message = '请先选择一个文件'
    return
  }
  if (big.busy) return
  big.busy = true
  big.message = ''
  big.downloadUrl = ''
  try {
    const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE))
    const fileId = await buildFileId(file)
    big.fileId = fileId
    big.totalChunks = totalChunks
    big.chunks = Array.from({ length: totalChunks }, (_, i) => {
      const start = i * CHUNK_SIZE
      return {
        index: i + 1,
        size: Math.min(CHUNK_SIZE, file.size - start),
        status: 'pending',
        progress: 0,
        attempts: 0
      }
    })
    updateBigPercent()

    // 第一步：问服务端哪些片已经有了（断点续传的关键）
    big.phase = 'checking'
    big.message = '正在检查服务端已有的分片…'
    const check = await checkChunks({ fileId, fileName: file.name, totalChunks })
    if (check.code !== 200) {
      throw new Error(check.message || '检查分片失败')
    }
    const uploaded = new Set(check.data.uploadedChunks || [])
    for (const c of big.chunks) {
      if (uploaded.has(c.index)) {
        c.status = 'reused'
        c.progress = c.size
      }
    }
    big.reusedChunks = uploaded.size
    updateBigPercent()

    const missing = big.chunks.filter((c) => c.status === 'pending')
    big.phase = 'uploading'
    if (missing.length) {
      big.message = `服务端已有 ${uploaded.size} 片，继续上传剩余 ${missing.length} 片`
      await uploadChunkList(missing)
    } else {
      big.message = '服务端已存在全部分片（断点续传命中），直接合并'
    }

    // 第二步：合并
    big.phase = 'merging'
    big.message = '正在按序号合并分片…'
    const url = await mergeWithRepair(file, totalChunks, fileId)
    big.downloadUrl = url
    big.phase = 'done'
    big.message = `上传完成，共 ${totalChunks} 片，已合并为：${url}`
    big.percent = 100

    // 顺手填到下面的下载区，方便继续演示下载
    dl.url = url
    onUrlChange()
  } catch (e) {
    big.phase = 'error'
    big.message = e.message || '上传失败'
  } finally {
    big.busy = false
  }
}

// ============================================================
// 2. 小文件批量上传
// ============================================================
const smallFiles = ref([])
const small = reactive({ uploading: false, percent: 0, message: '', results: [] })

function onSmallFilesChange(event) {
  smallFiles.value = Array.from(event.target.files || [])
  small.message = ''
}

async function uploadSmallBatch() {
  if (!smallFiles.value.length) {
    small.message = '请先选择文件（可以多选）'
    return
  }
  small.uploading = true
  small.percent = 0
  small.message = ''
  small.results = []
  try {
    const result = await uploadSmallFiles(smallFiles.value, (loaded, total) => {
      small.percent = total ? Math.round((loaded / total) * 100) : 0
    })
    if (result.code === 200) {
      small.results = result.data || []
      small.percent = 100
      small.message = `批量上传成功：${small.results.length} 个文件（一次请求完成）`
    } else {
      small.message = result.message || '批量上传失败'
    }
  } catch (e) {
    small.message = e.message || '批量上传失败'
  } finally {
    small.uploading = false
  }
}

async function downloadSmallFile(item) {
  try {
    const part = await downloadRange(`/api/file/small/download/${item.key}`)
    saveBlob(part.blob, item.fileName)
  } catch (e) {
    small.message = e.message || '下载失败'
  }
}

async function downloadSmallZip() {
  if (!small.results.length) return
  try {
    const blob = await batchDownloadSmall(small.results.map((r) => r.key))
    saveBlob(blob, 'small-files.zip')
  } catch (e) {
    small.message = e.message || '打包下载失败'
  }
}

// ============================================================
// 3. 下载：进度条 / 断点续传 / 并发分段
// ============================================================
const dl = reactive({
  url: '',
  storedName: '',
  fileName: '',
  size: 0,
  phase: 'idle', // idle / downloading / paused / done / error
  received: 0,
  percent: 0,
  message: '',
  segments: []
})

let dlController = null
let dlParts = []

function onUrlChange() {
  let raw = (dl.url || '').trim()
  // 允许粘贴完整地址（如 http://localhost:8080/api/file/download/xxx），统一成同源相对路径
  const apiIndex = raw.indexOf('/api/')
  if (apiIndex > 0) {
    raw = raw.slice(apiIndex)
    dl.url = raw
  }
  dl.received = 0
  dl.percent = 0
  dl.size = 0
  dl.fileName = ''
  dl.phase = 'idle'
  dl.message = ''
  dl.segments = []
  dlParts = []
  if (!raw) {
    dl.storedName = ''
    return
  }
  const clean = raw.split('?')[0].replace(/\/+$/, '')
  dl.storedName = clean.split('/').pop() || ''
  if (dl.storedName) {
    loadDownloadMeta()
  }
}

async function loadDownloadMeta() {
  if (!dl.storedName) return
  const result = await fetchFileMeta(dl.storedName)
  if (result.code === 200 && result.data) {
    dl.size = result.data.size
    dl.fileName = result.data.fileName
  } else {
    dl.message = result.message || '读取文件信息失败'
  }
}

function pauseDownload() {
  if (dlController) {
    dlController.abort()
  }
}

async function startDownload() {
  if (!dl.storedName) {
    dl.message = '请先填写下载地址'
    return
  }
  if (!dl.size) {
    await loadDownloadMeta()
  }
  if (!dl.size) {
    dl.message = '无法获取文件大小，请检查地址'
    return
  }

  dl.message = ''
  dl.phase = 'downloading'
  dlController = new AbortController()
  const base = dl.received
  try {
    const part = await downloadRange(dl.url, {
      start: base,
      onProgress: (n) => {
        dl.received = Math.min(dl.size, dl.received + n)
        dl.percent = Math.floor((dl.received / dl.size) * 100)
      },
      signal: dlController.signal
    })
    if (part.blob.size) {
      dlParts.push(part.blob)
    }
    if (part.aborted) {
      dl.phase = 'paused'
      dl.message = `已暂停，已下载 ${formatBytes(dl.received)}，点「继续下载」从断点续传`
      return
    }
    dl.received = dl.size
    dl.percent = 100
    saveBlob(new Blob(dlParts), dl.fileName || 'download.bin')
    dl.phase = 'done'
    dl.message = '下载完成'
  } catch (e) {
    dl.phase = 'error'
    dl.message = e.message || '下载失败'
  }
}

function resetDownload() {
  if (dlController) {
    dlController.abort()
    dlController = null
  }
  dlParts = []
  dl.received = 0
  dl.percent = 0
  dl.phase = 'idle'
  dl.message = ''
  dl.segments = []
}

function updateConcurrentPercent() {
  const total = dl.segments.reduce((sum, s) => sum + s.total, 0)
  const loaded = dl.segments.reduce((sum, s) => sum + s.loaded, 0)
  dl.received = loaded
  dl.percent = total ? Math.floor((loaded / total) * 100) : 0
}

async function startConcurrentDownload() {
  if (!dl.storedName) {
    dl.message = '请先填写下载地址'
    return
  }
  if (!dl.size) {
    await loadDownloadMeta()
  }
  if (!dl.size) {
    dl.message = '无法获取文件大小，请检查地址'
    return
  }

  dl.message = ''
  dl.phase = 'downloading'
  dl.received = 0
  dl.percent = 0

  const total = dl.size
  const segCount = Math.min(CONCURRENT_SEGMENTS, Math.max(1, Math.ceil(total / (256 * 1024))))
  const segSize = Math.ceil(total / segCount)
  dl.segments = Array.from({ length: segCount }, (_, i) => {
    const start = i * segSize
    const end = Math.min(total - 1, start + segSize - 1)
    return { index: i + 1, start, end, total: Math.max(0, end - start + 1), loaded: 0 }
  })

  try {
    const blobs = await Promise.all(
      dl.segments.map(async (segment) => {
        const part = await downloadRange(dl.url, {
          start: segment.start,
          end: segment.end,
          onProgress: (n) => {
            segment.loaded = Math.min(segment.total, segment.loaded + n)
            updateConcurrentPercent()
          }
        })
        return part.blob
      })
    )
    saveBlob(new Blob(blobs), dl.fileName || 'download.bin')
    dl.phase = 'done'
    dl.percent = 100
    dl.message = `${segCount} 条连接并发分段下载完成`
  } catch (e) {
    dl.phase = 'error'
    dl.message = e.message || '并发下载失败'
  }
}

const phaseText = computed(
  () =>
    ({
      idle: '待开始',
      downloading: '下载中',
      paused: '已暂停（可续传）',
      done: '已完成',
      error: '出错'
    })[dl.phase] || ''
)
</script>

<template>
  <div class="file-page">
    <!-- 1. 大文件分片上传 -->
    <section class="card">
      <div class="card-head">
        <h2>大文件分片上传</h2>
        <span class="hint">2MB 一片 · 3 片并发 · 坏片自动重传 · 刷新后续传</span>
      </div>

      <div class="row">
        <input type="file" :disabled="big.busy" @change="onBigFileChange" />
        <button type="button" :disabled="big.busy || !bigFile" @click="startBigUpload">
          {{ big.busy ? '处理中…' : '开始分片上传' }}
        </button>
        <label class="check">
          <input v-model="big.demoCorrupt" type="checkbox" />
          演示坏片重传（第 1 片先传错一次）
        </label>
      </div>

      <p v-if="bigFile" class="meta">
        {{ bigFile.name }} · {{ formatBytes(bigFile.size) }} ·
        共 {{ Math.max(1, Math.ceil(bigFile.size / CHUNK_SIZE)) }} 片
      </p>

      <div class="bar-wrap">
        <div class="bar"><div class="bar-inner" :style="{ width: big.percent + '%' }"></div></div>
        <span class="bar-text">{{ big.percent }}%</span>
      </div>

      <p class="meta">
        已处理 {{ big.doneChunks }} / {{ big.totalChunks }} 片
        <template v-if="big.reusedChunks">（其中 {{ big.reusedChunks }} 片命中续传）</template>
        <template v-if="big.fileId">· fileId: {{ big.fileId.slice(0, 12) }}…</template>
      </p>

      <p v-if="big.message" :class="['msg', big.phase === 'error' ? 'err' : '']">{{ big.message }}</p>

      <div v-if="big.chunks.length" class="chunk-grid">
        <div
          v-for="c in big.chunks"
          :key="c.index"
          :class="['chunk', c.status]"
          :title="`第 ${c.index} 片 · ${formatBytes(c.size)} · ${chunkStatusText(c)}`"
        >
          <b>{{ c.index }}</b>
          <small>{{ chunkStatusText(c) }}</small>
        </div>
      </div>
    </section>

    <!-- 2. 小文件批量上传 -->
    <section class="card">
      <div class="card-head">
        <h2>小文件批量上传</h2>
        <span class="hint">一次请求携带多个文件 · 合并存储到 64MB 大块</span>
      </div>

      <div class="row">
        <input type="file" multiple @change="onSmallFilesChange" />
        <button type="button" :disabled="small.uploading || !smallFiles.length" @click="uploadSmallBatch">
          {{ small.uploading ? '上传中…' : '批量上传' }}
        </button>
        <button
          v-if="small.results.length"
          type="button"
          class="ghost"
          @click="downloadSmallZip"
        >
          打包下载 zip
        </button>
      </div>

      <p v-if="smallFiles.length" class="meta">
        已选择 {{ smallFiles.length }} 个：{{ smallFiles.map((f) => f.name).join('、') }}
      </p>

      <div class="bar-wrap">
        <div class="bar"><div class="bar-inner" :style="{ width: small.percent + '%' }"></div></div>
        <span class="bar-text">{{ small.percent }}%</span>
      </div>

      <p v-if="small.message" class="msg">{{ small.message }}</p>

      <div v-if="small.results.length" class="table">
        <div class="table-row table-head">
          <span>文件名</span><span>大小</span><span>key</span><span>block / offset</span><span>操作</span>
        </div>
        <div v-for="item in small.results" :key="item.key" class="table-row">
          <span>{{ item.fileName }}</span>
          <span>{{ formatBytes(item.size) }}</span>
          <span class="mono">{{ item.key }}</span>
          <span class="mono">{{ item.block }} / {{ item.offset }}</span>
          <span><button class="link" type="button" @click="downloadSmallFile(item)">下载</button></span>
        </div>
      </div>
    </section>

    <!-- 3. 下载 -->
    <section class="card">
      <div class="card-head">
        <h2>下载：进度条 / 断点续传 / 并发分段</h2>
        <span class="hint">基于 HTTP Range（206）</span>
      </div>

      <div class="row">
        <input
          v-model="dl.url"
          class="url"
          placeholder="粘贴下载地址，例如 /api/file/download/abcd1234_xxx.zip"
          @change="onUrlChange"
        />
        <button type="button" class="ghost" @click="onUrlChange">读取信息</button>
      </div>

      <p class="meta">
        <template v-if="dl.size">
          文件：{{ dl.fileName || dl.storedName }} · {{ formatBytes(dl.size) }} ·
          已下载 {{ formatBytes(dl.received) }}
        </template>
        <template v-else>填写地址后可读取文件大小</template>
      </p>

      <div class="bar-wrap">
        <div class="bar"><div class="bar-inner" :style="{ width: dl.percent + '%' }"></div></div>
        <span class="bar-text">{{ dl.percent }}%</span>
      </div>

      <div class="row">
        <button v-if="dl.phase !== 'downloading'" type="button" :disabled="!dl.storedName" @click="startDownload">
          {{ dl.phase === 'paused' ? '继续下载（断点续传）' : '开始下载' }}
        </button>
        <button v-else type="button" class="ghost" @click="pauseDownload">暂停</button>
        <button
          type="button"
          :disabled="dl.phase === 'downloading' || !dl.storedName"
          @click="startConcurrentDownload"
        >
          4 线程并发下载
        </button>
        <button type="button" class="ghost" @click="resetDownload">重置</button>
      </div>

      <p v-if="dl.message" :class="['msg', dl.phase === 'error' ? 'err' : '']">{{ dl.message }}</p>

      <div v-if="dl.segments.length" class="segments">
        <div v-for="s in dl.segments" :key="s.index" class="segment">
          <span>连接 {{ s.index }}：{{ formatBytes(s.start) }} - {{ formatBytes(s.end) }}</span>
          <div class="bar slim">
            <div class="bar-inner" :style="{ width: (s.total ? Math.floor((s.loaded / s.total) * 100) : 0) + '%' }"></div>
          </div>
        </div>
      </div>

      <p class="meta">当前状态：{{ phaseText }}</p>
    </section>
  </div>
</template>

<style scoped>
.file-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.card {
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 14px;
}
.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}
.card-head h2 {
  margin: 0;
  font-size: 17px;
  color: #1c3f7a;
}
.hint {
  font-size: 12px;
  color: #8b97ad;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
button {
  padding: 7px 14px;
  border: none;
  border-radius: 8px;
  background: #2f6fe4;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
}
button:disabled {
  background: #b7c7e6;
  cursor: not-allowed;
}
button.ghost {
  background: #ffffff;
  border: 1px solid #dde4ee;
  color: #46526b;
}
button.link {
  background: none;
  color: #2f6fe4;
  padding: 0;
}
.check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #46526b;
}
input[type='file'] {
  font-size: 13px;
  color: #46526b;
}
input.url {
  flex: 1;
  min-width: 280px;
  padding: 8px 10px;
  border: 1px solid #dde4ee;
  border-radius: 8px;
  font-size: 13px;
}
.meta {
  margin: 6px 0;
  font-size: 12px;
  color: #6b7891;
}
.msg {
  margin: 6px 0;
  font-size: 13px;
  color: #2f6fe4;
}
.msg.err {
  color: #d64545;
}
.bar-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0;
}
.bar {
  flex: 1;
  height: 8px;
  background: #eef2f9;
  border-radius: 999px;
  overflow: hidden;
}
.bar.slim {
  height: 5px;
  margin-top: 4px;
}
.bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #2f6fe4, #5b9bff);
  border-radius: 999px;
  transition: width 0.2s ease;
}
.bar-text {
  width: 42px;
  text-align: right;
  font-size: 12px;
  color: #46526b;
}
.chunk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(74px, 1fr));
  gap: 6px;
  margin-top: 10px;
  max-height: 168px;
  overflow: auto;
}
.chunk {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 4px;
  border-radius: 8px;
  background: #f4f7fb;
  color: #6b7891;
  font-size: 11px;
}
.chunk b {
  font-size: 12px;
}
.chunk.done {
  background: #e6f5ec;
  color: #1f7a45;
}
.chunk.reused {
  background: #e8f0ff;
  color: #1c4fb8;
}
.chunk.uploading {
  background: #fff4e0;
  color: #a86600;
}
.chunk.retrying,
.chunk.failed {
  background: #fdeaea;
  color: #b93a3a;
}
.table {
  margin-top: 12px;
  border: 1px solid #eef2f9;
  border-radius: 10px;
  overflow: hidden;
}
.table-row {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr 0.9fr 1.2fr 0.5fr;
  gap: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: #46526b;
  border-top: 1px solid #f2f5fa;
}
.table-head {
  background: #f8fafd;
  color: #8b97ad;
  border-top: none;
}
.mono {
  font-family: Consolas, monospace;
  font-size: 11px;
}
.segments {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 10px 0;
}
.segment {
  font-size: 12px;
  color: #6b7891;
}
</style>
