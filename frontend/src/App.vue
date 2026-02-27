<template>
  <div class="shell">
    <header v-if="!isLoginPage" class="topbar">
      <div class="brand">
        <h1>大学生综合测评管理系统</h1>
        <p v-if="state.loggedIn" class="subtitle">{{ state.realName || '未命名用户' }} · {{ roleText }}</p>
      </div>

      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
        <button v-if="state.loggedIn" class="btn ghost" @click="logout">退出登录</button>
      </nav>
    </header>

    <main :class="isLoginPage ? 'login-main' : 'page'">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getRole, getRealName, getToken, roleLabel } from './utils/auth'

const router = useRouter()
const route = useRoute()

const state = reactive({
  loggedIn: false,
  role: '',
  realName: ''
})

const syncAuthState = () => {
  state.loggedIn = Boolean(getToken())
  state.role = getRole()
  state.realName = getRealName()
}

syncAuthState()
watch(() => route.fullPath, syncAuthState)

const roleText = computed(() => roleLabel(state.role))
const isLoginPage = computed(() => route.path === '/login')

const navItems = computed(() => {
  if (!state.loggedIn) {
    return [{ to: '/login', label: '登录' }]
  }

  if (state.role === 'STUDENT') {
    return [
      { to: '/student', label: '学生端' },
      { to: '/ranking', label: '排名' }
    ]
  }

  if (state.role === 'COUNSELOR') {
    return [
      { to: '/teacher', label: '辅导员' },
      { to: '/ranking', label: '排名' }
    ]
  }

  if (state.role === 'ADMIN') {
    return [
      { to: '/admin', label: '管理员' },
      { to: '/teacher', label: '辅导员审核' },
      { to: '/ranking', label: '排名' }
    ]
  }

  return [{ to: '/login', label: '登录' }]
})

const logout = () => {
  clearAuth()
  syncAuthState()
  router.replace('/login')
}
</script>
