<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">管理员可对已提交且已审核的测评单执行终审。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="keyword"
          width="320px"
          placeholder="搜索学号/姓名/班级"
          :disabled="loadingTasks || isFinalizing"
          @submit="onSearchSubmit"
          @clear="onSearchSubmit"
        />
        <select v-model="statusFilter" style="width: 140px;" :disabled="loadingTasks || isFinalizing">
          <option value="ALL">全部状态</option>
          <option value="COUNSELOR_REVIEWED">已提交管理员</option>
          <option value="FINALIZED">已终审</option>
          <option value="PUBLISHED">已公示</option>
        </select>
      </div>
      <div class="table-search-right">
        <span class="muted">已选 {{ selection.selectedCount.value }} 项</span>
        <button class="btn secondary" @click="batchFinalize" :disabled="!canBatchFinalize">批量终审</button>
        <label>手动终审（输入测评单ID）</label>
        <input v-model.number="manualSubmissionId" type="number" min="1" placeholder="例如 1" style="max-width: 180px;" />
        <button class="btn" @click="finalize(manualSubmissionId)" :disabled="isFinalizing || !manualSubmissionId">立即终审</button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="admin_finalize_tasks">
          <thead>
            <tr>
              <th style="width: 44px;">
                <input
                  ref="headerCheckboxRef"
                  type="checkbox"
                  :checked="allCheckedOnPage"
                  :disabled="!pagedTaskIds.length || loadingTasks || isFinalizing"
                  @change="toggleAllOnPage"
                />
              </th>
              <th>学号</th>
              <th>学生</th>
              <th>班级</th>
              <th>状态</th>
              <th>当前总分</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in pager.pagedRows.value" :key="task.id">
              <td>
                <input
                  type="checkbox"
                  :checked="selection.isSelected(task.id)"
                  :disabled="loadingTasks || isFinalizing"
                  @change="selection.toggle(task.id)"
                />
              </td>
              <td>{{ task.student_no || '-' }}</td>
              <td>{{ task.real_name }}</td>
              <td>{{ task.class_name }}</td>
              <td><span class="badge">{{ statusLabel(task.status) }}</span></td>
              <td>{{ task.total_score ?? '-' }}</td>
              <td>
                <button class="btn" @click="finalize(task.id)" :disabled="isFinalizing">终审该测评单</button>
              </td>
            </tr>
            <tr v-if="!pager.pagedRows.value.length">
              <td colspan="7" class="empty">暂无符合条件的数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="pager.page.value"
        :total-pages="pager.totalPages.value"
        :total="pager.total.value"
        :page-size="pager.pageSize.value"
        :disabled="loadingTasks || isFinalizing"
        @change="onPageChange"
        @page-size-change="onPageSizeChange"
      />
    </div>

    <p v-if="lastResult" class="muted" style="margin-top: 12px;">
      最近一次终审：测评单 #{{ lastResult.id }}，状态 {{ statusLabel(lastResult.status) }}，总分 {{ lastResult.totalScore }}
    </p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import http from '../api/http'
import SearchCapsule from '../components/SearchCapsule.vue'
import TablePager from '../components/TablePager.vue'
import useIdleAutoRefresh from '../composables/useIdleAutoRefresh'
import useTablePager from '../composables/useTablePager'
import useTableSelection from '../composables/useTableSelection'

const tasks = ref([])
const loadingTasks = ref(false)
const isFinalizing = ref(false)
const manualSubmissionId = ref(1)
const lastResult = ref(null)

const keyword = ref('')
const statusFilter = ref('ALL')

const filteredTasks = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  return tasks.value.filter((item) => {
    const source = `${item.student_no || ''} ${item.real_name || ''} ${item.class_name || ''}`.toLowerCase()
    const matchedKeyword = !kw || source.includes(kw)
    const matchedStatus = statusFilter.value === 'ALL' || String(item.status || '').toUpperCase() === statusFilter.value
    return matchedKeyword && matchedStatus
  })
})

const pager = useTablePager(filteredTasks, 10)
const selection = useTableSelection()
const headerCheckboxRef = ref(null)
const pagedTaskIds = computed(() => pager.pagedRows.value.map((item) => item.id))
const allCheckedOnPage = computed(() => selection.isAllCheckedOnPage(pagedTaskIds.value))
const indeterminateOnPage = computed(() => selection.isIndeterminateOnPage(pagedTaskIds.value))
const canBatchFinalize = computed(() => selection.selectedCount.value > 0 && !loadingTasks.value && !isFinalizing.value)

const statusLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const loadTasks = async ({ keepPage = false } = {}) => {
  loadingTasks.value = true
  try {
    const { data } = await http.get('/admin/tasks')
    tasks.value = data.data || []
    selection.clear()
    if (!keepPage) {
      pager.resetPage()
    }
  } finally {
    loadingTasks.value = false
  }
}

const requestFinalize = async (submissionId) => {
  const { data } = await http.post(`/admin/submissions/${submissionId}/finalize`)
  return data.data
}

const finalize = async (submissionId) => {
  if (!submissionId) {
    alert('请先输入测评单ID')
    return
  }

  isFinalizing.value = true
  try {
    lastResult.value = await requestFinalize(submissionId)
    alert(`终审成功：测评单 #${submissionId}`)
    selection.clear()
    await loadTasks({ keepPage: true })
  } finally {
    isFinalizing.value = false
  }
}

const selectedRowsOnPage = computed(() => {
  const selected = new Set(selection.selectedList.value)
  return pager.pagedRows.value.filter((item) => selected.has(String(item.id)))
})

const toggleAllOnPage = () => {
  selection.toggleAll(pagedTaskIds.value)
}

const onPageChange = (nextPage) => {
  pager.goPage(nextPage)
  selection.clear()
}

const onPageSizeChange = (nextSize) => {
  pager.setPageSize(nextSize)
  selection.clear()
}

const batchFinalize = async () => {
  const selectedRows = selectedRowsOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要处理的数据')
    return
  }
  if (!confirm(`确认批量终审已选 ${selectedRows.length} 项吗？`)) return

  isFinalizing.value = true
  try {
    let success = 0
    let failed = 0
    let skipped = 0

    for (const row of selectedRows) {
      const status = String(row.status || '').trim().toUpperCase()
      if (status !== 'COUNSELOR_REVIEWED') {
        skipped += 1
        continue
      }
      try {
        await requestFinalize(row.id)
        success += 1
      } catch (e) {
        failed += 1
      }
    }

    selection.clear()
    await loadTasks({ keepPage: true })
    alert(`批量终审完成：成功 ${success}，失败 ${failed}，跳过 ${skipped}`)
  } finally {
    isFinalizing.value = false
  }
}

const onSearchSubmit = () => {
  selection.clear()
  pager.resetPage()
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = 'ALL'
  selection.clear()
  pager.resetPage()
}

watch([keyword, statusFilter], () => {
  selection.clear()
  pager.resetPage()
})

watch(indeterminateOnPage, (value) => {
  if (!headerCheckboxRef.value) return
  headerCheckboxRef.value.indeterminate = value
})

const busyRef = computed(() => loadingTasks.value || isFinalizing.value)

useIdleAutoRefresh({
  refreshFn: () => loadTasks({ keepPage: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: false
})

onMounted(() => {
  loadTasks()
})
</script>

