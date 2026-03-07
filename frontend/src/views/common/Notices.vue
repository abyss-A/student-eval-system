<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">发布通知、查看公告。学生仅可查看已发布公告。</p>
      </div>
      <div class="toolbar-row">
        <el-button v-if="canManage" type="primary" @click="openCreate">新建公告</el-button>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="keyword"
          width="180px"
          placeholder="搜索公告标题/内容"
          :disabled="loading"
          @submit="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-if="canManage"
          v-model="activeTab"
          style="width: 160px;"
          :disabled="loading"
          @change="onStatusChange"
        >
          <el-option label="全部状态" value="ALL" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
      </div>
      <div v-if="canManage" class="table-search-right">
        <span class="muted">已选 {{ selection.selectedCount.value }} 项</span>
        <el-button type="default" :disabled="!canBatchOperate" @click="batchPublish">批量发布</el-button>
        <el-button type="default" :disabled="!canBatchOperate" @click="batchOffline">批量下线</el-button>
        <el-button type="danger" :disabled="!canBatchOperate" @click="batchDelete">批量删除</el-button>
      </div>
    </div>

    <div v-if="errorMsg" class="card notices-error-card">
      <el-alert type="error" :closable="false" title="加载失败" />
      <div class="muted notices-error-text">{{ errorMsg }}</div>
      <div class="toolbar-row notices-error-actions">
        <el-button type="default" :loading="loading" @click="load">重试</el-button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky table-fixed-right" style="--sticky-action-w: 250px; --sticky-status-w: 110px;" data-resize-key="notices_main">
          <thead>
            <tr>
              <th style="width: 44px;">
                <input
                  ref="headerCheckboxRef"
                  type="checkbox"
                  :checked="allCheckedOnPage"
                  :disabled="!pagedNoticeIds.length || loading"
                  @change="toggleAllOnPage"
                />
              </th>
              <th>标题</th>
              <th style="width: 140px;">发布人</th>
              <th style="width: 170px;">发布时间</th>
              <th style="width: 170px;">更新时间</th>
              <th class="col-status" style="width: 110px;">状态</th>
              <th class="col-action" style="width: 250px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="n in pager.pagedRows.value" :key="n.id">
              <td>
                <input
                  type="checkbox"
                  :checked="selection.isSelected(n.id)"
                  :disabled="loading"
                  @change="selection.toggle(n.id)"
                />
              </td>
              <td>
                <TableOverflowCell
                  :text="n.title"
                  :cell-key="`notice_title_${n.id}`"
                />
              </td>
              <td>{{ n.publisher_real_name || '-' }}</td>
              <td>{{ formatDate(n.published_at) }}</td>
              <td>{{ formatDate(n.updated_at) }}</td>
              <td class="col-status">
                <span class="badge" :class="statusBadge(n.status)">{{ statusLabel(n.status) }}</span>
              </td>
              <td class="col-action">
                <div class="action-row">
                  <el-button type="default" @click="goDetail(n.id)">查看</el-button>
                  <el-button v-if="canManage && canEdit(n)" type="default" @click="openEdit(n)">编辑</el-button>
                  <el-button v-if="canManage && canPublish(n)" type="primary" @click="publish(n.id)">发布</el-button>
                  <el-button v-if="canManage && canOffline(n)" type="default" @click="offline(n.id)">下线</el-button>
                  <el-button v-if="canManage && canDelete(n)" type="danger" @click="remove(n.id)">删除</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!pager.pagedRows.value.length && !errorMsg">
              <td colspan="7" class="empty">暂无公告</td>
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
        @change="onPageChange"
        @page-size-change="onPageSizeChange"
      />
    </div>
  </section>

  <div v-if="drawer.open" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">{{ drawer.mode === 'create' ? '新建公告' : '编辑公告' }}</div>
          <p class="muted" style="margin-top: 6px;">公告内容支持换行显示。已发布公告需先下线再修改。</p>
        </div>
        <el-button class="workspace-tool-btn" type="default" circle @click="closeDrawer" aria-label="关闭">×</el-button>
      </div>

      <div class="drawer-body">
        <div class="stack">
          <label class="field">
            <span class="field-label">标题</span>
            <el-input v-model.trim="form.title" placeholder="请输入公告标题" />
          </label>
          <label class="field">
            <span class="field-label">内容</span>
            <el-input
              v-model.trim="form.content"
              type="textarea"
              :rows="10"
              placeholder="请输入公告内容（支持换行）"
            />
          </label>
        </div>
      </div>

      <div class="drawer-footer">
        <el-button type="default" @click="closeDrawer" :disabled="drawer.saving">取消</el-button>
        <el-button type="primary" @click="saveDraft" :loading="drawer.saving">
          {{ drawer.saving ? '保存中...' : '保存草稿' }}
        </el-button>
        <el-button v-if="drawer.mode === 'edit'" type="default" @click="publish(form.id)" :disabled="drawer.saving">发布</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import SearchCapsule from '../../components/SearchCapsule.vue'
