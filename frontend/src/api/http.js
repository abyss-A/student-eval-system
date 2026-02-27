import axios from 'axios'
import { clearAuth } from '../utils/auth'

const DEFAULT_BASE_URL = 'http://localhost:8080/api/v1'
// Optional: set `VITE_API_BASE_URL` in `frontend/.env` to override.
const API_BASE_URL = String(import.meta?.env?.VITE_API_BASE_URL || DEFAULT_BASE_URL).trim()
const DEFAULT_BACKEND_ORIGIN = DEFAULT_BASE_URL.replace(/\/api\/v1$/, '')

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
})

function buildRequestDebug(config) {
  const method = String(config?.method || 'GET').toUpperCase()
  const url = `${config?.baseURL || API_BASE_URL}${config?.url || ''}`
  return `${method} ${url}`
}

function pickMessageFromResponse(resp) {
  const data = resp?.data
  if (!data) return ''
  if (typeof data === 'string') return data.trim()
  if (typeof data === 'object') {
    if (typeof data.message === 'string' && data.message.trim()) return data.message.trim()
    if (typeof data.error === 'string' && data.error.trim()) return data.error.trim()
  }
  return ''
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    // Backend统一返回 ApiResponse：{code,message,data}
    const data = resp?.data
    if (data && typeof data === 'object' && typeof data.code === 'number' && data.code !== 0) {
      const err = new Error(data.message || '请求失败')
      err.bizCode = data.code
      err.userMessage = data.message || '请求失败'
      err.response = resp

      // 业务级未登录：强制退回登录页
      if (data.code === 40101) {
        clearAuth()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else if (!resp?.config?.meta?.silent) {
        alert(err.userMessage)
      }
      return Promise.reject(err)
    }
    return resp
  },
  (err) => {
    const status = err?.response?.status
    const silent = Boolean(err?.config?.meta?.silent)

    // HTTP级未登录：强制退回登录页
    if (status === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(err)
    }

    let msg = pickMessageFromResponse(err?.response)
    if (!msg) {
      if (!status) {
        msg = `无法连接后端，请确认后端已启动：${DEFAULT_BACKEND_ORIGIN}`
      } else if (status === 404) {
        msg =
          '接口不存在（404）。通常是后端未更新或未重启导致的。\n' +
          '如果你刚更新了代码，请重新启动后端；如新增了数据表，还需要执行 init-db 脚本。\n' +
          `请求：${buildRequestDebug(err?.config)}`
      } else if (status === 403) {
        msg = '无权限访问（403）。请确认当前账号角色是否正确。'
      } else if (status >= 500) {
        msg = `服务器异常（HTTP ${status}）。请查看后端控制台/日志。`
      } else {
        msg = `请求失败（HTTP ${status}）。`
      }
    }

    err.userMessage = msg
    if (!silent) {
      alert(msg)
    }
    return Promise.reject(err)
  }
)

export default http
