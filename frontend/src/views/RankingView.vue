<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">按学期查看综合排名，并支持关键词筛选。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <el-select
          v-model="selectedSemesterId"
          class="ranking-semester-select"
          placeholder="选择学期"
          :disabled="semesterLoading || loading || !semesters.length"
          filterable
          @change="onSemesterChange"
        >
          <el-option
            v-for="s in semesters"
            :key="s.id"
            :label="semesterOptionLabel(s)"
            :value="s.id"
          />
        </el-select>
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

const semesters = ref([])
const selectedSemesterId = ref(null)
const semesterLoading = ref(false)
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

const isActiveSemester = (s) => Number(s?.isActive) === 1
const semesterOptionLabel = (s) => {
  const name = String(s?.name || '').trim() || '-'
  return isActiveSemester(s) ? `${name}（当前）` : name
}

const resolveDefaultSemesterId = (list) => {
  const items = Array.isArray(list) ? list : []
  const active = items.find((s) => isActiveSemester(s))
  return active?.id ?? items[0]?.id ?? null
}

const loadSemesters = async ({ silent = false } = {}) => {
  semesterLoading.value = true
  if (!silent) errorMsg.value = ''
  try {
    const { data } = await http.get('/semesters', { meta: { silent: true } })
    const list = data.data || []
    semesters.value = Array.isArray(list) ? list : []
    const next = resolveDefaultSemesterId(semesters.value)
    if (next != null) {
      selectedSemesterId.value = next
    }
  } catch (e) {
    semesters.value = []
    selectedSemesterId.value = null
    errorMsg.value = e?.userMessage || e?.message || '学期列表加载失败'
  } finally {
    semesterLoading.value = false
  }
}

const load = async ({ keepPage = false, silent = false } = {}) => {
  const semesterId = selectedSemesterId.value
  if (!semesterId) {
    if (!silent) {
      alert('请先选择学期')
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

const onSemesterChange = () => {
  pager.resetPage()
  load({ silent: true })
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
  loadSemesters({ silent: true }).then(() => load({ silent: true }))
})
</script>

<style scoped>
.ranking-semester-select {
  width: 240px;
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


