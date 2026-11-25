import { TOKEN_KEY } from './http'

const BASE = '/api'

function authHeaders(extra = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  return token ? { ...extra, Authorization: `Bearer ${token}` } : { ...extra }
}

/** 分片上传前检查：返回服务端已收到的分片序号，以及每片上传时记录的哈希（用于续传） */
export async function checkChunks({ fileId, fileName, totalChunks }) {
  const res = await fetch(`${BASE}/file/chunk/check`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ fileId, fileName, totalChunks })
  })
  return res.json()
}

/**
 * 上传单个分片。
 * 用 XHR 而不是 fetch，是因为只有 XHR 的 upload.onprogress 能拿到上传进度。
 * 返回 { ok, code, message }；code === 409 表示服务端判定该片损坏（已删除），需要重传。
 */
export function uploadChunk({ fileId, chunkIndex, chunk, chunkSha256, onProgress }) {
  return new Promise((resolve) => {
    const form = new FormData()
    form.append('fileId', fileId)
    form.append('chunkIndex', String(chunkIndex))
    if (chunkSha256) {
      form.append('chunkSha256', chunkSha256)
    }
    form.append('file', chunk, `${fileId}-${chunkIndex}.part`)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${BASE}/file/chunk/upload`)
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress(e.loaded, e.total)
      }
    }
    xhr.onload = () => {
      let body = null
      try {
        body = JSON.parse(xhr.responseText)
      } catch (e) {
        // 非 JSON 响应（例如网关错误页），下面统一按 HTTP 状态处理
      }
      resolve({
        ok: xhr.status === 200 && !!body && body.code === 200,
        code: body && body.code != null ? body.code : xhr.status,
        message: (body && body.message) || `HTTP ${xhr.status}`
      })
    }
    xhr.onerror = () => resolve({ ok: false, code: 0, message: '网络异常，分片上传失败' })
    xhr.send(form)
  })
}

/** 合并分片，返回合并后文件的下载地址 */
export async function mergeChunks({ fileId, fileName, totalChunks }) {
  const form = new FormData()
  form.append('fileId', fileId)
  form.append('fileName', fileName)
  form.append('totalChunks', String(totalChunks))
  const res = await fetch(`${BASE}/file/chunk/merge`, {
    method: 'POST',
    headers: authHeaders(),
    body: form
  })
  return res.json()
}

/** 小文件批量上传：一次请求带多个 files 字段，XHR 拿整体进度 */
export function uploadSmallFiles(files, onProgress) {
  return new Promise((resolve) => {
    const form = new FormData()
    for (const file of files) {
      form.append('files', file, file.name)
    }

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${BASE}/file/small/batch-upload`)
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress(e.loaded, e.total)
      }
    }
    xhr.onload = () => {
      let body = null
      try {
        body = JSON.parse(xhr.responseText)
      } catch (e) {
        // 忽略非 JSON 响应
      }
      resolve(body || { code: xhr.status, message: `HTTP ${xhr.status}` })
    }
    xhr.onerror = () => resolve({ code: 0, message: '网络异常，批量上传失败' })
    xhr.send(form)
  })
}

/** 读取文件元信息（大小、原始文件名），记住：参数是下载地址里的存储名 */
export async function fetchFileMeta(storedName) {
  const res = await fetch(`${BASE}/file/meta/${encodeURIComponent(storedName)}`, {
    headers: authHeaders()
  })
  return res.json()
}

/**
 * 下载指定字节区间，返回 { blob, received, aborted }。
 * start/end 会被拼成 HTTP Range 头：
 *   - 暂停后从 start = 已下载字节数 继续 → 断点续传
 *   - 多段同时请求 → 并发分段下载
 */
export async function downloadRange(url, { start = 0, end, onProgress, signal } = {}) {
  const headers = authHeaders()
  headers.Range = `bytes=${start}-${end == null ? '' : end}`

  let res
  try {
    res = await fetch(url, { headers, signal })
  } catch (e) {
    if (e.name === 'AbortError') {
      return { blob: new Blob([]), received: 0, aborted: true }
    }
    throw e
  }

  if (res.status !== 200 && res.status !== 206) {
    throw new Error(`下载失败（HTTP ${res.status}）`)
  }

  const chunks = []
  let received = 0
  if (res.body) {
    const reader = res.body.getReader()
    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          break
        }
        chunks.push(value)
        received += value.length
        if (onProgress) {
          onProgress(value.length)
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') {
        throw e
      }
    }
  }

  return { blob: new Blob(chunks), received, aborted: signal ? signal.aborted : false }
}

/** 小文件打包下载：多个 key 打包成一个 zip */
export async function batchDownloadSmall(keys) {
  const res = await fetch(`${BASE}/file/small/batch-download`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(keys)
  })
  if (!res.ok) {
    throw new Error(`打包下载失败（HTTP ${res.status}）`)
  }
  return res.blob()
}

/** 用浏览器把 Blob 存成文件 */
export function saveBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName || 'download.bin'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 延迟释放，避免个别浏览器还没开始下载就把 URL 回收
  setTimeout(() => URL.revokeObjectURL(url), 4000)
}
