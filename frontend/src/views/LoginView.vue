<template>
  <div class="login-wrap">
    <div class="login-box">
      <section class="login-hero" aria-hidden="true">
        <img class="login-illus" :src="illus" alt="校园插画" />
        <div class="login-hero-text">
          <h1 class="login-title">大学生综合测评管理系统</h1>
          <p class="login-sub">综合测评填报、审核、终审与排名公示</p>
        </div>
      </section>

      <section class="login-form card">
        <h2 class="form-title">账号登录</h2>
        <p class="muted" style="margin-top: 6px;">请输入用户名与密码登录系统</p>

        <form class="stack" style="margin-top: 12px;" @submit.prevent="login">
          <label class="field">
            <span class="field-label">用户名</span>
            <input v-model.trim="form.username" placeholder="例如：stu0001" autocomplete="username" />
          </label>

          <label class="field">
            <span class="field-label">密码</span>
            <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
          </label>

          <button class="btn" type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>

          <div class="login-quick">
            <button class="btn secondary" type="button" @click="fill('STUDENT')" :disabled="loading">填充学生</button>
            <button class="btn secondary" type="button" @click="fill('COUNSELOR')" :disabled="loading">填充辅导员</button>
            <button class="btn secondary" type="button" @click="fill('ADMIN')" :disabled="loading">填充管理员</button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import illus from '../assets/login-illustration.svg'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: 'stu0001',
  password: '123456'
})

const fill = (role) => {
  if (role === 'STUDENT') form.username = 'stu0001'
  else if (role === 'COUNSELOR') form.username = 'counselor1'
  else form.username = 'admin'
  form.password = '123456'
}

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

