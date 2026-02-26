import axios from 'axios'
import { clearAuth } from '../utils/auth'

const http = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const status = err?.response?.status
    const msg = err?.response?.data?.message || err.message || '请求失败'

    if (status === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(err)
    }

    alert(msg)
    return Promise.reject(err)
  }
)

export default http