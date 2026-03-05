<template>
  <div class="reg-page">
    <div class="reg-shell">
      <button class="reg-back-btn" type="button" @click="goBack">
        <span class="reg-back-icon-wrap" aria-hidden="true">
          <svg class="reg-back-icon" viewBox="0 0 24 24" fill="none">
            <path d="M20 12H7.5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
            <path d="M12 7L7 12L12 17" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <span>返回登录</span>
      </button>

      <section class="reg-card">
        <header class="reg-header">
          <h1 class="reg-title">创建新账户</h1>
        </header>

        <p v-if="state.formError" class="reg-banner error">{{ state.formError }}</p>

        <form class="stack" @submit.prevent="submitRegister" novalidate>
          <div class="reg-grid">
            <label class="field">
              <span class="field-label">姓名</span>
              <input
                v-model.trim="form.realName"
                :class="{ 'is-error': Boolean(errors.realName) }"
                placeholder="请输入姓名"
                :aria-invalid="Boolean(errors.realName)"
                aria-describedby="reg-realname-error"
                @blur="validateField('realName')"
                @input="clearFieldError('realName')"
              />
              <span id="reg-realname-error" class="reg-field-error">{{ errors.realName }}</span>
            </label>

            <label class="field">
              <span class="field-label">密码</span>
              <div class="reg-password-wrap">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  :class="{ 'is-error': Boolean(errors.password) }"
                  placeholder="至少6位，最多32位"
                  autocomplete="new-password"
                  :aria-invalid="Boolean(errors.password)"
                  aria-describedby="reg-password-error"
                  @blur="validateField('password')"
                  @input="clearFieldError('password')"
                />
                <button
                  class="reg-toggle-btn"
                  type="button"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <span id="reg-password-error" class="reg-field-error">{{ errors.password }}</span>
            </label>

            <label class="field">
              <span class="field-label">确认密码</span>
              <div class="reg-password-wrap">
                <input
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  :class="{ 'is-error': Boolean(errors.confirmPassword) }"
                  placeholder="请再次输入密码"
                  autocomplete="new-password"
                  :aria-invalid="Boolean(errors.confirmPassword)"
                  aria-describedby="reg-confirm-error"
                  @blur="validateField('confirmPassword')"
                  @input="clearFieldError('confirmPassword')"
                />
                <button
                  class="reg-toggle-btn"
                  type="button"
                  :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                  @click="showConfirmPassword = !showConfirmPassword"
                >
                  {{ showConfirmPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <span id="reg-confirm-error" class="reg-field-error">{{ errors.confirmPassword }}</span>
            </label>

            <label class="field">
              <span class="field-label">学号/工号</span>
              <input
                v-model.trim="form.accountNo"
                :class="{ 'is-error': Boolean(errors.accountNo) }"
                placeholder="请输入学号/工号"
                :aria-invalid="Boolean(errors.accountNo)"
                aria-describedby="reg-accountno-error"
                @blur="validateField('accountNo')"
                @input="clearFieldError('accountNo')"
              />
              <span id="reg-accountno-error" class="reg-field-error">{{ errors.accountNo }}</span>
            </label>

            <label class="field">
              <span class="field-label">年级班级</span>
              <input
                v-model.trim="form.gradeClass"
                :class="{ 'is-error': Boolean(errors.gradeClass) }"
                placeholder="例如：2023级1班"
                :aria-invalid="Boolean(errors.gradeClass)"
                aria-describedby="reg-gradeclass-error"
                @blur="validateField('gradeClass')"
                @input="clearFieldError('gradeClass')"
              />
              <span id="reg-gradeclass-error" class="reg-field-error">{{ errors.gradeClass }}</span>
            </label>

            <label class="field">
              <span class="field-label">联系电话</span>
              <input
                v-model.trim="form.phone"
                :class="{ 'is-error': Boolean(errors.phone) }"
                placeholder="仅数字或连字符"
                :aria-invalid="Boolean(errors.phone)"
                aria-describedby="reg-phone-error"
                @blur="validateField('phone')"
                @input="clearFieldError('phone')"
              />
              <span id="reg-phone-error" class="reg-field-error">{{ errors.phone }}</span>
            </label>

            <label class="field">
              <span class="field-label">专业（选填）</span>
              <input
                v-model.trim="form.majorName"
                :class="{ 'is-error': Boolean(errors.majorName) }"
                placeholder="例如：数学与应用数学"
                :aria-invalid="Boolean(errors.majorName)"
                aria-describedby="reg-major-error"
                @blur="validateField('majorName')"
                @input="clearFieldError('majorName')"
              />
              <span id="reg-major-error" class="reg-field-error">{{ errors.majorName }}</span>
            </label>

            <fieldset class="reg-gender-field">
              <legend class="field-label">性别</legend>
              <div class="reg-gender-grid">
                <label class="reg-gender-card">
                  <span>男</span>
                  <input v-model="form.gender" type="radio" value="男" @change="clearFieldError('gender')" />
                </label>
                <label class="reg-gender-card">
                  <span>女</span>
                  <input v-model="form.gender" type="radio" value="女" @change="clearFieldError('gender')" />
                </label>
              </div>
              <span class="reg-field-error">{{ errors.gender }}</span>
            </fieldset>
          </div>

          <p class="reg-note">点击注册即表示你同意平台使用规则。</p>

          <button class="reg-submit-btn" type="submit" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import '../styles/register.css'

const router = useRouter()

const phonePattern = /^[0-9-]{7,20}$/

const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const form = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  gender: '男',
  accountNo: '',
  gradeClass: '',
  phone: '',
  majorName: ''
})

