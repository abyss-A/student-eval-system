<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">管理员终审</h2>
        <p class="muted" style="margin-top: 6px;">管理员可对已提交且已审核的测评单执行终审。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <button class="search-back-icon" type="button" aria-label="恢复默认筛选" @click="resetFilters">&lt;</button>
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
          <option value="DRAFT">草稿</option>
          <option value="SUBMITTED">已提交</option>
          <option value="FINALIZED">已终审</option>
          <option value="PUBLISHED">已公示</option>
        </select>
      </div>
      <div class="table-search-right">
        <label>手动终审（输入测评单ID）</label>
        <input v-model.number="manualSubmissionId" type="number" min="1" placeholder="例如 1" style="max-width: 180px;" />
        <button class="btn" @click="finalize(manualSubmissionId)" :disabled="isFinalizing || !manualSubmissionId">立即终审</button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky">
          <thead>
            <tr>
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
              <td colspan="6" class="empty">暂无符合条件的数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="pager.page.value"
        :total-pages="pager.totalPages.value"
        :total="pager.total.value"
        :disabled="loadingTasks || isFinalizing"
        @change="pager.goPage"
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

const statusLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const loadTasks = async ({ keepPage = false } = {}) => {
  loadingTasks.value = true
  try {
    const { data } = await http.get('/admin/tasks')
    tasks.value = data.data || []
    if (!keepPage) {
      pager.resetPage()
    }
  } finally {
    loadingTasks.value = false
  }
}

const finalize = async (submissionId) => {
  if (!submissionId) {
    alert('请先输入测评单ID')
    return
  }

  isFinalizing.value = true
  try {
    const { data } = await http.post(`/admin/submissions/${submissionId}/finalize`)
    lastResult.value = data.data
    alert(`终审成功：测评单 #${submissionId}`)
    await loadTasks({ keepPage: true })
  } finally {
    isFinalizing.value = false
  }
}

const onSearchSubmit = () => {
  pager.resetPage()
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = 'ALL'
  pager.resetPage()
}

watch([keyword, statusFilter], () => {
  pager.resetPage()
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
