<template>
  <section class="card scope-page">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">管理员可按班级分配辅导员审核权限。一个辅导员可管理多个班级，同一班级同一时刻仅归属一位辅导员。</p>
      </div>
    </div>

    <div class="scope-body">
      <article class="card scope-left" style="box-shadow: none;">
        <div class="scope-panel-head">
          <h3 class="scope-title">辅导员列表</h3>
          <span class="muted">共 {{ counselorPager.total.value }} 人</span>
        </div>

        <div class="table-search-bar" style="margin-top: 8px;">
          <div class="table-search-left">
            <SearchCapsule
              v-model="counselorKeyword"
              width="280px"
              placeholder="搜索工号/姓名"
              :disabled="loadingCounselors || saving"
              @submit="loadCounselors"
              @clear="loadCounselors"
            />
          </div>
        </div>

        <div class="table-shell" style="margin-top: 10px;">
          <div class="table-scroll-main">
            <table class="table table-sticky" data-resize-key="admin_counselor_scope_counselors">
              <thead>
                <tr>
                  <th style="width: 120px;">工号</th>
                  <th style="width: 120px;">姓名</th>
                  <th style="width: 100px;">已管班级</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in counselorPager.pagedRows.value"
                  :key="item.id"
                  :class="{ 'scope-active-row': Number(item.id) === selectedCounselorId }"
                  @click="selectCounselor(item)"
                >
                  <td>{{ item.accountNo || '-' }}</td>
                  <td>{{ item.realName || '-' }}</td>
                  <td>{{ item.scopeCount ?? 0 }}</td>
                </tr>
                <tr v-if="!counselorPager.pagedRows.value.length">
                  <td colspan="3" class="empty">暂无辅导员</td>
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
            @change="counselorPager.goPage"
            @page-size-change="counselorPager.setPageSize"
          />
        </div>
      </article>

      <article class="card scope-right" style="box-shadow: none;">
        <div v-if="!selectedCounselorId" class="scope-empty">
          请先在左侧选择辅导员。
        </div>

        <template v-else>
          <div class="scope-panel-head">
            <h3 class="scope-title">班级权限配置</h3>
            <span class="muted" v-if="isDirty">已修改，尚未保存</span>
            <span class="muted" v-else>当前配置已保存</span>
          </div>

          <p class="muted" style="margin-top: 6px;">
            辅导员：<b>{{ selectedCounselor?.realName || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            工号：<b>{{ selectedCounselor?.accountNo || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            已选班级：<b>{{ draftClassCount }}</b>
          </p>

          <div class="table-search-bar" style="margin-top: 12px;">
            <div class="table-search-left">
              <SearchCapsule
                v-model="classKeyword"
                width="320px"
                placeholder="搜索班级"
                :disabled="scopeLoading || saving"
              />
            </div>
            <div class="table-search-right">
              <button class="btn secondary" type="button" :disabled="scopeLoading || saving || !filteredClasses.length" @click="selectAllFiltered">全选当前筛选</button>
              <button class="btn secondary" type="button" :disabled="scopeLoading || saving || draftClassCount === 0" @click="clearAll">清空</button>
              <button class="btn secondary" type="button" :disabled="scopeLoading || saving || !isDirty" @click="restore">恢复</button>
              <button class="btn" type="button" :disabled="scopeLoading || saving || !isDirty" @click="saveScopes">{{ saving ? '保存中...' : '保存' }}</button>
            </div>
          </div>

          <div v-if="scopeLoading" class="empty" style="margin-top: 16px;">加载中...</div>
          <div v-else-if="!availableClasses.length" class="empty" style="margin-top: 16px;">暂无可分配班级（当前没有学生班级数据）。</div>
          <div v-else class="scope-class-grid">
            <label
              v-for="className in filteredClasses"
              :key="className"
              class="scope-class-item"
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
        </template>
      </article>
    </div>
  </section>
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
        selectedCounselorId.value = null
        selectedCounselor.value = null
        originalClassSet.value = new Set()
        draftClassSet.value = new Set()
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

const selectCounselor = async (item) => {
  if (!item || !item.id) return
  if (isDirty.value) {
    const ok = window.confirm('当前存在未保存修改，切换辅导员会丢失这些修改，是否继续？')
    if (!ok) return
  }
  selectedCounselorId.value = Number(item.id)
  selectedCounselor.value = item
  classKeyword.value = ''
  await loadScopes(item.id)
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

const restore = () => {
  draftClassSet.value = new Set(originalClassSet.value)
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
.scope-body {
  margin-top: 12px;
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 12px;
}

.scope-left,
.scope-right {
  min-height: 560px;
}

.scope-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.scope-title {
  margin: 0;
  font-size: 16px;
}

.scope-active-row {
  background: #eef5ff;
}

.scope-active-row td {
  border-color: #cfe1ff;
}

.scope-empty {
  min-height: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d1d9e6;
  border-radius: 10px;
  color: #64748b;
  background: #f8fbff;
}

.scope-class-grid {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  max-height: 470px;
  overflow: auto;
}

.scope-class-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  background: #fff;
}

.scope-class-item input {
  width: auto;
  margin: 0;
}

@media (max-width: 1180px) {
  .scope-body {
    grid-template-columns: 1fr;
  }

  .scope-left,
  .scope-right {
    min-height: 0;
  }
}
</style>