const errors = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  gender: '',
  accountNo: '',
  gradeClass: '',
  phone: '',
  majorName: ''
})

const state = reactive({
  formError: ''
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/login')
}

const clearFormError = () => {
  state.formError = ''
}

const clearFieldError = (field) => {
  errors[field] = ''
  clearFormError()
}

const validateField = (field) => {
  const password = String(form.password || '')
  const confirmPassword = String(form.confirmPassword || '')
  const realName = String(form.realName || '').trim()
  const gender = String(form.gender || '').trim()
  const accountNo = String(form.accountNo || '').trim()
  const gradeClass = String(form.gradeClass || '').trim()
  const phone = String(form.phone || '').trim()
  const majorName = String(form.majorName || '').trim()

  if (field === 'password') {
    if (!password) errors.password = '请输入密码'
    else if (password.length < 6 || password.length > 32) errors.password = '密码长度需在6-32位之间'
    else errors.password = ''
    if (confirmPassword) validateField('confirmPassword')
    return
  }

  if (field === 'confirmPassword') {
    if (!confirmPassword) errors.confirmPassword = '请再次输入密码'
    else if (confirmPassword !== password) errors.confirmPassword = '两次输入的密码不一致'
    else errors.confirmPassword = ''
    return
  }

  if (field === 'realName') {
    if (!realName) errors.realName = '请输入姓名'
    else if (realName.length > 32) errors.realName = '姓名长度不能超过32位'
    else errors.realName = ''
    return
  }

  if (field === 'gender') {
    errors.gender = gender ? '' : '请选择性别'
    return
  }

  if (field === 'accountNo') {
    if (!accountNo) errors.accountNo = '请输入学号/工号'
    else if (accountNo.length > 32) errors.accountNo = '学号/工号长度不能超过32位'
    else errors.accountNo = ''
    return
  }

  if (field === 'gradeClass') {
    if (!gradeClass) errors.gradeClass = '请输入年级班级'
    else if (gradeClass.length > 64) errors.gradeClass = '年级班级长度不能超过64位'
    else errors.gradeClass = ''
    return
  }

  if (field === 'phone') {
    if (!phone) errors.phone = '请输入联系电话'
    else if (!phonePattern.test(phone)) errors.phone = '联系电话格式不正确'
    else errors.phone = ''
    return
  }

  if (field === 'majorName') {
    if (majorName && majorName.length > 64) errors.majorName = '专业长度不能超过64位'
    else errors.majorName = ''
  }
}

const validateForm = () => {
  validateField('realName')
  validateField('password')
  validateField('confirmPassword')
  validateField('accountNo')
  validateField('gradeClass')
  validateField('phone')
  validateField('majorName')
  validateField('gender')

  return (
    !errors.realName &&
    !errors.password &&
    !errors.confirmPassword &&
    !errors.accountNo &&
    !errors.gradeClass &&
    !errors.phone &&
    !errors.majorName &&
    !errors.gender
  )
}

const submitRegister = async () => {
  clearFormError()
  if (!validateForm()) return

  const payload = {
    password: String(form.password || ''),
    realName: String(form.realName || '').trim(),
    gender: String(form.gender || '').trim(),
    accountNo: String(form.accountNo || '').trim(),
    gradeClass: String(form.gradeClass || '').trim(),
    phone: String(form.phone || '').trim(),
    majorName: String(form.majorName || '').trim()
  }

  loading.value = true
  try {
    await http.post('/auth/register/student', payload, { meta: { silent: true } })
    router.replace({
      path: '/login',
      query: {
        registered: '1',
        accountNo: payload.accountNo
      }
    })
  } catch (error) {
    state.formError = error?.userMessage || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
