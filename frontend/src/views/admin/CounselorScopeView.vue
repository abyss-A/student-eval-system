<template>
  <section class="card scope-page page-shell">
    <div class="scope-head">
      <div>
        <p class="muted">管理员可按班级分配辅导员审核权限。一个辅导员可管理多个班级，同一班级同一时刻仅归属一位辅导员。</p>
      </div>
      <el-tag type="info" effect="plain">学院工作台</el-tag>
    </div>

    <div class="scope-main">
      <div class="table-search-bar">
        <div class="table-search-left">
          <SearchCapsule
            v-model="counselorKeyword"
            width="180px"
            placeholder="搜索工号/姓名"
            :disabled="loadingCounselors || saving"
            @submit="loadCounselors"
            @clear="loadCounselors"
          />
        </div>
      </div>

      <div class="table-shell scope-table-shell">
        <div class="table-scroll-main">
          <table class="table table-sticky table-fixed-right" style="--sticky-action-w: 104px;">
            <thead>
              <tr>
                <th>工号</th>
                <th>姓名</th>
                <th>已管班级</th>
                <th class="scope-op-col col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in counselorPager.pagedRows.value"
                :key="item.id"
                :class="{ 'scope-active-row': Number(item.id) === selectedCounselorId && scopeDialogOpen }"
                @click="selectCounselor(item)"
              >
                <td>{{ item.accountNo || '-' }}</td>
                <td>{{ item.realName || '-' }}</td>
                <td>{{ item.scopeCount ?? 0 }}</td>
                <td class="col-action">
                  <el-button
                    type="default"
                    size="small"
                    :disabled="scopeLoading || saving"
                    @click.stop="openScopeDialog(item)"
                  >配置班级</el-button>
                </td>
              </tr>
              <tr v-if="!counselorPager.pagedRows.value.length">
                <td colspan="4" class="empty">暂无辅导员</td>
              </tr>
            </tbody>
          </table>
        </div>

        <TablePager
          :page="counselorPager.page.value"
          :total-pages="counselorPager.totalPages.value"
          :total="counselorPager.total.value"
          :page-size="counselorPager.pageSize.value"
          :disabled="loadingCounselors || saving"
          :show-quick-jumper="false"
          @change="counselorPager.goPage"
          @page-size-change="counselorPager.setPageSize"
        />
      </div>
    </div>
  </section>

  <el-dialog
    v-model="scopeDialogOpen"
    width="960px"
    top="6vh"
    class="scope-dialog"
    :before-close="handleDialogBeforeClose"
  >
    <template #header>
      <div class="scope-dialog-head">
        <h3 class="scope-title">班级权限配置</h3>
        <el-tag :type="isDirty ? 'warning' : 'success'" effect="light">
          {{ isDirty ? '已修改，尚未保存' : '当前配置已保存' }}
        </el-tag>
      </div>
    </template>

    <p class="muted scope-user-meta">
      辅导员：<b>{{ selectedCounselor?.realName || '-' }}</b>
      <span class="scope-sep">|</span>
      工号：<b>{{ selectedCounselor?.accountNo || '-' }}</b>
      <span class="scope-sep">|</span>
      已选班级：<b>{{ draftClassCount }}</b>
    </p>

    <div class="table-search-bar scope-toolbar-top">
      <div class="table-search-left">
        <SearchCapsule
          v-model="classKeyword"
          width="180px"
          placeholder="搜索班级"
          :disabled="scopeLoading || saving"
        />
      </div>
      <div class="table-search-right">
        <el-button type="default" :disabled="scopeLoading || saving || !filteredClasses.length" @click="selectAllFiltered">全选当前筛选</el-button>
        <el-button type="default" :disabled="scopeLoading || saving || draftClassCount === 0" @click="clearAll">清空</el-button>
        <el-button type="default" :disabled="scopeLoading || saving || !isDirty" @click="restore">恢复</el-button>
      </div>
    </div>

    <div v-if="scopeLoading" class="empty scope-state">加载中...</div>
    <div v-else-if="!availableClasses.length" class="empty scope-state">暂无可分配班级（当前没有学生班级数据）。</div>
    <div v-else class="scope-class-grid">
      <label
        v-for="className in filteredClasses"
        :key="className"
        class="scope-class-item"
        :class="{ checked: isChecked(className) }"
      >
        <input
          type="checkbox"
          :checked="isChecked(className)"
          :disabled="saving"
          @change="toggleClass(className)"
        />
        <span>{{ className }}</span>
      </label>
      <div v-if="!filteredClasses.length" class="empty">没有匹配的班级</div>
    </div>

    <template #footer>
      <div class="scope-dialog-footer">
        <el-button type="default" :disabled="saving" @click="closeScopeDialog">关闭</el-button>
        <el-button type="primary" :disabled="scopeLoading || saving || !isDirty" @click="saveScopes">
          {{ saving ? '保存中...' : '保存' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import http from '../../api/http'
import SearchCapsule from '../../components/SearchCapsule.vue'
import TablePager from '../../components/TablePager.vue'
import useTablePager from '../../composables/useTablePager'

const counselors = ref([])
const availableClasses = ref([])

const counselorKeyword = ref('')
const classKeyword = ref('')

const loadingCounselors = ref(false)
const scopeLoading = ref(false)
const saving = ref(false)
const scopeDialogOpen = ref(false)

const selectedCounselorId = ref(null)
const selectedCounselor = ref(null)
const originalClassSet = ref(new Set())
const draftClassSet = ref(new Set())

const counselorPager = useTablePager(counselors, 10)

const normalizeClassName = (raw) => String(raw || '').trim()

const filteredClasses = computed(() => {
  const keyword = String(classKeyword.value || '').trim().toLowerCase()
  const source = Array.isArray(availableClasses.value) ? availableClasses.value : []
  if (!keyword) return source
  return source.filter((name) => String(name || '').toLowerCase().includes(keyword))
})

const draftClassCount = computed(() => draftClassSet.value.size)

const isDirty = computed(() => {
  const original = originalClassSet.value
  const draft = draftClassSet.value
  if (original.size !== draft.size) return true
  for (const item of draft) {
    if (!original.has(item)) return true
  }
  return false
})

const isChecked = (className) => draftClassSet.value.has(className)

const resetSelectedState = () => {
  selectedCounselorId.value = null
  selectedCounselor.value = null
  originalClassSet.value = new Set()
  draftClassSet.value = new Set()
}

const loadCounselors = async () => {
  loadingCounselors.value = true
  try {
    const keyword = String(counselorKeyword.value || '').trim()
    const params = keyword ? { keyword } : {}
    const { data } = await http.get('/admin/counselors', { params, meta: { silent: true } })
    counselors.value = (data.data || []).map((item) => ({
      id: Number(item.id),
      accountNo: item.accountNo || '-',
      realName: item.realName || '-',
      scopeCount: Number(item.scopeCount || 0)
    }))
    counselorPager.resetPage()

    if (selectedCounselorId.value != null) {
      const hit = counselors.value.find((item) => Number(item.id) === Number(selectedCounselorId.value))
      if (hit) {
        selectedCounselor.value = hit
      } else {
        scopeDialogOpen.value = false
        classKeyword.value = ''
        resetSelectedState()
      }
    }
  } finally {
    loadingCounselors.value = false
  }
}

const loadClasses = async () => {
  const { data } = await http.get('/admin/classes', { meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  availableClasses.value = rows
    .map((item) => normalizeClassName(item?.className || item?.class_name))
    .filter(Boolean)
}

const loadScopes = async (counselorId) => {
  scopeLoading.value = true
  try {
    const { data } = await http.get(`/admin/counselors/${counselorId}/scopes`, { meta: { silent: true } })
    const payload = data.data || {}
    const classes = Array.isArray(payload.classNames) ? payload.classNames : []
    const normalized = classes.map((item) => normalizeClassName(item)).filter(Boolean)
    originalClassSet.value = new Set(normalized)
    draftClassSet.value = new Set(normalized)
  } finally {
    scopeLoading.value = false
  }
}

const restore = () => {
  draftClassSet.value = new Set(originalClassSet.value)
}

const closeScopeDialog = (force = false) => {
  if (!force && isDirty.value) {
    const ok = window.confirm('当前存在未保存修改，关闭后将丢失这些修改，是否继续？')
    if (!ok) return
    restore()
  }
  classKeyword.value = ''
  scopeDialogOpen.value = false
}

const handleDialogBeforeClose = (done) => {
  if (isDirty.value) {
    const ok = window.confirm('当前存在未保存修改，关闭后将丢失这些修改，是否继续？')
    if (!ok) return
    restore()
  }
  classKeyword.value = ''
  done()
}

const openScopeDialog = async (item) => {
  if (!item || !item.id) return

  if (scopeDialogOpen.value && isDirty.value && Number(item.id) !== Number(selectedCounselorId.value)) {
    const ok = window.confirm('当前存在未保存修改，切换辅导员会丢失这些修改，是否继续？')
    if (!ok) return
    restore()
  }

  selectedCounselorId.value = Number(item.id)
  selectedCounselor.value = item
  classKeyword.value = ''

  try {
    await loadScopes(item.id)
    scopeDialogOpen.value = true
  } catch (e) {
    const msg = e?.userMessage || e?.message || '加载辅导员班级权限失败'
    alert(msg)
  }
}

const selectCounselor = async (item) => {
  await openScopeDialog(item)
}

const toggleClass = (className) => {
  const next = new Set(draftClassSet.value)
  if (next.has(className)) next.delete(className)
  else next.add(className)
  draftClassSet.value = next
}

const selectAllFiltered = () => {
  const next = new Set(draftClassSet.value)
  filteredClasses.value.forEach((name) => next.add(name))
  draftClassSet.value = next
}

const clearAll = () => {
  draftClassSet.value = new Set()
}

const saveScopes = async () => {
  if (!selectedCounselorId.value || !isDirty.value) return

  const original = originalClassSet.value
  const draft = draftClassSet.value
  let added = 0
  let removed = 0

  for (const item of draft) {
    if (!original.has(item)) added += 1
  }
  for (const item of original) {
    if (!draft.has(item)) removed += 1
  }

  const ok = window.confirm(`确认保存班级权限变更？新增 ${added} 个，移除 ${removed} 个。`)
  if (!ok) return

  saving.value = true
  try {
    const classNames = Array.from(draft).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
    await http.put(`/admin/counselors/${selectedCounselorId.value}/scopes`, { classNames })
    originalClassSet.value = new Set(classNames)
    draftClassSet.value = new Set(classNames)

    const hit = counselors.value.find((item) => Number(item.id) === Number(selectedCounselorId.value))
    if (hit) {
      hit.scopeCount = classNames.length
      selectedCounselor.value = hit
    }

    await loadCounselors()
    alert('班级权限已保存')
    scopeDialogOpen.value = false
    classKeyword.value = ''
  } catch (e) {
    const msg = e?.userMessage || e?.message || '保存失败'
    alert(msg)
  } finally {
    saving.value = false
  }
}

watch(
  () => counselorKeyword.value,
  () => {
    counselorPager.resetPage()
  }
)

const initialize = async () => {
  await Promise.all([loadCounselors(), loadClasses()])
}

initialize()
</script>

<style scoped>
.page-shell {
  padding: 16px;
}

.scope-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.scope-main {
  min-height: 0;
}

.scope-title {
  margin: 0;
  font-size: 18px;
}

.scope-toolbar-top {
  margin-top: 10px;
}

.scope-table-shell {
  margin-top: 12px;
}

.scope-active-row {
  background: #ebf3ff;
}

.scope-active-row td {
  border-color: #c8dcf9;
}

.scope-op-col {
  width: 120px;
}

.scope-user-meta {
  margin: 4px 0 0;
  line-height: 1.7;
}

.scope-sep {
  margin: 0 8px;
  color: #a2b2c8;
}

.scope-state {
  margin-top: 14px;
}

.scope-class-grid {
  margin-top: 10px;
  border: 1px solid #dde7f5;
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  max-height: min(52vh, 520px);
  overflow: auto;
  background: #f9fbff;
}

.scope-class-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbe5f4;
  border-radius: 10px;
  padding: 9px 12px;
  background: #fff;
  color: #21354f;
  transition: all 0.12s ease;
}

.scope-class-item.checked {
  border-color: #96b8e4;
  background: #edf5ff;
}

.scope-class-item input {
  width: auto;
  margin: 0;
  accent-color: #2d6bb7;
}

.scope-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.scope-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.scope-table-shell :deep(.table-pager-wrap) {
  overflow-x: hidden !important;
}

.scope-table-shell :deep(.table-pager-wrap::-webkit-scrollbar) {
  height: 0 !important;
}

</style>
