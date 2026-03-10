<template>
  <section class="card account-page page-shell">
    <p class="muted account-note">
      账号由管理员统一创建，默认初始密码为 <b>123456</b>。学生和辅导员拿到账号后可在账号中心自行修改密码。
    </p>

    <div v-if="noticeText" class="account-notice" :class="noticeType">
      <span class="account-notice-label">{{ noticeType === 'error' ? '处理失败' : '操作完成' }}</span>
      <span>{{ noticeText }}</span>
    </div>

    <div class="table-search-bar account-toolbar">
      <div class="table-search-left account-filter-row">
        <SearchCapsule
          v-model="keyword"
          width="220px"
          placeholder="搜索学号/工号/姓名/班级"
          :disabled="loading"
          @submit="submitSearch"
          @clear="clearSearch"
        />

        <el-select v-model="filters.role" placeholder="全部角色" style="width: 140px;" :disabled="loading" @change="loadAccounts">
          <el-option label="全部角色" value="" />
          <el-option label="学生" value="STUDENT" />
          <el-option label="辅导员" value="COUNSELOR" />
        </el-select>

        <el-select v-model="filters.enabled" placeholder="全部状态" style="width: 140px;" :disabled="loading" @change="loadAccounts">
          <el-option label="全部状态" value="" />
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </div>

      <div class="table-search-right account-toolbar-actions">
        <el-button type="default" data-testid="account-import-open" :disabled="loading" @click="openImportDialog">
          批量导入
        </el-button>
        <el-button type="primary" data-testid="account-create-open" :disabled="loading" @click="openCreateDialog">
          新建账号
        </el-button>
      </div>
    </div>

    <div class="table-shell account-table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky table-fixed-right account-table" style="--sticky-action-w: 228px; --fixed-action-btn-w: 64px; --fixed-action-gap: 3px; --fixed-action-padding-x: 3px;">
          <thead>
            <tr>
              <th>角色</th>
              <th>学号/工号</th>
              <th>姓名</th>
              <th>性别</th>
              <th>联系电话</th>
              <th>班级</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pager.pagedRows.value" :key="item.id">
              <td>
                <span class="role-chip" :class="String(item.role || '').toLowerCase()">{{ roleLabel(item.role) }}</span>
              </td>
              <td>
                <TableOverflowCell :text="item.accountNo" :cell-key="`account_no_${item.id}`" />
              </td>
              <td>
                <TableOverflowCell :text="item.realName" :cell-key="`account_name_${item.id}`" />
              </td>
              <td>{{ item.gender || '-' }}</td>
              <td>
                <TableOverflowCell :text="item.phone || '-'" :cell-key="`account_phone_${item.id}`" />
              </td>
              <td>
                <TableOverflowCell :text="item.className || '-'" :cell-key="`account_class_${item.id}`" />
              </td>
              <td>
                <span class="status-chip" :class="Number(item.enabled) === 1 ? 'enabled' : 'disabled'">
                  {{ enabledLabel(item.enabled) }}
                </span>
              </td>
              <td class="cell-secondary">{{ formatDate(item.createdAt) }}</td>
              <td class="col-action">
                <div class="action-row inline-actions account-inline-actions">
                  <el-button size="small" type="default" @click="openEditDialog(item)">编辑</el-button>
                  <el-button
                    size="small"
                    type="default"
                    :loading="actionKey === `enabled_${item.id}`"
                    @click="toggleEnabled(item)"
                  >
                    {{ Number(item.enabled) === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRowCommand(command, item)">
                    <el-button size="small" type="default" :title="item.canDelete ? '更多操作' : item.deleteBlockReason || '当前账号不可删除'">
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                        <el-dropdown-item command="delete" :disabled="!item.canDelete">删除账号</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </td>
            </tr>
            <tr v-if="!pager.pagedRows.value.length">
              <td colspan="9" class="empty">暂无账号数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <TablePager
        :page="pager.page.value"
        :total-pages="pager.totalPages.value"
        :total="pager.total.value"
        :page-size="pager.pageSize.value"
        :disabled="loading"
        :show-quick-jumper="false"
        @change="pager.goPage"
        @page-size-change="pager.setPageSize"
      />
    </div>
  </section>

  <el-dialog v-model="editorOpen" width="640px" top="7vh" :close-on-click-modal="false">
    <template #header>
      <div class="account-dialog-head">
        <div>
          <h3>{{ editorMode === 'create' ? '新建账号' : '编辑账号' }}</h3>
          <span class="muted">
            {{ editorMode === 'create' ? '账号创建后，学号/工号与角色不可修改。' : '仅可修改基础资料与班级信息。' }}
          </span>
        </div>
      </div>
    </template>

    <div class="account-form-grid">
      <label class="field">
        <span class="field-label">角色</span>
        <select
          v-model="form.role"
          data-testid="account-form-role"
          class="account-select account-input"
          :disabled="editorMode === 'edit' || savingEditor"
          @change="handleRoleChange"
        >
          <option value="STUDENT">学生</option>
          <option value="COUNSELOR">辅导员</option>
        </select>
      </label>

      <label class="field">
        <span class="field-label">学号/工号</span>
        <input
          v-model.trim="form.accountNo"
          data-testid="account-form-accountNo"
          class="account-input"
          :disabled="editorMode === 'edit' || savingEditor"
          placeholder="请输入学号/工号"
        />
      </label>

      <label class="field">
        <span class="field-label">姓名</span>
        <input
          v-model.trim="form.realName"
          data-testid="account-form-realName"
          class="account-input"
          :disabled="savingEditor"
          placeholder="请输入姓名"
        />
      </label>

      <label class="field">
        <span class="field-label">性别</span>
        <select
          v-model="form.gender"
          data-testid="account-form-gender"
          class="account-select account-input"
          :disabled="savingEditor"
        >
          <option value="男">男</option>
          <option value="女">女</option>
        </select>
      </label>

      <label class="field">
        <span class="field-label">联系电话</span>
        <input
          v-model.trim="form.phone"
          data-testid="account-form-phone"
          class="account-input"
          :disabled="savingEditor"
          placeholder="可留空"
        />
      </label>

      <label v-if="form.role === 'STUDENT'" class="field account-field-full">
        <span class="field-label">班级</span>
        <input
          v-model.trim="form.className"
          data-testid="account-form-className"
          class="account-input"
          :disabled="savingEditor"
          placeholder="请输入班级"
        />
      </label>
    </div>

    <p class="account-password-tip">创建后默认密码为 <b>123456</b>。</p>
    <p v-if="editorError" class="account-error-text">{{ editorError }}</p>

    <template #footer>
      <div class="account-dialog-footer">
        <el-button type="default" :disabled="savingEditor" @click="editorOpen = false">取消</el-button>
        <el-button type="primary" data-testid="account-form-submit" :loading="savingEditor" @click="submitEditor">
          {{ savingEditor ? '提交中...' : editorMode === 'create' ? '创建账号' : '保存修改' }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="importOpen" width="820px" top="6vh" :close-on-click-modal="false">
    <template #header>
      <div class="account-dialog-head">
        <div>
          <h3>批量导入账号</h3>
          <span class="muted">先上传 xlsx 文件做预校验，再确认导入可通过的行。</span>
        </div>
      </div>
    </template>

    <div class="account-import-panel">
      <div class="account-import-topbar">
        <select
          v-model="importRole"
          data-testid="account-import-role"
          class="account-select account-input account-import-role-field"
          :disabled="previewing || committing"
          @change="resetImportPreview"
        >
          <option value="STUDENT">学生</option>
          <option value="COUNSELOR">辅导员</option>
        </select>

        <div class="account-import-actions">
          <el-button type="default" :disabled="previewing || committing" @click="downloadTemplate">下载模板</el-button>
          <button type="button" class="account-file-btn" @click="triggerImportFile">选择文件</button>
          <input
            ref="importFileRef"
            data-testid="account-import-file"
            class="account-file-input"
            type="file"
            accept=".xlsx"
            @change="handleImportFileChange"
          />
          <span class="muted">{{ importFileName || '未选择文件' }}</span>
          <el-button
            type="primary"
            data-testid="account-import-preview"
            :disabled="!importFile || previewing || committing"
            :loading="previewing"
            @click="previewImport"
          >
            {{ previewing ? '预校验中...' : '开始预校验' }}
          </el-button>
        </div>
      </div>

      <p v-if="importError" class="account-error-text">{{ importError }}</p>

      <div v-if="previewResult" class="account-preview-wrap">
        <h4>预校验结果</h4>
        <div class="account-preview-summary">
          <div class="preview-stat">
            <span>总行数</span>
            <strong>{{ previewResult.totalRows }}</strong>
          </div>
          <div class="preview-stat success">
            <span>可导入</span>
            <strong>{{ previewResult.validRows }}</strong>
          </div>
          <div class="preview-stat danger">
            <span>错误</span>
            <strong>{{ previewResult.errorRows }}</strong>
          </div>
        </div>

        <div class="account-preview-grid">
          <section class="account-preview-block">
            <h5>可导入数据</h5>
            <div class="table-scroll-main account-preview-table">
              <table class="table">
                <thead>
                  <tr>
                    <th>行号</th>
                    <th>学号/工号</th>
                    <th>姓名</th>
                    <th>性别</th>
                    <th>联系电话</th>
                    <th v-if="importRole === 'STUDENT'">班级</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in previewResult.validItems || []" :key="`valid_${item.rowNumber}_${item.accountNo}`">
                    <td>{{ item.rowNumber }}</td>
                    <td>{{ item.accountNo || '-' }}</td>
                    <td>{{ item.realName || '-' }}</td>
                    <td>{{ item.gender || '-' }}</td>
                    <td>{{ item.phone || '-' }}</td>
                    <td v-if="importRole === 'STUDENT'">{{ item.className || '-' }}</td>
                  </tr>
                  <tr v-if="!(previewResult.validItems || []).length">
                    <td :colspan="importRole === 'STUDENT' ? 6 : 5" class="empty">当前没有可导入数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="account-preview-block">
            <h5>错误明细</h5>
            <div class="table-scroll-main account-preview-table">
              <table class="table">
                <thead>
                  <tr>
                    <th>行号</th>
                    <th>学号/工号</th>
                    <th>错误原因</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in previewResult.errors || []" :key="`error_${item.rowNumber}_${item.accountNo}`">
                    <td>{{ item.rowNumber }}</td>
                    <td>{{ item.accountNo || '-' }}</td>
                    <td>{{ item.message || '-' }}</td>
                  </tr>
                  <tr v-if="!(previewResult.errors || []).length">
                    <td colspan="3" class="empty">没有错误行</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="account-dialog-footer">
        <el-button type="default" :disabled="previewing || committing" @click="closeImportDialog">关闭</el-button>
        <el-button
          type="primary"
          data-testid="account-import-commit"
          :disabled="!previewResult?.previewToken || Number(previewResult?.validRows || 0) <= 0 || previewing || committing"
          :loading="committing"
          @click="commitImport"
        >
          {{ committing ? '导入中...' : '确认导入' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import SearchCapsule from '../../components/SearchCapsule.vue'
import TablePager from '../../components/TablePager.vue'
import TableOverflowCell from '../../components/TableOverflowCell.vue'
import useTablePager from '../../composables/useTablePager'
import http from '../../api/http'

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const filters = reactive({
  role: '',
  enabled: ''
})

const pager = useTablePager(rows, 10)

const editorOpen = ref(false)
const editorMode = ref('create')
const editingAccountId = ref(null)
const savingEditor = ref(false)
const editorError = ref('')
const form = reactive({
  role: 'STUDENT',
  accountNo: '',
  realName: '',
  gender: '男',
  phone: '',
  className: ''
})

const importOpen = ref(false)
const importRole = ref('COUNSELOR')
const importFileRef = ref(null)
const importFile = ref(null)
const importFileName = ref('')
const importError = ref('')
const previewResult = ref(null)
const previewing = ref(false)
const committing = ref(false)

const actionKey = ref('')
const noticeText = ref('')
const noticeType = ref('success')

const showNotice = (text, type = 'success') => {
  noticeText.value = text
  noticeType.value = type
}

const clearNotice = () => {
  noticeText.value = ''
  noticeType.value = 'success'
}

const roleLabel = (role) => {
  const normalized = String(role || '').toUpperCase()
  if (normalized === 'STUDENT') return '学生'
  if (normalized === 'COUNSELOR') return '辅导员'
  return role || '-'
}

const enabledLabel = (enabled) => (Number(enabled) === 1 ? '启用' : '停用')

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const resetForm = () => {
  editorMode.value = 'create'
  editingAccountId.value = null
  editorError.value = ''
  form.role = 'STUDENT'
  form.accountNo = ''
  form.realName = ''
  form.gender = '男'
  form.phone = ''
  form.className = ''
}

const handleRoleChange = () => {
  if (form.role !== 'STUDENT') {
    form.className = ''
  }
}

const validateEditor = () => {
  const accountNo = String(form.accountNo || '').trim()
  const realName = String(form.realName || '').trim()
  const phone = String(form.phone || '').trim()
  const className = String(form.className || '').trim()
  if (!accountNo) return '请输入学号/工号'
  if (!realName) return '请输入姓名'
  if (!form.gender) return '请选择性别'
  if (phone && !/^[0-9-]{7,20}$/.test(phone)) return '联系电话格式不正确'
  if (form.role === 'STUDENT' && !className) return '请输入班级'
  return ''
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const params = {}
    const trimmedKeyword = String(keyword.value || '').trim()
    if (trimmedKeyword) params.keyword = trimmedKeyword
    if (filters.role) params.role = filters.role
    if (filters.enabled !== '') params.enabled = Number(filters.enabled)
    const { data } = await http.get('/admin/accounts', { params })
    rows.value = data.data || []
  } finally {
    loading.value = false
  }
}

const submitSearch = async () => {
  pager.resetPage()
  await loadAccounts()
}

const clearSearch = async () => {
  keyword.value = ''
  pager.resetPage()
  await loadAccounts()
}

const openCreateDialog = () => {
  resetForm()
  clearNotice()
  editorMode.value = 'create'
  editorOpen.value = true
}

const openEditDialog = (item) => {
  resetForm()
  clearNotice()
  editorMode.value = 'edit'
  editingAccountId.value = item.id
  form.role = String(item.role || 'STUDENT').toUpperCase()
  form.accountNo = item.accountNo || ''
  form.realName = item.realName || ''
  form.gender = item.gender || '男'
  form.phone = item.phone || ''
  form.className = item.className || ''
  editorOpen.value = true
}

const submitEditor = async () => {
  editorError.value = validateEditor()
  if (editorError.value) return

  const payload = {
    role: form.role,
    accountNo: String(form.accountNo || '').trim(),
    realName: String(form.realName || '').trim(),
    gender: String(form.gender || '').trim(),
    phone: String(form.phone || '').trim(),
    className: form.role === 'STUDENT' ? String(form.className || '').trim() : ''
  }

  savingEditor.value = true
  try {
    if (editorMode.value === 'create') {
      await http.post('/admin/accounts', payload)
      showNotice('账号创建成功，初始密码为 123456')
    } else {
      await http.put(`/admin/accounts/${editingAccountId.value}`, payload)
      showNotice('账号信息已更新')
    }
    editorOpen.value = false
    await loadAccounts()
  } catch (error) {
    editorError.value = error?.userMessage || '账号保存失败'
  } finally {
    savingEditor.value = false
  }
}

const confirmAction = async ({ title, message, confirmButtonText = '确认', type = 'warning' }) => {
  try {
    await ElMessageBox.confirm(message, title, {
      type,
      confirmButtonText,
      cancelButtonText: '取消',
      autofocus: false,
      closeOnClickModal: false
    })
    return true
  } catch {
    return false
  }
}

const toggleEnabled = async (item) => {
  const nextEnabled = Number(item.enabled) !== 1
  const confirmed = await confirmAction({
    title: nextEnabled ? '启用账号' : '停用账号',
    message: nextEnabled
      ? `确认启用账号 ${item.accountNo} 吗？`
      : `确认停用账号 ${item.accountNo} 吗？停用后该账号将无法登录。`,
    confirmButtonText: nextEnabled ? '确认启用' : '确认停用'
  })
  if (!confirmed) return

  actionKey.value = `enabled_${item.id}`
  try {
    await http.put(`/admin/accounts/${item.id}/enabled`, { enabled: nextEnabled })
    await loadAccounts()
    showNotice(nextEnabled ? '账号已启用' : '账号已停用')
  } catch (error) {
    showNotice(error?.userMessage || '账号状态更新失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const resetPassword = async (item) => {
  const confirmed = await confirmAction({
    title: '重置密码',
    message: `确认将账号 ${item.accountNo} 的密码重置为 123456 吗？`,
    confirmButtonText: '确认重置'
  })
  if (!confirmed) return

  actionKey.value = `reset_${item.id}`
  try {
    await http.post(`/admin/accounts/${item.id}/reset-password`)
    showNotice(`账号 ${item.accountNo} 的密码已重置为 123456`)
  } catch (error) {
    showNotice(error?.userMessage || '密码重置失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const deleteAccount = async (item) => {
  if (!item.canDelete) return
  const confirmed = await confirmAction({
    title: '删除账号',
    message: `确认删除账号 ${item.accountNo} 吗？该操作不可撤销。`,
    confirmButtonText: '确认删除',
    type: 'error'
  })
  if (!confirmed) return

  actionKey.value = `delete_${item.id}`
  try {
    await http.delete(`/admin/accounts/${item.id}`)
    await loadAccounts()
    showNotice(`账号 ${item.accountNo} 已删除`)
  } catch (error) {
    showNotice(error?.userMessage || '删除账号失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const handleRowCommand = async (command, item) => {
  if (command === 'reset') {
    await resetPassword(item)
    return
  }
  if (command === 'delete') {
    await deleteAccount(item)
  }
}

const openImportDialog = () => {
  clearNotice()
  importOpen.value = true
  resetImportPreview()
}

const closeImportDialog = () => {
  importOpen.value = false
  resetImportPreview()
}

const resetImportPreview = () => {
  previewResult.value = null
  importError.value = ''
  importFile.value = null
  importFileName.value = ''
  if (importFileRef.value) {
    importFileRef.value.value = ''
  }
}

const triggerImportFile = () => {
  importFileRef.value?.click()
}

const handleImportFileChange = (event) => {
  const file = event.target?.files?.[0] || null
  importFile.value = file
  importFileName.value = file?.name || ''
  previewResult.value = null
  importError.value = ''
}

const downloadTemplate = async () => {
  const response = await http.get('/admin/accounts/import/template', {
    params: { role: importRole.value },
    responseType: 'blob'
  })
  const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = importRole.value === 'STUDENT' ? 'student-account-import-template.xlsx' : 'counselor-account-import-template.xlsx'
  anchor.click()
  URL.revokeObjectURL(url)
}

const previewImport = async () => {
  if (!importFile.value) {
    importError.value = '请先选择导入文件'
    return
  }
  previewing.value = true
  importError.value = ''
  previewResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    const { data } = await http.post(`/admin/accounts/import/preview?role=${importRole.value}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      meta: { silent: true }
    })
    previewResult.value = data.data
  } catch (error) {
    importError.value = error?.userMessage || '预校验失败'
  } finally {
    previewing.value = false
  }
}

const commitImport = async () => {
  if (!previewResult.value?.previewToken) return
  committing.value = true
  try {
    const { data } = await http.post('/admin/accounts/import/commit', {
      previewToken: previewResult.value.previewToken
    })
    const result = data.data || {}
    showNotice(`导入完成：成功 ${result.successCount || 0}，失败 ${result.failedCount || 0}`)
    closeImportDialog()
    await loadAccounts()
  } catch (error) {
    importError.value = error?.userMessage || '确认导入失败'
  } finally {
    committing.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.account-note {
  margin: 0;
  line-height: 1.7;
}

.account-notice {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  border: 1px solid transparent;
}

.account-notice-label {
  font-weight: 700;
  white-space: nowrap;
}

.account-notice.success {
  background: #eaf7ef;
  border-color: #c8e9d1;
  color: #1d6b42;
}

.account-notice.error {
  background: #fef0f0;
  border-color: #f6d4d1;
  color: #b42318;
}

.account-toolbar {
  align-items: center;
}

.account-filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.account-toolbar-actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.account-select,
.account-input {
  min-width: 132px;
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid #c7d7f2;
  background: #fff;
  padding: 0 12px;
  color: #1f2a38;
}

.account-select:focus,
.account-input:focus {
  outline: none;
  border-color: #7ea7d6;
  box-shadow: 0 0 0 3px rgba(47, 109, 184, 0.12);
}

.account-table-shell {
  margin-top: 0;
}

.account-table td,
.account-table th {
  vertical-align: middle;
}

.cell-secondary {
  color: #607086;
}

.role-chip,
.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.role-chip.student {
  background: #edf4ff;
  color: #285d98;
}

.role-chip.counselor {
  background: #eef6e4;
  color: #5a712c;
}

.status-chip.enabled {
  background: #e9f7ef;
  color: #1d6b42;
}

.status-chip.disabled {
  background: #f3f4f6;
  color: #5f6b7a;
}

.account-inline-actions {
  gap: 2px;
}

.account-inline-actions :deep(.el-button) {
  width: 62px;
  min-width: 62px;
  max-width: 62px;
  height: 28px;
  min-height: 28px;
  padding: 0 4px;
  border-radius: 8px;
}

.account-inline-actions :deep(.el-dropdown) {
  display: inline-flex;
}

.account-dialog-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.account-dialog-head h3 {
  margin: 0;
  font-size: 18px;
}

.account-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.account-field-full {
  grid-column: 1 / -1;
}

.account-password-tip {
  margin: 14px 0 0;
  color: #355070;
}

.account-error-text {
  margin: 12px 0 0;
  color: #b42318;
}

.account-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.account-import-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.account-import-topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.account-import-role-field {
  width: 140px;
}

.account-import-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.account-file-input {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.account-file-btn {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid #c7d7f2;
  background: #fff;
  color: #355070;
  padding: 0 14px;
  cursor: pointer;
}

.account-file-btn:hover {
  border-color: #9ebee5;
  background: #f8fbff;
}

.account-preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.account-preview-wrap h4,
.account-preview-block h5 {
  margin: 0;
}

.account-preview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.preview-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #eef4ff;
  color: #345c90;
}

.preview-stat strong {
  font-size: 22px;
  line-height: 1;
}

.preview-stat.success {
  background: #e9f7ef;
  color: #1d6b42;
}

.preview-stat.danger {
  background: #fef0f0;
  color: #b42318;
}

.account-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.account-preview-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-preview-table {
  max-height: 300px;
}

@media (max-width: 1024px) {
  .account-toolbar,
  .account-import-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .account-form-grid,
  .account-preview-grid,
  .account-preview-summary {
    grid-template-columns: 1fr;
  }
}
</style>
