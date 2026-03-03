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
    <p v-if="isSportModule" class="muted" style="margin-top: 8px; color: #334155;">
      请填写大学体育成绩，若没有则不填；仅标题为“大学体育”按体育课成绩计算，其余体育活动按 15% 计算。
    </p>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="keyword"
          width="320px"
          placeholder="搜索活动标题或说明"
          @submit="onSearchSubmit"
          @clear="onSearchSubmit"
        />
        <select v-model="reviewFilter" style="width: 140px;">
          <option value="ALL">全部审核</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">通过</option>
          <option value="REJECTED">驳回</option>
        </select>
      </div>
      <div class="table-search-right">
        <span class="muted">已选 {{ selection.selectedCount.value }} 项</span>
        <button class="btn secondary" type="button" :disabled="!canBatchDeleteRows" @click="batchDeleteRows">批量删除</button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="student_eval_module">
          <thead>
            <tr>
              <th style="width: 44px;">
                <input
                  ref="headerCheckboxRef"
                  type="checkbox"
                  :checked="allCheckedOnPage"
                  :disabled="!pagedRowKeys.length || loading || !canRemoveRows"
                  @change="toggleAllOnPage"
                />
              </th>
              <th style="width: 180px;">活动标题</th>
              <th>说明</th>
              <th style="width: 110px;">分数</th>
              <th style="width: 320px;">证明材料</th>
              <th style="width: 110px;">审核结论</th>
              <th style="width: 200px;">审核理由</th>
              <th style="width: 90px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(a, idx) in pagedRows" :key="a.id || a._rowKey || idx">
              <td>
                <input
                  type="checkbox"
                  :checked="selection.isSelected(rowSelectionKey(a))"
                  :disabled="loading || !canRemoveRows"
                  @change="selection.toggle(rowSelectionKey(a))"
                />
              </td>
              <td>
                <input
                  v-model.trim="a.title"
                  placeholder="例如：主题团日"
                  :disabled="loading || !canEditActivityRow(a)"
                  @blur="triggerImmediateSave(a)"
                />
              </td>
              <td>
                <input
                  v-model.trim="a.description"
                  placeholder="可填写简要说明"
                  :disabled="loading || !canEditActivityRow(a)"
                  @blur="triggerImmediateSave(a)"
                />
              </td>
              <td>
                <input
                  v-model.number="a.selfScore"
                  type="number"
                  min="0"
                  :max="isSportModule ? 100 : null"
                  step="0.5"
                  :disabled="loading || !canEditActivityRow(a)"
                  @blur="triggerImmediateSave(a)"
                />
              </td>
              <td>
                <ImageIdsUploader
                  v-model="a.evidenceFileIds"
                  :max="6"
                  :hint="''"
                  :readonly="!canEditActivityRow(a)"
                />
              </td>
              <td>
                <span class="badge" :class="reviewResultBadge(activityReviewResult(a))">
                  {{ activityReviewResult(a) }}
                </span>
              </td>
              <td>
                <div style="white-space: pre-wrap;">{{ displayReviewerComment(a.reviewerComment) }}</div>
              </td>
              <td>
                <button
                  class="btn secondary"
                  type="button"
                  @click="removeItem(a)"
                  :disabled="loading || !canRemoveRows"
                >
                  删除
                </button>
              </td>
            </tr>
            <tr v-if="!pagedRows.length">
              <td colspan="8" class="empty">暂无符合条件的活动</td>
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

    <div class="toolbar-row module-footer-row" style="margin-top: 10px;">
      <button class="btn secondary" type="button" @click="addRow" :disabled="loading || !isDraftEditable">新增</button>
      <div class="formula-note">
        {{ moduleFormulaText }}
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import submissionStore from '../../stores/submissionStore'
import ImageIdsUploader from '../../components/ImageIdsUploader.vue'
import TablePager from '../../components/TablePager.vue'
import SearchCapsule from '../../components/SearchCapsule.vue'
import useTablePager from '../../composables/useTablePager'
import useAutoSaveDraft from '../../composables/useAutoSaveDraft'
import useTableSelection from '../../composables/useTableSelection'

const props = defineProps({
  moduleType: { type: String, required: true },
  label: { type: String, required: true }
})

const store = submissionStore
const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const reviewFilter = ref('ALL')
const suppressDirty = ref(false)

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
const isSportModule = computed(() => props.moduleType === 'SPORT_ACTIVITY')
const UNIVERSITY_PE_TITLE = '大学体育'