import TableOverflowCell from '../../components/TableOverflowCell.vue'
import TablePager from '../../components/TablePager.vue'
import useIdleAutoRefresh from '../../composables/useIdleAutoRefresh'
import useTablePager from '../../composables/useTablePager'
import useTableSelection from '../../composables/useTableSelection'
import { getRole, getUserId } from '../../utils/auth'

const router = useRouter()
const route = useRoute()

const role = ref(getRole())
const myUserId = Number(getUserId() || 0)

const canManage = computed(() => role.value === 'COUNSELOR' || role.value === 'ADMIN')

const activeTab = ref('PUBLISHED')
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const errorMsg = ref('')

const filteredRows = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter((item) => {
    const source = `${item.title || ''} ${item.content || ''}`.toLowerCase()
    return source.includes(kw)
  })
})

const pager = useTablePager(filteredRows, 10)
const selection = useTableSelection()
const headerCheckboxRef = ref(null)

const pagedNoticeIds = computed(() => pager.pagedRows.value.map((item) => item.id))
const allCheckedOnPage = computed(() => selection.isAllCheckedOnPage(pagedNoticeIds.value))
const indeterminateOnPage = computed(() => selection.isIndeterminateOnPage(pagedNoticeIds.value))

const drawer = reactive({
  open: false,
  mode: 'create',
  saving: false
})
const canBatchOperate = computed(() => canManage.value && selection.selectedCount.value > 0 && !loading.value && !drawer.saving)

const form = reactive({
  id: null,
  title: '',
  content: '',
  status: ''
})

const statusLabel = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'DRAFT') return '草稿'
  if (s === 'PUBLISHED') return '已发布'
  if (s === 'OFFLINE') return '已下线'
  return raw || '-'
}

const statusBadge = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'PUBLISHED') return 'success'
  if (s === 'DRAFT') return 'warning'
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

