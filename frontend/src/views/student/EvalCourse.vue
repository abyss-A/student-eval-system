<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
          <span>
            测评单ID：<b>{{ submissionId || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            状态：<span class="badge">{{ statusLabel(status) }}</span>
          </span>
          <span v-if="canEditAny" :class="['save-state-chip', 'inline', autoSave.error.value ? 'error' : '']">
            <template v-if="autoSave.error.value">自动保存失败：{{ autoSave.error.value }}</template>
            <template v-else-if="autoSave.saving.value">自动保存中...</template>
            <template v-else-if="autoSave.dirty.value">有未保存变更</template>
            <template v-else-if="autoSave.lastSavedAt.value">已自动保存 {{ autoSave.lastSavedAt.value }}</template>
            <template v-else>编辑后将自动保存</template>
          </span>
        </p>
      </div>
      <div class="toolbar-row">
        <button class="btn" type="button" @click="save" :disabled="loading || !canEditAny">保存</button>
      </div>
    </div>

    <p v-if="isSubmittedReadOnly" class="muted" style="margin-top: 6px; color: #64748b;">
      审核中，仅可查看，暂不可修改。
    </p>
    <p v-else-if="isRejectedOnlyEditable" class="muted" style="margin-top: 6px; color: #64748b;">
      仅可修改被驳回条目，修改后请再次提交。
    </p>
    <p v-else-if="isFullyReadOnly" class="muted" style="margin-top: 6px; color: #64748b;">
      当前测评单状态不可编辑，如需修改请联系管理员。
    </p>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="keyword"
          width="300px"
          placeholder="搜索课程名称"
          @submit="onSearchSubmit"
          @clear="onSearchSubmit"
        />
        <select v-model="typeFilter" style="width: 140px;">
          <option value="ALL">全部类型</option>
          <option value="REQUIRED">必修</option>
          <option value="ELECTIVE">选修</option>
          <option value="RETAKE">重修</option>
          <option value="RELEARN">再修</option>
        </select>
        <select v-model="reviewFilter" style="width: 140px;">
          <option value="ALL">全部审核</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">通过</option>
          <option value="REJECTED">驳回</option>
        </select>
      </div>
      <div class="table-search-right">
        <span class="muted">已选 {{ selection.selectedCount.value }} 项</span>
        <button class="btn secondary" type="button" :disabled="!canBatchDeleteCourses" @click="batchDeleteCourses">批量删除</button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="student_eval_courses">
          <thead>
            <tr>
              <th style="width: 44px;">
                <input
                  ref="headerCheckboxRef"
                  type="checkbox"
                  :checked="allCheckedOnPage"
                  :disabled="!pagedCourseKeys.length || loading || !canRemoveRows"
                  @change="toggleAllOnPage"
                />
              </th>
              <th style="width: 200px;">课程名称</th>
              <th style="width: 110px;">类型</th>
              <th style="width: 110px;">分数</th>
              <th style="width: 90px;">学分</th>
              <th style="width: 110px;">审核结论</th>
              <th>审核理由</th>
              <th style="width: 90px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(c, idx) in pagedCourses" :key="c.id || c._rowKey || idx">
              <td>
                <input
                  type="checkbox"
                  :checked="selection.isSelected(courseSelectionKey(c))"
                  :disabled="loading || !canRemoveRows"
                  @change="selection.toggle(courseSelectionKey(c))"
                />
              </td>
              <td>
                <input
                  v-model.trim="c.courseName"
                  placeholder="例如：高等代数"
                  :disabled="loading || !canEditCourseRow(c)"
                  @blur="triggerImmediateSave(c)"
                />
              </td>
              <td>
                <select v-model="c.courseType" :disabled="loading || !canEditCourseRow(c)" @blur="triggerImmediateSave(c)">
                  <option value="REQUIRED">必修</option>
                  <option value="ELECTIVE">选修</option>
                  <option value="RETAKE">重修</option>
                  <option value="RELEARN">再修</option>
                </select>
              </td>
              <td>
                <input
                  v-model.number="c.score"
                  type="number"
                  min="0"
                  max="100"
                  step="0.5"
                  placeholder="0-100"
                  :disabled="loading || !canEditCourseRow(c)"
                  @blur="triggerImmediateSave(c)"
                />
              </td>
              <td>
                <input
                  v-model.number="c.credit"
                  type="number"
                  min="0"
                  step="0.5"
                  placeholder="例如：3"
                  :disabled="loading || !canEditCourseRow(c)"
                  @blur="triggerImmediateSave(c)"
                />
              </td>
              <td>
                <span class="badge" :class="reviewResultBadge(courseReviewResult(c))">
                  {{ courseReviewResult(c) }}
                </span>
              </td>
              <td>
                <div style="white-space: pre-wrap;">{{ displayReviewerComment(c.reviewerComment) }}</div>
              </td>
              <td>
                <button
                  class="btn secondary"
                  type="button"
                  @click="removeCourse(c)"
                  :disabled="loading || !canRemoveRows"
                >
                  删除
                </button>
              </td>
            </tr>
            <tr v-if="!pagedCourses.length">
              <td colspan="8" class="empty">暂无符合条件的课程</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="page"
        :total-pages="totalPages"
        :total="total"
        :page-size="pageSize"
        :disabled="loading"
        @change="onPageChange"
        @page-size-change="onPageSizeChange"
      />
    </div>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn secondary" type="button" @click="addRow" :disabled="loading || !isDraftEditable">新增</button>
      <p class="muted">提示：保存时会自动忽略“完全空白”的行。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import submissionStore from '../../stores/submissionStore'
import TablePager from '../../components/TablePager.vue'
import SearchCapsule from '../../components/SearchCapsule.vue'
import useTablePager from '../../composables/useTablePager'
import useAutoSaveDraft from '../../composables/useAutoSaveDraft'
import useTableSelection from '../../composables/useTableSelection'

const store = submissionStore
const courses = ref([])
const loading = ref(false)
const suppressDirty = ref(false)

const keyword = ref('')
const typeFilter = ref('ALL')
const reviewFilter = ref('ALL')

const submissionId = computed(() => store.state.submissionId)
const status = computed(() => store.state.status || store.state.detail?.submission?.status || '')
const editableRejectedOnly = computed(() => Boolean(store.state.score?.editableRejectedOnly))
const reviewReady = computed(() => Boolean(store.state.score?.reviewReady))
const statusCode = computed(() => String(status.value || '').trim().toUpperCase())
const isDraftEditable = computed(() => statusCode.value === 'DRAFT')
const isRejectedOnlyEditable = computed(() => statusCode.value === 'SUBMITTED' && editableRejectedOnly.value)
const isSubmittedReadOnly = computed(() => statusCode.value === 'SUBMITTED' && !editableRejectedOnly.value)
const isFullyReadOnly = computed(() => (
  statusCode.value === 'COUNSELOR_REVIEWED'
  || statusCode.value === 'FINALIZED'
  || statusCode.value === 'PUBLISHED'
))
const canEditAny = computed(() => isDraftEditable.value || isRejectedOnlyEditable.value)
const canRemoveRows = computed(() => isDraftEditable.value)

const canEditCourseRow = (row) => {
  if (isDraftEditable.value) return true
  if (!isRejectedOnlyEditable.value) return false
  const reviewStatus = String(row?.reviewStatus || '').trim().toUpperCase()
  return reviewStatus === 'REJECTED'
}

const isSubmitStage = computed(() => {
  const code = String(status.value || '').trim().toUpperCase()
  return code === 'SUBMITTED'
})

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const courseReviewResult = (c) => {
  if (!reviewReady.value) return '-'
  const statusCode = String(c?.reviewStatus || '').trim().toUpperCase()
  if (statusCode === 'REJECTED') return '驳回'
  if (statusCode === 'APPROVED') return '通过'
  return '待审核'
}

const reviewResultBadge = (label) => {
  if (label === '通过') return 'success'
  if (label === '驳回') return 'danger'
  return ''
}

const isAutoReason = (text) => {
  const value = String(text || '').trim().toUpperCase()
  return value === '辅导员APPROVE' || value === '辅导员REJECT'
}

const displayReviewerComment = (text) => {
  if (!reviewReady.value) return '-'
  if (!text || isAutoReason(text)) return '-'
  return text
}

const isSportCourseEntry = (courseName, courseType) => {
  const name = String(courseName || '').trim().toLowerCase().replace(/\s+/g, '')
  const type = String(courseType || '').trim().toLowerCase()
  if (!name && !type) return false
  if (type.includes('体育') || type.includes('sport') || type.includes('physical') || type === 'pe') return true
  return (
    name.includes('体育')
    || name.includes('体测')
    || name.includes('体能')
    || name.includes('sport')
    || name.includes('physical')
  )
}

const mapCourse = (c) => ({
  id: c?.id,
  courseName: c?.courseName || '',
  courseType: (c?.courseType || 'REQUIRED').toUpperCase(),
  score: c?.score ?? 0,
  credit: c?.credit ?? 0,
  evidenceFileId: c?.evidenceFileId ?? null,
  reviewStatus: c?.reviewStatus || 'PENDING',
  reviewerComment: c?.reviewerComment || '',
  _rowKey: c?.id ? `course_${c.id}` : `course_tmp_${Date.now()}_${Math.random()}`
})

const filteredCourses = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  return courses.value.filter((item) => {
    const matchedKeyword = !kw || String(item.courseName || '').toLowerCase().includes(kw)
    const matchedType = typeFilter.value === 'ALL' || String(item.courseType || '').toUpperCase() === typeFilter.value
    const code = String(item.reviewStatus || '').toUpperCase()
    const matchedReview = reviewFilter.value === 'ALL' || code === reviewFilter.value
    return matchedKeyword && matchedType && matchedReview
  })
})

