<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">管理员可查看辅导员已提交的测评单明细（只读）。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="keyword"
          width="320px"
          placeholder="搜索学号/姓名/班级"
          :disabled="loadingTasks || loadingDetail"
          @submit="onSearchSubmit"
          @clear="onSearchSubmit"
        />
      </div>
      <div class="table-search-right">
        <span class="muted">共 {{ pager.total.value }} 条</span>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky" data-resize-key="admin_review_tasks">
          <thead>
            <tr>
              <th>学号</th>
              <th>学生</th>
              <th>班级</th>
              <th>状态</th>
              <th>当前总分</th>
              <th>通过时间</th>
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
              <td>{{ formatDate(task.passTime || task.counselorSubmitTime) }}</td>
              <td>
                <button class="btn secondary" @click="openTask(task.id)" :disabled="loadingDetail">
                  {{ selectedSubmissionId === task.id && drawerOpen ? '已打开' : '查看' }}
                </button>
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
        :disabled="loadingTasks || loadingDetail"
        @change="onPageChange"
        @page-size-change="onPageSizeChange"
      />
    </div>
  </section>

  <div v-if="drawerOpen && current" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel drawer-wide drawer-review">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">查看测评单 #{{ current.submission.id }}</div>
          <p class="muted" style="margin-top: 6px;">
            学号：<b>{{ current.student.studentNo || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            学生：<b>{{ current.student.realName }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            状态：<span class="badge">{{ statusLabel(current.submission.status) }}</span>
          </p>
        </div>
        <button class="icon-btn" type="button" @click="closeDrawer" aria-label="关闭">X</button>
      </div>

      <div class="drawer-body">
        <h4 class="section-title">课程成绩</h4>
        <div class="table-shell" style="margin-top: 0;">
          <div class="table-scroll-drawer">
            <table class="table table-sticky">
              <thead>
                <tr>
                  <th class="nowrap">课程</th>
                  <th class="nowrap">类型</th>
                  <th class="nowrap">学分</th>
                  <th class="nowrap">原始分</th>
                  <th class="nowrap">审核状态</th>
                  <th class="nowrap">审核分</th>
                  <th>审核理由</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="course in current.courses || []" :key="`course_${course.id}`">
                  <td class="nowrap">{{ course.courseName }}</td>
                  <td class="nowrap">{{ courseTypeLabel(course.courseType) }}</td>
                  <td class="nowrap">{{ course.credit }}</td>
                  <td class="nowrap">{{ course.score }}</td>
                  <td class="nowrap"><span class="badge">{{ reviewStatusLabel(course.reviewStatus) }}</span></td>
                  <td class="nowrap">{{ course.reviewerScore ?? '-' }}</td>
                  <td>{{ course.reviewerComment || '-' }}</td>
                </tr>
                <tr v-if="!(current.courses || []).length">
                  <td colspan="7" class="empty">暂无课程数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <h4 class="section-title">活动成绩</h4>
        <div class="table-shell" style="margin-top: 0;">
          <div class="table-scroll-drawer">
            <table class="table table-sticky">
              <thead>
                <tr>
                  <th class="nowrap">模块</th>
                  <th class="nowrap">标题</th>
                  <th class="nowrap">自评分</th>
                  <th class="nowrap">审核状态</th>
                  <th class="nowrap">审核分</th>
                  <th>审核理由</th>
                  <th class="nowrap">证明图片</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="activity in current.activities || []" :key="`act_${activity.id}`">
                  <td class="nowrap">{{ moduleLabel(activity.moduleType) }}</td>
                  <td class="nowrap">{{ activity.title }}</td>
                  <td class="nowrap">{{ activity.selfScore }}</td>
                  <td class="nowrap"><span class="badge">{{ reviewStatusLabel(activity.reviewStatus) }}</span></td>
                  <td class="nowrap">{{ activity.finalScore ?? '-' }}</td>
                  <td>{{ activity.reviewerComment || '-' }}</td>
                  <td>
                    <div v-if="activity._evidenceMetas && activity._evidenceMetas.length" class="chip-list">
                      <span v-for="m in activity._evidenceMetas" :key="m.id" class="chip">
                        <button class="link" type="button" @click="previewEvidence(m.id, activity._evidenceMetas)">
                          {{ m.fileName || ('附件#' + m.id) }}
                        </button>
                      </span>
                    </div>
                    <span v-else class="muted" style="font-size: 12px;">未上传</span>
                  </td>
                </tr>
                <tr v-if="!(current.activities || []).length">
                  <td colspan="7" class="empty">暂无活动数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="drawer-footer">
        <button class="btn secondary" type="button" @click="closeDrawer">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import http from '../api/http'
import { previewImageById } from '../utils/imagePreview'
import SearchCapsule from '../components/SearchCapsule.vue'
import TablePager from '../components/TablePager.vue'
import useIdleAutoRefresh from '../composables/useIdleAutoRefresh'
import useTablePager from '../composables/useTablePager'

const tasks = ref([])
const loadingTasks = ref(false)
const loadingDetail = ref(false)
const keyword = ref('')

const drawerOpen = ref(false)
const current = ref(null)
const selectedSubmissionId = ref(null)
const evidenceMetaCache = reactive({})

const filteredTasks = computed(() => {
  const kw = String(keyword.value || '').trim().toLowerCase()
  return tasks.value.filter((item) => {
    if (!kw) return true
    const source = `${item.student_no || ''} ${item.real_name || ''} ${item.class_name || ''}`.toLowerCase()
    return source.includes(kw)
  })
})

const pager = useTablePager(filteredTasks, 10)

const statusLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const reviewStatusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'PENDING') return '待审核'
  if (code === 'APPROVED') return '已通过'
  if (code === 'REJECTED') return '已驳回'
  return raw || '-'
}

const courseTypeLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'REQUIRED') return '必修'
  if (code === 'ELECTIVE') return '选修'
  if (code === 'RETAKE') return '重修'
  if (code === 'RELEARN') return '再修'
  return raw || '-'
}

const moduleLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'MORAL') return '德育'
  if (code === 'INTEL_PRO_INNOV') return '智育'
  if (code === 'SPORT_ACTIVITY') return '体育'
  if (code === 'ART') return '美育'
  if (code === 'LABOR') return '劳育'
  return raw || '-'
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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

const parseEvidenceIds = (raw) => {
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => Number.isFinite(n) && n > 0)
}

const hydrateEvidenceMetas = async () => {
  if (!current.value?.activities) return

  const ids = []
  for (const a of current.value.activities) {
    ids.push(...parseEvidenceIds(a.evidenceFileIds))
  }
  const uniqueIds = Array.from(new Set(ids))
  if (!uniqueIds.length) {
    for (const a of current.value.activities) {
      a._evidenceMetas = []
    }
    return
  }

  const { data } = await http.post('/files/metas', { ids: uniqueIds })
  const metas = data.data || []
  const map = {}
  for (const m of metas) {
    map[m.id] = m
    evidenceMetaCache[m.id] = m
  }

  for (const a of current.value.activities) {
    const aIds = parseEvidenceIds(a.evidenceFileIds)
    a._evidenceMetas = aIds.map((id) => map[id] || evidenceMetaCache[id] || { id, fileName: `附件#${id}` })
  }
}

const openTask = async (submissionId) => {
  if (!submissionId) return
  loadingDetail.value = true
  try {
    const { data } = await http.get(`/submissions/${submissionId}`)
    current.value = data.data
    selectedSubmissionId.value = submissionId
    await hydrateEvidenceMetas()
    drawerOpen.value = true
  } finally {
    loadingDetail.value = false
  }
}

const closeDrawer = () => {
  drawerOpen.value = false
}

const previewEvidence = async (fileId, metas = []) => {
  const galleryIds = (metas || [])
    .map((m) => Number(m?.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  const fileNameMap = {}
  for (const m of metas || []) {
    const id = Number(m?.id)
    if (!Number.isFinite(id) || id <= 0) continue
    fileNameMap[id] = m.fileName || `附件#${id}`
  }
  await previewImageById(http, fileId, '证明材料预览', galleryIds, fileNameMap)
}

const onPageChange = (nextPage) => {
  pager.goPage(nextPage)
}

const onPageSizeChange = (nextSize) => {
  pager.setPageSize(nextSize)
}

const onSearchSubmit = () => {
  pager.resetPage()
}

watch(keyword, () => {
  pager.resetPage()
})

const busyRef = computed(() => loadingTasks.value || loadingDetail.value)
const pausedRef = computed(() => drawerOpen.value)

useIdleAutoRefresh({
  refreshFn: () => loadTasks({ keepPage: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: pausedRef
})

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}
</style>
