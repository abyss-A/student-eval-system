<template>
  <section class="card login-card">
    <h2>系统登录</h2>
    <p class="muted">可用测试账号：`stu0001` / `counselor1` / `admin`，默认密码 `123456`</p>

    <div class="grid" style="margin-top: 12px;">
      <label>
        用户名
        <input v-model.trim="form.username" placeholder="例如 stu0001" @keyup.enter="login" />
      </label>

      <label>
        密码
        <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="login" />
      </label>

      <button class="btn" @click="login" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: 'stu0001',
  password: '123456'
})

const login = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const { data } = await http.post('/auth/login', form)
    const payload = data.data

    localStorage.setItem('token', payload.token)
    localStorage.setItem('role', payload.role)
    localStorage.setItem('userId', String(payload.userId))
    localStorage.setItem('realName', payload.realName || '')

    if (payload.role === 'STUDENT') {
      router.replace('/student')
    } else if (payload.role === 'COUNSELOR') {
      router.replace('/teacher')
    } else {
      router.replace('/admin')
    }
  } finally {
    loading.value = false
  }
}
</script>