const {
  page,
  pageSize,
  total,
  totalPages,
  pagedRows: pagedCourses,
  goPage,
  setPageSize,
  resetPage
} = useTablePager(filteredCourses, 10)

const selection = useTableSelection()
const headerCheckboxRef = ref(null)

const courseSelectionKey = (course) => {
  if (!course) return ''
  if (course.id != null) return `id_${course.id}`
  return String(course._rowKey || '')
}

const pagedCourseKeys = computed(() => pagedCourses.value.map((item) => courseSelectionKey(item)).filter(Boolean))
const allCheckedOnPage = computed(() => selection.isAllCheckedOnPage(pagedCourseKeys.value))
const indeterminateOnPage = computed(() => selection.isIndeterminateOnPage(pagedCourseKeys.value))
const canBatchDeleteCourses = computed(() => selection.selectedCount.value > 0 && canRemoveRows.value && !loading.value)

const ensureEditable = () => {
  if (canEditAny.value) return true
  if (isSubmitStage.value) {
    alert('当前审核阶段不可修改课程')
    return false
  }
  alert('当前测评单状态不可编辑')
  return false
}

const ensureDraftEditable = () => {
  if (isDraftEditable.value) return true
  if (isSubmitStage.value) {
    alert('审核中不可新增或删除课程')
    return false
  }
  alert('当前测评单状态不可编辑')
  return false
}

