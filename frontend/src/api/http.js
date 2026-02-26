import axios from 'axios'

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
    const msg = err?.response?.data?.message || err.message || '请求失败'
    alert(msg)
    return Promise.reject(err)
  }
)

export default http
