<template>
  <section class="card" style="max-width:460px;margin:50px auto;">
    <h2>系统登录</h2>
    <div class="grid" style="margin-top:12px;">
      <label>用户名 <input v-model="form.username" placeholder="如: stu0001 / counselor1 / admin" /></label>
      <label>密码 <input v-model="form.password" type="password" placeholder="默认 123456" /></label>
      <button class="btn" @click="login" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'stu0001', password: '123456' })

const login = async () => {
  loading.value = true
  try {
    const { data } = await http.post('/auth/login', form)
    localStorage.setItem('token', data.data.token)
    localStorage.setItem('role', data.data.role)
    localStorage.setItem('userId', String(data.data.userId))
    localStorage.setItem('realName', data.data.realName)
    if (data.data.role === 'STUDENT') router.push('/student')
    else if (data.data.role === 'COUNSELOR') router.push('/teacher')
    else router.push('/admin')
  } finally {
    loading.value = false
  }
}
</script>
