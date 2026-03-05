<template>
  <div class="fb-login-page">
    <div class="fb-login-main">
      <section class="fb-brand-panel" aria-hidden="true">
        <p class="fb-brand-logo">综测系统</p>
        <h1 class="fb-brand-title">大学生综合测评管理系统</h1>
        <p class="fb-brand-subtitle">连接学生、辅导员与管理员，完成测评填报、审核与公示。</p>
      </section>

      <section class="fb-login-panel">
        <div class="fb-login-card">
          <p v-if="loginState.success" class="form-banner success">{{ loginState.success }}</p>
          <p v-if="loginErrors.form" class="form-banner error">{{ loginErrors.form }}</p>
          <p v-if="loginState.info" class="form-banner info">{{ loginState.info }}</p>

          <form class="stack" @submit.prevent="login" novalidate>
            <label class="field">
              <span class="visually-hidden">学号/工号</span>
              <input
                v-model.trim="loginForm.accountNo"
                :class="{ 'is-error': Boolean(loginErrors.accountNo) }"
                placeholder="请输入学号/工号"
                autocomplete="username"
                :aria-invalid="Boolean(loginErrors.accountNo)"
                aria-describedby="login-accountno-error"
                @blur="validateLoginField('accountNo')"
                @input="clearLoginFieldError('accountNo')"
              />
              <span id="login-accountno-error" class="field-error">{{ loginErrors.accountNo }}</span>
            </label>

            <label class="field">
              <span class="visually-hidden">密码</span>
              <div class="fb-password-wrap">
                <input
                  ref="loginPasswordRef"
                  v-model="loginForm.password"
                  :type="showLoginPassword ? 'text' : 'password'"
                  :class="{ 'is-error': Boolean(loginErrors.password) }"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                  :aria-invalid="Boolean(loginErrors.password)"
                  aria-describedby="login-password-error"
                  @blur="validateLoginField('password')"
                  @input="clearLoginFieldError('password')"
                />
                <button
                  class="fb-password-toggle"
                  type="button"
                  :aria-label="showLoginPassword ? '隐藏密码' : '显示密码'"
                  @click="showLoginPassword = !showLoginPassword"
                >
                  {{ showLoginPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <span id="login-password-error" class="field-error">{{ loginErrors.password }}</span>
            </label>

            <label class="fb-remember-line">
              <input v-model="rememberAccountNo" type="checkbox" />
              <span>记住学号/工号</span>
            </label>

            <button class="fb-btn fb-btn-primary" type="submit" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>

          <button class="fb-link-btn" type="button" :disabled="loading" @click="onForgotPassword">忘记密码？</button>

          <div class="fb-divider" aria-hidden="true"></div>

          <button class="fb-btn fb-btn-success fb-create-btn" type="button" :disabled="loading" @click="goRegister">
            创建新账户
          </button>
        </div>

        <div v-if="showQuickFill" class="fb-dev-tools">
          <span class="fb-dev-title">开发调试</span>
          <div class="fb-dev-actions">
            <button class="fb-dev-btn" type="button" :disabled="loading" @click="fill('STUDENT')">填充学生</button>
            <button class="fb-dev-btn" type="button" :disabled="loading" @click="fill('COUNSELOR')">填充辅导员</button>
            <button class="fb-dev-btn" type="button" :disabled="loading" @click="fill('ADMIN')">填充管理员</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import submissionStore from '../stores/submissionStore'
import '../styles/login.css'

const router = useRouter()
const route = useRoute()

const REMEMBER_FLAG_KEY = 'login_remember_account_no'
const SAVED_ACCOUNT_NO_KEY = 'login_saved_account_no'

const showQuickFill = import.meta.env.DEV === true
const savedRememberFlag = localStorage.getItem(REMEMBER_FLAG_KEY) === '1'
const savedAccountNo = savedRememberFlag ? String(localStorage.getItem(SAVED_ACCOUNT_NO_KEY) || '') : ''

const loading = ref(false)
const rememberAccountNo = ref(savedRememberFlag)
const showLoginPassword = ref(false)
const loginPasswordRef = ref(null)

const loginState = reactive({
  success: '',
  info: ''
})

const loginForm = reactive({
  accountNo: savedAccountNo || (showQuickFill ? '2022000001' : ''),
  password: showQuickFill ? '123456' : ''
})

const loginErrors = reactive({
  form: '',
  accountNo: '',
  password: ''
})

const clearLoginMessages = () => {
  loginState.success = ''
  loginState.info = ''
  loginErrors.form = ''
}

const clearLoginFieldError = (field) => {
  loginErrors[field] = ''
  clearLoginMessages()
}

const validateLoginField = (field) => {
  if (field === 'accountNo') {
    const accountNo = String(loginForm.accountNo || '').trim()
    loginErrors.accountNo = accountNo ? '' : '请输入学号/工号'
  }
  if (field === 'password') {
    const password = String(loginForm.password || '')
    loginErrors.password = password ? '' : '请输入密码'
  }
}

const validateLoginForm = () => {
  validateLoginField('accountNo')
  validateLoginField('password')
  return !loginErrors.accountNo && !loginErrors.password
}

const rememberAccountNoIfNeeded = () => {
  if (rememberAccountNo.value && loginForm.accountNo.trim()) {
    localStorage.setItem(REMEMBER_FLAG_KEY, '1')
    localStorage.setItem(SAVED_ACCOUNT_NO_KEY, loginForm.accountNo.trim())
    return
  }
  localStorage.removeItem(REMEMBER_FLAG_KEY)
  localStorage.removeItem(SAVED_ACCOUNT_NO_KEY)
}

const fill = (role) => {
  clearLoginMessages()
  if (role === 'STUDENT') loginForm.accountNo = '2022000001'
  else if (role === 'COUNSELOR') loginForm.accountNo = '9000000002'
  else loginForm.accountNo = '9000000001'
  loginForm.password = '123456'
}

const goRegister = () => {
  router.push('/register')
}

const onForgotPassword = () => {
  clearLoginMessages()
  loginState.info = '忘记密码功能暂未开放，请联系管理员处理。'
}

const login = async () => {
  clearLoginMessages()
  if (!validateLoginForm()) return

  loading.value = true
  try {
    submissionStore.reset()
    const payload = {
      accountNo: String(loginForm.accountNo || '').trim(),
      password: String(loginForm.password || '')
    }
    const { data } = await http.post('/auth/login', payload, { meta: { silent: true } })
    const authData = data.data

    localStorage.setItem('token', authData.token)
    localStorage.setItem('role', authData.role)
    localStorage.setItem('userId', String(authData.userId))
    localStorage.setItem('realName', authData.realName || '')
    rememberAccountNoIfNeeded()

    if (authData.role === 'STUDENT') {
      router.replace('/student')
    } else if (authData.role === 'COUNSELOR') {
      router.replace('/teacher')
    } else {
      router.replace('/admin')
    }
  } catch (error) {
    loginErrors.form = error?.userMessage || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (String(route.query.registered || '') !== '1') return

  const accountNo = String(route.query.accountNo || '').trim()
  loginState.success = '注册成功，请登录。'
  loginState.info = ''
  loginErrors.form = ''
  if (accountNo) loginForm.accountNo = accountNo

  router.replace({ path: '/login', query: {} })
  loginPasswordRef.value?.focus()
})
</script>

