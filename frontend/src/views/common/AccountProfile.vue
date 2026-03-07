<template>
  <section class="card stack">
    <div>
      <p class="muted" style="margin-top: 6px;">查看个人资料，修改联系电话与登录密码。</p>
    </div>

    <article class="card" style="box-shadow: none; background: #fbfdff;">
      <h3 style="margin: 0 0 10px; font-size: 16px;">基本信息</h3>
      <div class="grid two">
        <div class="field"><span class="field-label">{{ accountNoLabel }}</span><el-input :model-value="profile.accountNo || '-'" disabled /></div>
        <div class="field"><span class="field-label">姓名</span><el-input :model-value="profile.realName || '-'" disabled /></div>
        <div class="field"><span class="field-label">角色</span><el-input :model-value="roleText" disabled /></div>
        <div class="field"><span class="field-label">性别</span><el-input :model-value="profile.gender || '-'" disabled /></div>
        <div class="field"><span class="field-label">班级</span><el-input :model-value="profile.className || '-'" disabled /></div>
      </div>
    </article>

    <article class="card" style="box-shadow: none; background: #fbfdff;">
      <h3 style="margin: 0 0 10px; font-size: 16px;">联系电话</h3>
      <div class="account-phone-row">
        <label class="field account-phone-field">
          <span class="field-label">电话</span>
          <el-input
            v-model.trim="phoneDraft"
            :disabled="savingPhone"
            placeholder="请输入联系电话"
            @input="phoneError = ''"
          />
        </label>
        <div class="toolbar-row account-phone-actions">
          <el-button type="primary" :loading="savingPhone" @click="savePhone">
            {{ savingPhone ? '保存中...' : '保存电话' }}
          </el-button>
        </div>
      </div>
      <span class="muted account-inline-error">{{ phoneError }}</span>
    </article>

    <article class="card" style="box-shadow: none; background: #fbfdff;">
      <h3 style="margin: 0 0 10px; font-size: 16px;">修改密码</h3>
      <div class="grid two">
        <label class="field">
          <span class="field-label">旧密码</span>
          <el-input v-model="pwdForm.oldPassword" type="password" :show-password="true" :disabled="changingPwd" @input="pwdError = ''" />
        </label>
        <label class="field">
          <span class="field-label">新密码</span>
          <el-input v-model="pwdForm.newPassword" type="password" :show-password="true" :disabled="changingPwd" @input="pwdError = ''" />
        </label>
        <label class="field">
          <span class="field-label">确认新密码</span>
          <el-input v-model="pwdForm.confirmPassword" type="password" :show-password="true" :disabled="changingPwd" @input="pwdError = ''" />
        </label>
      </div>
      <p class="muted" style="color: #b42318; min-height: 20px; margin-top: 8px;">{{ pwdError }}</p>
      <div class="toolbar-row" style="margin-top: 8px;">
        <el-button type="primary" :loading="changingPwd" @click="changePassword">
          {{ changingPwd ? '提交中...' : '修改密码' }}
        </el-button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../api/http'
import { getRole, roleLabel } from '../../utils/auth'

const phonePattern = /^[0-9-]{7,20}$/

const role = ref(getRole())
const roleText = computed(() => roleLabel(role.value))
const accountNoLabel = computed(() => (role.value === 'STUDENT' ? '学号' : '工号'))

const profile = reactive({
  accountNo: '',
  realName: '',
  gender: '',
  phone: '',
  className: ''
})

const phoneDraft = ref('')
const phoneError = ref('')
const savingPhone = ref(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdError = ref('')
const changingPwd = ref(false)

const applyProfile = (data) => {
  profile.accountNo = data?.accountNo || ''
  profile.realName = data?.realName || ''
  profile.gender = data?.gender || ''
  profile.phone = data?.phone || ''
  profile.className = data?.className || ''
  phoneDraft.value = profile.phone
}

const loadProfile = async () => {
  const { data } = await http.get('/me/profile')
  applyProfile(data.data || {})
}

const savePhone = async () => {
  const phone = String(phoneDraft.value || '').trim()
  if (!phone) {
    phoneError.value = '联系电话不能为空'
    return
  }
  if (!phonePattern.test(phone)) {
    phoneError.value = '联系电话格式不正确'
    return
  }

  savingPhone.value = true
  phoneError.value = ''
  try {
    const { data } = await http.put('/me/profile', { phone })
    applyProfile(data.data || {})
    alert('联系电话已更新')
  } catch (e) {
    phoneError.value = e?.userMessage || e?.message || '联系电话更新失败'
  } finally {
    savingPhone.value = false
  }
}

const changePassword = async () => {
  const oldPassword = String(pwdForm.oldPassword || '')
  const newPassword = String(pwdForm.newPassword || '')
  const confirmPassword = String(pwdForm.confirmPassword || '')

  if (!oldPassword) {
    pwdError.value = '请输入旧密码'
    return
  }
  if (newPassword.length < 6 || newPassword.length > 32) {
    pwdError.value = '新密码长度需在6-32位之间'
    return
  }
  if (newPassword !== confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  if (oldPassword === newPassword) {
    pwdError.value = '新密码不能与旧密码相同'
    return
  }

  changingPwd.value = true
  pwdError.value = ''
  try {
    await http.post('/me/password', {
      oldPassword,
      newPassword,
      confirmPassword
    })
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    alert('密码修改成功，请使用新密码登录')
  } catch (e) {
    pwdError.value = e?.userMessage || e?.message || '密码修改失败'
  } finally {
    changingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.account-phone-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.account-phone-actions {
  justify-content: flex-start;
}

.account-inline-error {
  display: block;
  color: #b42318;
  min-height: 20px;
  margin-top: 6px;
}

@media (max-width: 768px) {
  .account-phone-row {
    grid-template-columns: 1fr;
  }
}
</style>

