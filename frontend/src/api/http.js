const BASE = '/api'

export const TOKEN_KEY = 'accessToken'
export const REFRESH_KEY = 'refreshToken'

/**
 * 统一请求封装：自动携带 Bearer 令牌，统一解析 Result 结构
 */
export async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const token = localStorage.getItem(TOKEN_KEY)
  if (auth && token) {
    headers.Authorization = `Bearer ${token}`
  }

  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    })
  } catch (e) {
    return { code: 0, message: '无法连接后端服务，请确认 8080 端口已启动' }
  }

  try {
    return await res.json()
  } catch (e) {
    return { code: res.status, message: `请求失败（HTTP ${res.status}）` }
  }
}
