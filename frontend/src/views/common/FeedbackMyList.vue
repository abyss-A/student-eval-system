<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">我的反馈</h2>
        <p class="muted" style="margin-top: 6px;">查看我提交的反馈与处理结果。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <button class="search-back-icon" type="button" aria-label="恢复默认筛选" @click="resetFilters">&lt;</button>
        <SearchCapsule
          v-model="keyword"
          width="320px"
          placeholder="搜索反馈标题"
          :disabled="loading"
          @submit="onSearchSubmit"
          @clear="onSearchSubmit"
        />
        <select v-model="activeStatus" style="width: 140px;" :disabled="loading" @change="onStatusChange">
          <option value="ALL">全部状态</option>
          <option value="NEW">待处理</option>
          <option value="REPLIED">已回复</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </div>
    </div>

    <div
      v-if="errorMsg"
      class="card"
      style="margin-top: 12px; border-color: #fecaca; background: #fef2f2; box-shadow: none;"
    >
      <div style="font-weight: 700; color: #b42318;">加载失败</div>
      <div class="muted" style="margin-top: 6px; white-space: pre-wrap;">{{ errorMsg }}</div>
      <div class="toolbar-row" style="margin-top: 10px;">
        <button class="btn secondary" type="button" @click="load()" :disabled="loading">重试</button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="feedback_my_list">
          <thead>
            <tr>
              <th>标题</th>
              <th style="width: 110px;">状态</th>
              <th style="width: 170px;">提交时间</th>
              <th style="width: 170px;">更新时间</th>
              <th style="width: 120px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in pager.pagedRows.value" :key="f.id">
              <td style="font-weight: 700; color: #0f172a;">{{ f.title }}</td>
              <td>
                <span class="badge" :class="statusBadge(f.status)">{{ statusLabel(f.status) }}</span>
              </td>
              <td>{{ formatDate(f.created_at) }}</td>
              <td>{{ formatDate(f.updated_at) }}</td>
              <td>
                <button class="btn ghost" type="button" @click="goDetail(f.id)">查看</button>
              </td>
            </tr>
            <tr v-if="!pager.pagedRows.value.length && !errorMsg">
              <td colspan="5" class="empty">暂无反馈</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="pager.page.value"
        :total-pages="pager.totalPages.value"
        :total="pager.total.value"
        :disabled="loading"
        @change="pager.goPage"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import SearchCapsule from '../../components/SearchCapsule.vue'
import TablePager from '../../components/TablePager.vue'
import useIdleAutoRefresh from '../../composables/useIdleAutoRefresh'
import useTablePager from '../../composables/useTablePager'

const router = useRouter()
const route = useRoute()

const rows = ref([])
const loading = ref(false)
const activeStatus = ref('ALL')
const keyword = ref('')
const errorMsg = ref('')

const filteredRows = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter((item) => String(item.title || '').toLowerCase().includes(kw))
})

const pager = useTablePager(filteredRows, 10)

const statusLabel = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return '待处理'
  if (s === 'REPLIED') return '已回复'
  if (s === 'CLOSED') return '已关闭'
  if (s === 'ALL') return '全部'
  return raw || '-'
}

const statusBadge = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return 'warning'
  if (s === 'REPLIED') return 'success'
  return ''
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return String(raw)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const load = async ({ keepPage = false } = {}) => {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {}
    if (activeStatus.value && activeStatus.value !== 'ALL') {
      params.status = activeStatus.value
    }
    const { data } = await http.get('/feedbacks/my', { params, meta: { silent: true } })
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

const onSearchSubmit = () => {
  pager.resetPage()
}

const onStatusChange = () => {
  load()
}

const resetFilters = () => {
  keyword.value = ''
  activeStatus.value = 'ALL'
  load()
}

watch(keyword, () => {
  pager.resetPage()
})

const goDetail = (id) => {
  const seg = String(route.path || '').split('/')[1] || 'student'
  router.push(`/${seg}/feedback/${id}`)
}

const busyRef = computed(() => loading.value)

useIdleAutoRefresh({
  refreshFn: () => load({ keepPage: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: false
})

onMounted(() => {
  load()
})
</script>
