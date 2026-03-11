<template>
  <section class="card semester-page page-shell">
    <p class="muted semester-note">
      学期用于区分每学期的填报、审核与综合排名。新建学期默认不立即生效；切换当前学期前请确保本学期待审核已清零。
    </p>

    <div v-if="noticeText" class="semester-notice" :class="noticeType">
      <span class="semester-notice-label">{{ noticeType === 'error' ? '处理失败' : '操作完成' }}</span>
      <span>{{ noticeText }}</span>
    </div>

    <div class="semester-summary">
      <div class="semester-summary-left">
        <div class="semester-summary-row">
          <span class="semester-summary-label">当前学期</span>
          <span class="semester-summary-value">{{ activeSemester?.name || '未设置' }}</span>
          <el-tag v-if="activeSemester?.name" type="success" effect="light" style="margin-left: 8px;">当前</el-tag>
        </div>
        <div class="semester-summary-row">
          <span class="semester-summary-label">待审核（SUBMITTED）</span>
          <span class="semester-summary-value">{{ submittedPendingCount }}</span>
          <span class="muted" style="margin-left: 8px;">份</span>
        </div>
      </div>
      <div class="semester-summary-actions">
        <el-button type="default" :disabled="loading" @click="load">刷新</el-button>
        <el-button type="primary" :disabled="loading" @click="openCreateDialog">新建学期</el-button>
      </div>
    </div>

    <el-alert
      v-if="Number(submittedPendingCount) > 0"
      type="warning"
      :closable="false"
      show-icon
      title="当前学期仍存在待审核测评单，已禁止切换学期。请先完成本学期审核后再切换。"
      class="semester-warning"
    />

    <div v-if="errorMsg" class="card semester-error-card">
      <el-alert type="error" :closable="false" title="加载失败" />
      <div class="muted semester-error-text">{{ errorMsg }}</div>
      <div class="toolbar-row semester-error-actions">
        <el-button type="default" @click="load" :loading="loading">重试</el-button>
      </div>
    </div>

    <div class="table-shell semester-table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky table-fixed-right semester-table" style="--sticky-action-w: 132px;">
          <thead>
            <tr>
              <th>学期名称</th>
              <th>年份</th>
              <th>季节</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in semesters" :key="item.id">
              <td>
                <span class="semester-name">{{ item.name || '-' }}</span>
              </td>
              <td>{{ item.year ?? '-' }}</td>
              <td>{{ seasonLabel(item.term) }}</td>
              <td>
                <el-tag v-if="Number(item.isActive) === 1" type="success" effect="light">当前</el-tag>
                <el-tag v-else type="info" effect="plain">非当前</el-tag>
              </td>
              <td class="cell-secondary">{{ formatDate(item.createdAt) }}</td>
              <td class="col-action">
                <div class="action-row inline-actions">
                  <el-button
                    size="small"
                    type="default"
                    :loading="actionKey === `activate_${item.id}`"
                    :disabled="loading || Number(item.isActive) === 1 || Number(submittedPendingCount) > 0"
                    @click="activate(item)"
                  >设为当前</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!semesters.length">
              <td colspan="6" class="empty">暂无学期数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <el-dialog v-model="createDialogOpen" width="520px" class="semester-create-dialog" :before-close="handleCreateBeforeClose">
    <template #header>
      <div class="semester-dialog-head">
        <h3 class="semester-dialog-title">新建学期</h3>
        <el-tag type="info" effect="plain">默认不立即生效</el-tag>
      </div>
    </template>

    <div class="semester-form">
      <div class="semester-form-row">
        <span class="semester-form-label">年份</span>
        <el-input-number v-model="createForm.year" :min="2000" :max="2100" :disabled="creating" controls-position="right" />
      </div>

      <div class="semester-form-row">
        <span class="semester-form-label">季节</span>
        <el-radio-group v-model="createForm.season" :disabled="creating">
          <el-radio-button label="SPRING">春季</el-radio-button>
          <el-radio-button label="AUTUMN">秋季</el-radio-button>
        </el-radio-group>
      </div>

      <div class="semester-form-row">
        <span class="semester-form-label">名称</span>
        <el-input
          v-model.trim="createForm.name"
          placeholder="例如：2026年春季学期"
          :disabled="creating"
          @input="onCreateNameInput"
        />
      </div>

      <div v-if="createError" class="semester-form-error">{{ createError }}</div>
    </div>

    <template #footer>
      <div class="semester-dialog-footer">
        <el-button type="default" :disabled="creating" @click="closeCreateDialog">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="creating" @click="createSemester">
          {{ creating ? '创建中...' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import http from '../../api/http'

const loading = ref(false)
const errorMsg = ref('')
const semesters = ref([])
const activeSemester = ref(null)
const submittedPendingCount = ref(0)

const noticeText = ref('')
const noticeType = ref('success')
const actionKey = ref('')

const showNotice = (text, type = 'success') => {
  noticeText.value = text
  noticeType.value = type
}

const clearNotice = () => {
  noticeText.value = ''
  noticeType.value = 'success'
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const seasonLabel = (term) => {
  const value = Number(term)
  if (value === 1) return '春季'
  if (value === 2) return '秋季'
  return '-'
}

const load = async () => {
  loading.value = true
  errorMsg.value = ''
  clearNotice()
  try {
    const { data } = await http.get('/admin/semesters', { meta: { silent: true } })
    const payload = data.data || {}
    semesters.value = payload.semesters || []
    activeSemester.value = payload.activeSemester || null
    submittedPendingCount.value = Number(payload.submittedPendingCount || 0)
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    semesters.value = []
    activeSemester.value = null
    submittedPendingCount.value = 0
  } finally {
    loading.value = false
  }
}

const createDialogOpen = ref(false)
const creating = ref(false)
const createError = ref('')
const createAutoName = ref(true)
const createForm = reactive({
  year: new Date().getFullYear(),
  season: 'SPRING',
  name: ''
})

const defaultName = computed(() => {
  const year = Number(createForm.year) || new Date().getFullYear()
  const seasonText = createForm.season === 'AUTUMN' ? '秋季' : '春季'
  return `${year}年${seasonText}学期`
})

watch(
  () => [createForm.year, createForm.season],
  () => {
    if (createAutoName.value) {
      createForm.name = defaultName.value
    }
  }
)

const onCreateNameInput = () => {
  createAutoName.value = false
}

const openCreateDialog = () => {
  createError.value = ''
  createAutoName.value = true
  createForm.year = new Date().getFullYear()
  createForm.season = 'SPRING'
  createForm.name = defaultName.value
  createDialogOpen.value = true
}

const closeCreateDialog = () => {
  if (creating.value) return
  createDialogOpen.value = false
}

const handleCreateBeforeClose = () => {
  closeCreateDialog()
}

const createSemester = async () => {
  if (creating.value) return
  createError.value = ''
  const payload = {
    year: Number(createForm.year),
    season: createForm.season,
    name: String(createForm.name || '').trim()
  }
  if (!payload.year || payload.year < 2000 || payload.year > 2100) {
    createError.value = '年份不合法'
    return
  }
  if (!payload.name) {
    createError.value = '学期名称不能为空'
    return
  }

  creating.value = true
  try {
    await http.post('/admin/semesters', payload, { meta: { silent: true } })
    showNotice('新学期已创建（未设为当前）', 'success')
    createDialogOpen.value = false
    await load()
  } catch (e) {
    createError.value = e?.userMessage || e?.message || '创建失败'
  } finally {
    creating.value = false
  }
}

const activate = async (item) => {
  if (!item?.id) return
  if (loading.value) return
  if (Number(submittedPendingCount.value) > 0) {
    showNotice(`当前学期仍有${submittedPendingCount.value}份待审核，无法切换`, 'error')
    return
  }

  const name = String(item?.name || '').trim() || '目标学期'
  try {
    await ElMessageBox.confirm(`确定将“${name}”设为当前学期吗？切换后学生将开始该学期填报。`, '确认切换', {
      type: 'warning',
      confirmButtonText: '确认切换',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  actionKey.value = `activate_${item.id}`
  try {
    await http.put(`/admin/semesters/${item.id}/active`, null, { meta: { silent: true } })
    showNotice('已切换当前学期', 'success')
    await load()
  } catch (e) {
    showNotice(e?.userMessage || e?.message || '切换失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.semester-note {
  margin-bottom: 10px;
}

.semester-notice {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 10px;
  border: 1px solid transparent;
}

.semester-notice.success {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1e3a8a;
}

.semester-notice.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #7f1d1d;
}

.semester-notice-label {
  font-weight: 600;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
}

.semester-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  margin-bottom: 10px;
}

.semester-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0;
}

.semester-summary-label {
  color: #64748b;
  font-size: 12px;
  min-width: 120px;
}

.semester-summary-value {
  font-weight: 600;
  color: #0f172a;
}

.semester-summary-actions {
  display: flex;
  gap: 8px;
}

.semester-warning {
  margin-bottom: 10px;
}

.semester-error-card {
  margin-bottom: 10px;
  border-color: #fecaca;
  background: #fef2f2;
  box-shadow: none;
}

.semester-error-text {
  margin-top: 6px;
  white-space: pre-wrap;
}

.semester-error-actions {
  margin-top: 10px;
}

.semester-table-shell {
  margin-top: 0;
}

.semester-name {
  font-weight: 600;
}

.semester-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.semester-dialog-title {
  margin: 0;
  font-size: 16px;
}

.semester-form {
  padding: 6px 0 0;
}

.semester-form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.semester-form-label {
  min-width: 74px;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
}

.semester-form-error {
  margin-top: 8px;
  color: #b91c1c;
  font-size: 12px;
  white-space: pre-wrap;
}

.semester-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>

