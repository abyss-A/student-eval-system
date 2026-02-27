<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-info">
        <h1 class="login-title">大学生综合测评管理系统</h1>
        <p class="muted" style="margin-top:10px;">
          面向中国高校学生综合测评填报与审核。支持课程成绩、德智体美劳活动填报、证明图片上传、辅导员审核与管理员终审。
        </p>

        <div class="login-flow">
          <span class="flow-item">学生填报</span>
          <span class="flow-sep">→</span>
          <span class="flow-item">辅导员审核</span>
          <span class="flow-sep">→</span>
          <span class="flow-item">管理员终审</span>
          <span class="flow-sep">→</span>
          <span class="flow-item">导出报告</span>
        </div>

        <ul class="login-list">
          <li>学生端：按模块分区填写活动，证明材料只支持 JPG/PNG 图片</li>
          <li>辅导员端：逐项审核课程/活动，可查看证明图片</li>
          <li>管理员端：终审测评单，查看排名</li>
        </ul>

        <p class="muted" style="margin-top:14px;">
          测试账号：学生 <code>stu0001</code>，辅导员 <code>counselor1</code>，管理员 <code>admin</code>，默认密码 <code>123456</code>
        </p>
      </div>

      <section class="card login-card">
        <h2 style="margin:0;">账号登录</h2>
        <p class="muted" style="margin-top:8px;">请输入用户名与密码登录系统。</p>

        <div class="grid" style="margin-top: 12px;">
          <label>
            用户名
            <input v-model.trim="form.username" placeholder="例如：stu0001" @keyup.enter="login" />
          </label>

          <label>
            密码
            <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="login" />
          </label>

          <button class="btn" @click="login" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>

        <div class="toolbar-row" style="margin-top: 12px;">
          <button class="btn secondary" @click="fill('STUDENT')" :disabled="loading">填充学生</button>
          <button class="btn secondary" @click="fill('COUNSELOR')" :disabled="loading">填充辅导员</button>
          <button class="btn secondary" @click="fill('ADMIN')" :disabled="loading">填充管理员</button>
        </div>
      </section>
    </div>
  </div>
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
