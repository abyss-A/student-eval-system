<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">按学期查看综合排名，并支持关键词筛选。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <el-input
          v-model.trim="semesterIdInput"
          type="number"
          class="ranking-semester-input"
          placeholder="学期ID"
          @keydown.enter.prevent="load()"
        />
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <SearchCapsule
          v-model="keyword"
          width="180px"
          placeholder="搜索学号/姓名/班级"
          :disabled="loading"
          @submit="onKeywordSearch"
          @clear="onKeywordSearch"
        />
      </div>
    </div>

    <div v-if="errorMsg" class="card ranking-error-card">
      <el-alert type="error" :closable="false" title="加载失败" />
      <div class="muted ranking-error-text">{{ errorMsg }}</div>
      <div class="toolbar-row ranking-error-actions">
        <el-button type="default" @click="load" :loading="loading">重试</el-button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="ranking_main">
          <thead>
            <tr>
              <th>学号</th>
              <th>学生</th>
              <th>班级</th>
              <th>总分</th>
              <th>班级排名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in pager.pagedRows.value" :key="r.id || `${r.account_no || r.accountNo || '-'}_${r.real_name || ''}`">
              <td>{{ r.account_no || r.accountNo || '-' }}</td>
              <td>{{ r.real_name }}</td>
              <td>
                <TableOverflowCell
                  :text="r.class_name"
                  :cell-key="`ranking_class_${r.id || r.account_no || r.accountNo || ''}`"
                />
              </td>
              <td>{{ r.total_score }}</td>
              <td>{{ r.rankClass }}</td>
            </tr>
            <tr v-if="!pager.pagedRows.value.length">
              <td colspan="5" class="empty">暂无排名数据</td>
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
        @change="pager.goPage"
        @page-size-change="pager.setPageSize"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import http from '../api/http'
import SearchCapsule from '../components/SearchCapsule.vue'
import TableOverflowCell from '../components/TableOverflowCell.vue'
import TablePager from '../components/TablePager.vue'
import useIdleAutoRefresh from '../composables/useIdleAutoRefresh'
import useTablePager from '../composables/useTablePager'

const semesterIdInput = ref('1')
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const errorMsg = ref('')

const filteredRows = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter((item) => {
    const source = `${item.account_no || item.accountNo || ''} ${item.real_name || ''} ${item.class_name || ''}`.toLowerCase()
    return source.includes(kw)
  })
})

const pager = useTablePager(filteredRows, 10)

const parseSemesterId = () => {
  const raw = String(semesterIdInput.value || '').trim()
  const parsed = Number.parseInt(raw, 10)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return parsed
}

const load = async ({ keepPage = false, silent = false } = {}) => {
  const semesterId = parseSemesterId()
  if (!semesterId) {
    if (!silent) {
      alert('请先输入有效的学期ID')
    }
    return
  }

  loading.value = true
  errorMsg.value = ''
  try {
    const { data } = await http.get('/rankings', { params: { semesterId }, meta: { silent: true } })
    rows.value = data.data || []
    if (!keepPage) {
      pager.resetPage()
    }
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    rows.value = []
    if (!keepPage) {
      pager.resetPage()
    }
  } finally {
    loading.value = false
  }
}

const onKeywordSearch = () => {
  pager.resetPage()
}

const resetKeyword = () => {
  keyword.value = ''
  pager.resetPage()
}

watch(keyword, () => {
  pager.resetPage()
})

const busyRef = computed(() => loading.value)

useIdleAutoRefresh({
  refreshFn: () => load({ keepPage: true, silent: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: false
})

onMounted(() => {
  load({ silent: true })
})
</script>

<style scoped>
.ranking-semester-input {
  width: 120px;
}

.ranking-error-card {
  margin-top: 12px;
  border-color: #fecaca;
  background: #fef2f2;
  box-shadow: none;
}

.ranking-error-text {
  margin-top: 6px;
  white-space: pre-wrap;
}

.ranking-error-actions {
  margin-top: 10px;
}
</style>