const ensureAtLeastOneRow = () => {
  if (!courses.value.length) {
    courses.value.push(mapCourse({}))
  }
}

const buildCoursePayload = ({ silent }) => {
  const out = []
  for (const c of courses.value) {
    const name = String(c.courseName || '').trim()
    const type = String(c.courseType || 'REQUIRED').trim()
    const score = Number(c.score)
    const credit = Number(c.credit)

    const isBlankRow = !name && (!Number.isFinite(score) || score === 0) && (!Number.isFinite(credit) || credit === 0)
    if (isBlankRow) continue

    if (!name) {
      const msg = '课程名称不能为空'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (!type) {
      const msg = '课程类型不能为空'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (isSportCourseEntry(name, type)) {
      const msg = '体育课成绩请到体育页面填写，且活动标题必须为“大学体育”'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (!Number.isFinite(score) || score < 0 || score > 100) {
      const msg = '课程分数必须在 0 到 100 之间'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (!Number.isFinite(credit) || credit < 0) {
      const msg = '课程学分必须是大于等于 0 的数字'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }

    out.push({
      id: c.id,
      courseName: name,
      courseType: type,
      score,
      credit,
      evidenceFileId: c.evidenceFileId || null
    })
  }
  return { ok: true, message: '', items: out }
}

const syncCoursesFromStore = () => {
  suppressDirty.value = true
  courses.value = (store.state.detail?.courses || []).map(mapCourse)
  ensureAtLeastOneRow()
  selection.clear()
  suppressDirty.value = false
}

const autoSave = useAutoSaveDraft(async () => {
  if (!canEditAny.value) return
  const payload = buildCoursePayload({ silent: true })
  if (!payload.ok) {
    throw new Error(payload.message || '当前填写内容未完成，暂未自动保存')
  }
  await store.saveCourses(payload.items, { syncAfterSave: false, silent: true })
}, { debounceMs: 1200 })

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    await store.loadScore()
    syncCoursesFromStore()
    resetPage()
    autoSave.resetState()
  } finally {
    loading.value = false
  }
}

const addRow = () => {
  if (!ensureDraftEditable()) return
  courses.value.push(mapCourse({}))
  selection.clear()
  resetPage()
}

const removeRow = (idx) => {
  if (!ensureDraftEditable()) return
  courses.value.splice(idx, 1)
  ensureAtLeastOneRow()
  selection.clear()
  resetPage()
}

const removeCourse = (course) => {
  const idx = courses.value.findIndex((c) => c === course || (course.id && c.id === course.id))
  if (idx >= 0) removeRow(idx)
}

const clearFilters = () => {
  keyword.value = ''
  typeFilter.value = 'ALL'
  reviewFilter.value = 'ALL'
  selection.clear()
  resetPage()
}

const onSearchSubmit = () => {
  selection.clear()
  resetPage()
}

const onPageChange = (nextPage) => {
  goPage(nextPage)
  selection.clear()
}

const onPageSizeChange = (nextSize) => {
  setPageSize(nextSize)
  selection.clear()
}

const toggleAllOnPage = () => {
  selection.toggleAll(pagedCourseKeys.value)
}

const selectedCoursesOnPage = computed(() => {
  const selected = new Set(selection.selectedList.value)
  return pagedCourses.value.filter((item) => selected.has(courseSelectionKey(item)))
})

const batchDeleteCourses = () => {
  if (!ensureDraftEditable()) return

  const selectedRows = selectedCoursesOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要删除的课程')
    return
  }
  if (!confirm(`确认批量删除已选 ${selectedRows.length} 条课程吗？`)) return

  const keys = new Set(selectedRows.map((item) => courseSelectionKey(item)))
  courses.value = courses.value.filter((item) => !keys.has(courseSelectionKey(item)))
  ensureAtLeastOneRow()
  selection.clear()
  resetPage()
  alert(`已删除 ${selectedRows.length} 条课程`)
}

const save = async () => {
  if (!ensureEditable()) return
  const payload = buildCoursePayload({ silent: false })
  if (!payload.ok) return

  loading.value = true
  try {
    await store.saveCourses(payload.items, { syncAfterSave: true })
    syncCoursesFromStore()
    autoSave.resetState()
    alert('课程已保存')
  } finally {
    loading.value = false
  }
}

const triggerImmediateSave = (row) => {
  if (!canEditAny.value || suppressDirty.value || !autoSave.dirty.value) return
  if (!canEditCourseRow(row)) return
  autoSave.saveNow().catch(() => {})
}

watch([keyword, typeFilter, reviewFilter], () => {
  selection.clear()
  resetPage()
})

watch(indeterminateOnPage, (value) => {
  if (!headerCheckboxRef.value) return
  headerCheckboxRef.value.indeterminate = value
})

watch(
  courses,
  () => {
    if (suppressDirty.value || loading.value || !canEditAny.value) return
    autoSave.markDirty()
  },
  { deep: true }
)

store.registerAutoSaveFlusher('courses', autoSave.flushBeforeSubmit)

onMounted(reload)

onBeforeUnmount(() => {
  if (!canEditAny.value) return
  autoSave.saveNow().catch(() => {})
})
</script>