const canEdit = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'DRAFT' && status !== 'OFFLINE') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canPublish = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'DRAFT' && status !== 'OFFLINE') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canOffline = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'PUBLISHED') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canDelete = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status === 'PUBLISHED') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const load = async ({ keepPage = false } = {}) => {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {}
    if (canManage.value) {
      if (activeTab.value && activeTab.value !== 'ALL') params.status = activeTab.value
      if (keyword.value) params.keyword = keyword.value
    }

    const { data } = await http.get('/notices', { params, meta: { silent: true } })
    rows.value = data.data || []
    selection.clear()
    if (!keepPage) {
      pager.resetPage()
    }
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    rows.value = []
    selection.clear()
    if (!keepPage) {
      pager.resetPage()
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (canManage.value) {
    load()
  } else {
    pager.resetPage()
  }
}

const onStatusChange = () => {
  if (!canManage.value) return
  load()
}

const resetFilters = () => {
  keyword.value = ''
  if (canManage.value) {
    load()
  } else {
    pager.resetPage()
  }
}

watch(keyword, () => {
  selection.clear()
  pager.resetPage()
})

watch(indeterminateOnPage, (value) => {
  if (!headerCheckboxRef.value) return
  headerCheckboxRef.value.indeterminate = value
})

const selectedRowsOnPage = computed(() => {
  const selected = new Set(selection.selectedList.value)
  return pager.pagedRows.value.filter((item) => selected.has(String(item.id)))
})

const toggleAllOnPage = () => {
  selection.toggleAll(pagedNoticeIds.value)
}

const onPageChange = (nextPage) => {
  pager.goPage(nextPage)
  selection.clear()
}

const onPageSizeChange = (nextSize) => {
  pager.setPageSize(nextSize)
  selection.clear()
}

const runBatchAction = async ({ label, canRun, run }) => {
  const selectedRows = selectedRowsOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要处理的数据')
    return
  }
  if (!confirm(`确认${label}已选 ${selectedRows.length} 项吗？`)) return

  let success = 0
  let failed = 0
  let skipped = 0

  for (const row of selectedRows) {
    if (!canRun(row)) {
      skipped += 1
      continue
    }
    try {
      await run(row)
      success += 1
    } catch (e) {
      failed += 1
    }
  }

  selection.clear()
  await load({ keepPage: true })
  alert(`${label}完成：成功 ${success}，失败 ${failed}，跳过 ${skipped}`)
}

const batchPublish = async () => {
  await runBatchAction({
    label: '批量发布',
    canRun: canPublish,
    run: (item) => http.post(`/notices/${item.id}/publish`)
  })
}

const batchOffline = async () => {
  await runBatchAction({
    label: '批量下线',
    canRun: canOffline,
    run: (item) => http.post(`/notices/${item.id}/offline`)
  })
}

const batchDelete = async () => {
  await runBatchAction({
    label: '批量删除',
    canRun: canDelete,
    run: (item) => http.delete(`/notices/${item.id}`)
  })
}

const goDetail = (id) => {
  router.push(`${route.path}/${id}`)
}

const openCreate = () => {
  drawer.open = true
  drawer.mode = 'create'
  form.id = null
  form.title = ''
  form.content = ''
  form.status = 'DRAFT'
}

const openEdit = (n) => {
  drawer.open = true
  drawer.mode = 'edit'
  form.id = n.id
  form.title = n.title || ''
  form.content = n.content || ''
  form.status = n.status || ''
}

const closeDrawer = () => {
  drawer.open = false
  drawer.saving = false
}

const saveDraft = async () => {
  if (!canManage.value) return
  if (!form.title.trim() || !form.content.trim()) {
    alert('标题和内容不能为空')
    return
  }

  drawer.saving = true
  try {
    if (drawer.mode === 'create') {
      const { data } = await http.post('/notices', { title: form.title, content: form.content })
      form.id = data.data?.id
      drawer.mode = 'edit'
    } else {
      await http.put(`/notices/${form.id}`, { title: form.title, content: form.content })
    }
    await load({ keepPage: true })
    alert('已保存')
  } finally {
    drawer.saving = false
  }
}

const publish = async (id) => {
  if (!id) {
    alert('请先保存草稿')
    return
  }
  await http.post(`/notices/${id}/publish`)
  await load({ keepPage: true })
  alert('已发布')
}

const offline = async (id) => {
  await http.post(`/notices/${id}/offline`)
  await load({ keepPage: true })
  alert('已下线')
}

const remove = async (id) => {
  if (!confirm('确认删除该公告？')) return
  await http.delete(`/notices/${id}`)
  await load({ keepPage: true })
  alert('已删除')
}

const busyRef = computed(() => loading.value || drawer.saving)
const pausedRef = computed(() => drawer.open)

useIdleAutoRefresh({
  refreshFn: () => load({ keepPage: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: pausedRef
})

onMounted(() => {
  load()
})
</script>

<style scoped>
.notices-error-card {
  margin-top: 12px;
  border-color: #fecaca;
  background: #fef2f2;
  box-shadow: none;
}

.notices-error-text {
  margin-top: 6px;
  white-space: pre-wrap;
}

.notices-error-actions {
  margin-top: 10px;
}
</style>