const canEditActivityRow = (row) => {
  if (isDraftEditable.value) return true
  if (!isRejectedOnlyEditable.value) return false
  const reviewStatus = String(row?.reviewStatus || '').trim().toUpperCase()
  return reviewStatus === 'REJECTED'
}

const isSubmitStage = computed(() => {
  const code = String(status.value || '').trim().toUpperCase()
  return code === 'SUBMITTED'
})

const moduleFormulaText = computed(() => {
  if (props.moduleType === 'MORAL') {
    return '德育计入分公式：德育计入分 = min(德育活动总分, 100) × 15%'
  }
  if (props.moduleType === 'INTEL_PRO_INNOV') {
    return '智育计入分公式：智育原始分 = 智育课程加权平均分 × 85% + min(智育活动总分, 100) × 15%；智育计入分 = 智育原始分 × 60%'
  }
  if (props.moduleType === 'SPORT_ACTIVITY') {
    return '体育计入分公式：体育原始分 = 大学体育分 × 85% + min(体育活动总分, 100) × 15%；体育计入分 = 体育原始分 × 10%'
  }
  if (props.moduleType === 'ART') {
    return '美育计入分公式：美育计入分 = min(美育活动总分, 100) × 7.5%'
  }
  if (props.moduleType === 'LABOR') {
    return '劳育计入分公式：劳育计入分 = min(劳育活动总分, 100) × 7.5%'
  }
  return ''
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

const activityReviewResult = (a) => {
  if (!reviewReady.value) return '-'
  const statusCode = String(a?.reviewStatus || '').trim().toUpperCase()
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

const isUniversityPeTitle = (title) => String(title || '').trim() === UNIVERSITY_PE_TITLE

const blankRow = () => ({
  moduleType: props.moduleType,
  title: '',
  description: '',
  selfScore: 0,
  reviewStatus: 'PENDING',
  reviewerComment: '',
  evidenceFileIds: '',
  _rowKey: `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const mapActivity = (a) => ({
  id: a?.id,
  moduleType: props.moduleType,
  title: a?.title || '',
  description: a?.description || '',
  selfScore: Number(a?.selfScore ?? 0),
  reviewStatus: a?.reviewStatus || 'PENDING',
  reviewerComment: a?.reviewerComment || '',
  evidenceFileIds: a?.evidenceFileIds || '',
  _rowKey: a?.id ? `act_${a.id}` : `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const filteredRows = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  return rows.value.filter((item) => {
    const source = `${item.title || ''} ${item.description || ''}`.toLowerCase()
    const matchedKeyword = !kw || source.includes(kw)
    const matchedReview = reviewFilter.value === 'ALL' || String(item.reviewStatus || '').toUpperCase() === reviewFilter.value
    return matchedKeyword && matchedReview
  })
})

const {
  page,
  pageSize,
  total,
  totalPages,
  pagedRows,
  goPage,
  setPageSize,
  resetPage
} = useTablePager(filteredRows, 10)

const selection = useTableSelection()
const headerCheckboxRef = ref(null)

const rowSelectionKey = (row) => {
  if (!row) return ''
  if (row.id != null) return `id_${row.id}`
  return String(row._rowKey || '')
}

const pagedRowKeys = computed(() => pagedRows.value.map((item) => rowSelectionKey(item)).filter(Boolean))
const allCheckedOnPage = computed(() => selection.isAllCheckedOnPage(pagedRowKeys.value))
const indeterminateOnPage = computed(() => selection.isIndeterminateOnPage(pagedRowKeys.value))
const canBatchDeleteRows = computed(() => selection.selectedCount.value > 0 && canRemoveRows.value && !loading.value)

const ensureEditable = () => {
  if (canEditAny.value) return true
  if (isSubmitStage.value) {
    alert('当前审核阶段不可修改活动')
    return false
  }
  alert('当前测评单状态不可编辑')
  return false
}

const ensureDraftEditable = () => {
  if (isDraftEditable.value) return true
  if (isSubmitStage.value) {
    alert('审核中不可新增或删除活动')
    return false
  }
  alert('当前测评单状态不可编辑')
  return false
}

const buildActivityPayload = ({ silent }) => {
  const out = []
  let universityPeCount = 0
  for (const a of rows.value) {
    const title = String(a.title || '').trim()
    const desc = String(a.description || '').trim()
    const score = Number(a.selfScore)
    const ev = String(a.evidenceFileIds || '').trim()

    const isBlank = !title && !desc && (!Number.isFinite(score) || score === 0) && !ev
    if (isBlank) continue

    if (!title) {
      const msg = '活动标题不能为空'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (!Number.isFinite(score) || score < 0 || (isSportModule.value && score > 100)) {
      const msg = isSportModule.value
        ? '体育模块分数必须在 0 到 100 之间'
        : '活动分数必须是大于等于 0 的数字'
      if (!silent) alert(msg)
      return { ok: false, message: msg, items: [] }
    }
    if (isSportModule.value && isUniversityPeTitle(title)) {
      universityPeCount += 1
    }

    out.push({
      id: a.id,
      moduleType: props.moduleType,
      title,
      description: desc,
      selfScore: score,
      evidenceFileIds: ev
    })
  }
  if (isSportModule.value && universityPeCount > 1) {
    const msg = '体育模块中“大学体育”最多只能填写一条'
    if (!silent) alert(msg)
    return { ok: false, message: msg, items: [] }
  }
  return { ok: true, message: '', items: out }
}

const autoSave = useAutoSaveDraft(async () => {
  if (!canEditAny.value) return
  const payload = buildActivityPayload({ silent: true })
  if (!payload.ok) {
    throw new Error(payload.message || '当前填写内容未完成，暂未自动保存')
  }
  await store.saveActivitiesModule(props.moduleType, payload.items, { syncAfterSave: false, silent: true })
}, { debounceMs: 1200 })

const syncRowsFromStore = () => {
  suppressDirty.value = true
  const all = store.state.detail?.activities || []
  rows.value = all
    .filter((x) => String(x.moduleType || '').toUpperCase() === props.moduleType)
    .map(mapActivity)
  if (!rows.value.length) rows.value.push(blankRow())
  selection.clear()
  suppressDirty.value = false
}

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    await store.loadScore()
    syncRowsFromStore()
    resetPage()
    autoSave.resetState()
  } finally {
    loading.value = false
  }
}

const addRow = () => {
  if (!ensureDraftEditable()) return
  rows.value.push(blankRow())
  selection.clear()
  resetPage()
}

const removeRow = (idx) => {
  if (!ensureDraftEditable()) return
  rows.value.splice(idx, 1)
  if (!rows.value.length) rows.value.push(blankRow())
  selection.clear()
  resetPage()
}

const removeItem = (item) => {
  const idx = rows.value.findIndex((x) => x === item || (item.id && x.id === item.id))
  if (idx >= 0) removeRow(idx)
}

const clearFilters = () => {
  keyword.value = ''
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
  selection.toggleAll(pagedRowKeys.value)
}

const selectedRowsOnPage = computed(() => {
  const selected = new Set(selection.selectedList.value)
  return pagedRows.value.filter((item) => selected.has(rowSelectionKey(item)))
})

const batchDeleteRows = () => {
  if (!ensureDraftEditable()) return

  const selectedRows = selectedRowsOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要删除的活动')
    return
  }
  if (!confirm(`确认批量删除已选 ${selectedRows.length} 条活动吗？`)) return

  const keys = new Set(selectedRows.map((item) => rowSelectionKey(item)))
  rows.value = rows.value.filter((item) => !keys.has(rowSelectionKey(item)))
  if (!rows.value.length) rows.value.push(blankRow())
  selection.clear()
  resetPage()
  alert(`已删除 ${selectedRows.length} 条活动`)
}

const save = async () => {
  if (!ensureEditable()) return
  const payload = buildActivityPayload({ silent: false })
  if (!payload.ok) return

  loading.value = true
  try {
    await store.saveActivitiesModule(props.moduleType, payload.items, { syncAfterSave: true })
    syncRowsFromStore()
    autoSave.resetState()
    alert('本模块活动已保存')
  } finally {
    loading.value = false
  }
}

const triggerImmediateSave = (row) => {
  if (!canEditAny.value || suppressDirty.value || !autoSave.dirty.value) return
  if (!canEditActivityRow(row)) return
  autoSave.saveNow().catch(() => {})
}

watch(
  () => props.moduleType,
  () => {
    store.registerAutoSaveFlusher(`module_${props.moduleType}`, autoSave.flushBeforeSubmit)
    reload()
  },
  { immediate: true }
)

watch([keyword, reviewFilter], () => {
  selection.clear()
  resetPage()
})

watch(
  rows,
  () => {
    if (suppressDirty.value || loading.value || !canEditAny.value) return
    autoSave.markDirty()
  },
  { deep: true }
)

watch(indeterminateOnPage, (value) => {
  if (!headerCheckboxRef.value) return
  headerCheckboxRef.value.indeterminate = value
})

onBeforeUnmount(() => {
  if (!canEditAny.value) return
  autoSave.saveNow().catch(() => {})
})
</script>

<style scoped>
.formula-note {
  font-size: 13px;
  color: #475569;
}

.module-footer-row {
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>


