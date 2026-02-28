<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">辅导员审核</h2>
        <p class="muted" style="margin-top: 6px;">先选择待审核测评单，再逐项审核课程和活动。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <button class="search-back-icon" type="button" aria-label="恢复默认筛选" @click="resetTaskFilter">&lt;</button>
        <SearchCapsule
          v-model="taskKeyword"
          width="320px"
          placeholder="搜索学号/姓名/班级"
          :disabled="loadingTasks"
          @submit="onTaskSearch"
          @clear="onTaskSearch"
        />
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
              <th>总分</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in taskPager.pagedRows.value" :key="task.id">
              <td>{{ task.student_no || '-' }}</td>
              <td>{{ task.real_name }}</td>
              <td>{{ task.class_name }}</td>
              <td>{{ task.total_score ?? '-' }}</td>
              <td>{{ formatDate(task.submitted_at) }}</td>
              <td>
                <button class="btn secondary" @click="openTask(task.id)" :disabled="loadingDetail">
                  {{ selectedSubmissionId === task.id ? '已打开' : '打开审核' }}
                </button>
              </td>
            </tr>
            <tr v-if="!taskPager.pagedRows.value.length">
              <td colspan="6" class="empty">暂无符合条件的测评单</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="taskPager.page.value"
        :total-pages="taskPager.totalPages.value"
        :total="taskPager.total.value"
        :disabled="loadingTasks"
        @change="taskPager.goPage"
      />
    </div>
  </section>

  <div v-if="drawerOpen && current" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel drawer-wide">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">审核测评单 #{{ current.submission.id }}</div>
          <p class="muted" style="margin-top: 6px;">
            学号：<b>{{ current.student.studentNo || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            学生：<b>{{ current.student.realName }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            状态：<span class="badge">{{ current.submission.status }}</span>
          </p>
        </div>
        <button class="icon-btn" type="button" @click="closeDrawer" aria-label="关闭">X</button>
      </div>

      <div class="drawer-body">
        <h4 class="section-title">课程审核</h4>
        <div class="table-search-bar" style="margin-top: 0;">
          <div class="table-search-left">
            <button class="search-back-icon" type="button" aria-label="恢复默认筛选" @click="resetCourseFilter">&lt;</button>
            <SearchCapsule
              v-model="courseKeyword"
              width="300px"
              placeholder="搜索课程名称"
              :disabled="loadingDetail || isDeciding"
              @submit="onCourseSearch"
              @clear="onCourseSearch"
            />
            <select v-model="courseTypeFilter" style="width: 140px;" :disabled="loadingDetail || isDeciding">
              <option value="ALL">全部类型</option>
              <option value="REQUIRED">必修</option>
              <option value="ELECTIVE">选修</option>
              <option value="RETAKE">重修</option>
              <option value="RELEARN">再修</option>
            </select>
          </div>
        </div>

        <div class="table-shell">
          <div class="table-scroll-drawer">
            <table class="table table-sticky">
              <thead>
                <tr>
                  <th class="nowrap">课程</th>
                  <th class="nowrap">类型</th>
                  <th class="nowrap">分数</th>
                  <th>理由</th>
                  <th class="nowrap">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="course in coursePager.pagedRows.value" :key="`course_${course.id}`">
                  <td class="nowrap">{{ course.courseName }}</td>
                  <td class="nowrap">{{ courseTypeLabel(course.courseType) }}</td>
                  <td class="nowrap">{{ course.score }}</td>
                  <td>
                    <input v-model.trim="drafts[courseKey(course.id)].reason" placeholder="可填写审核理由（选填）" />
                  </td>
                  <td>
                    <div class="action-row inline-actions">
                      <button class="btn" type="button" @click="decide('COURSE', course.id, 'APPROVE')" :disabled="isDeciding">通过</button>
                      <button class="btn danger" type="button" @click="decide('COURSE', course.id, 'REJECT')" :disabled="isDeciding">驳回</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!coursePager.pagedRows.value.length">
                  <td colspan="5" class="empty">暂无符合条件的课程</td>
                </tr>
              </tbody>
            </table>
          </div>
          <TablePager
            :page="coursePager.page.value"
            :total-pages="coursePager.totalPages.value"
            :total="coursePager.total.value"
            :disabled="isDeciding || loadingDetail"
            @change="coursePager.goPage"
          />
        </div>

        <h4 class="section-title">活动审核</h4>
        <div class="table-search-bar" style="margin-top: 0;">
          <div class="table-search-left">
            <button class="search-back-icon" type="button" aria-label="恢复默认筛选" @click="resetActivityFilter">&lt;</button>
            <SearchCapsule
              v-model="activityKeyword"
              width="300px"
              placeholder="搜索活动标题"
              :disabled="loadingDetail || isDeciding"
              @submit="onActivitySearch"
              @clear="onActivitySearch"
            />
            <select v-model="activityModuleFilter" style="width: 140px;" :disabled="loadingDetail || isDeciding">
              <option value="ALL">全部模块</option>
              <option value="MORAL">德育</option>
              <option value="INTEL_PRO_INNOV">智育</option>
              <option value="SPORT_ACTIVITY">体育</option>
              <option value="ART">美育</option>
              <option value="LABOR">劳育</option>
            </select>
          </div>
        </div>

        <div class="table-shell">
          <div class="table-scroll-drawer">
            <table class="table table-sticky activity-table">
              <thead>
                <tr>
                  <th class="nowrap col-module">模块</th>
                  <th class="nowrap col-title">标题</th>
                  <th class="nowrap col-score">分数</th>
                  <th class="nowrap col-evidence">证明图片</th>
                  <th class="nowrap col-reason">理由</th>
                  <th class="nowrap col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="activity in activityPager.pagedRows.value" :key="`act_${activity.id}`">
                  <td class="nowrap">{{ moduleLabel(activity.moduleType) }}</td>
                  <td class="nowrap">{{ activity.title }}</td>
                  <td class="nowrap">{{ activity.selfScore }}</td>
                  <td>
                    <div v-if="activity._evidenceMetas && activity._evidenceMetas.length" class="chip-list">
                      <span v-for="m in activity._evidenceMetas" :key="m.id" class="chip">
                        <button class="link" type="button" @click="previewEvidence(m.id)">{{ m.fileName || ('附件#' + m.id) }}</button>
                      </span>
                    </div>
                    <span v-else class="muted" style="font-size: 12px;">未上传</span>
                  </td>
                  <td>
                    <input v-model.trim="drafts[activityKey(activity.id)].reason" placeholder="可填写审核理由（选填）" />
                  </td>
                  <td>
                    <div class="action-row inline-actions">
                      <button class="btn" type="button" @click="decide('ACTIVITY', activity.id, 'APPROVE')" :disabled="isDeciding">通过</button>
                      <button class="btn danger" type="button" @click="decide('ACTIVITY', activity.id, 'REJECT')" :disabled="isDeciding">驳回</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!activityPager.pagedRows.value.length">
                  <td colspan="6" class="empty">暂无符合条件的活动</td>
                </tr>
              </tbody>
            </table>
          </div>
          <TablePager
            :page="activityPager.page.value"
            :total-pages="activityPager.totalPages.value"
            :total="activityPager.total.value"
            :disabled="isDeciding || loadingDetail"
            @change="activityPager.goPage"
          />
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
const current = ref(null)
const selectedSubmissionId = ref(null)

const loadingTasks = ref(false)
const loadingDetail = ref(false)
const isDeciding = ref(false)
const drawerOpen = ref(false)

const drafts = reactive({})
const evidenceMetaCache = reactive({})

const taskKeyword = ref('')
const courseKeyword = ref('')
const courseTypeFilter = ref('ALL')
const activityKeyword = ref('')
const activityModuleFilter = ref('ALL')

const courseKey = (id) => `COURSE_${id}`
const activityKey = (id) => `ACTIVITY_${id}`

const filteredTasks = computed(() => {
  const kw = String(taskKeyword.value || '').trim().toLowerCase()
  return tasks.value.filter((task) => {
    if (!kw) return true
    const source = `${task.student_no || ''} ${task.real_name || ''} ${task.class_name || ''}`.toLowerCase()
    return source.includes(kw)
  })
})

const taskPager = useTablePager(filteredTasks, 10)

const filteredCourses = computed(() => {
  const list = current.value?.courses || []
  const kw = String(courseKeyword.value || '').trim().toLowerCase()
  return list.filter((item) => {
    const matchKeyword = !kw || String(item.courseName || '').toLowerCase().includes(kw)
    const matchType = courseTypeFilter.value === 'ALL' || String(item.courseType || '').toUpperCase() === courseTypeFilter.value
    return matchKeyword && matchType
  })
})

const coursePager = useTablePager(filteredCourses, 10)

const filteredActivities = computed(() => {
  const list = current.value?.activities || []
  const kw = String(activityKeyword.value || '').trim().toLowerCase()
  return list.filter((item) => {
    const matchKeyword = !kw || String(item.title || '').toLowerCase().includes(kw)
    const matchModule = activityModuleFilter.value === 'ALL' || String(item.moduleType || '').toUpperCase() === activityModuleFilter.value
    return matchKeyword && matchModule
  })
})

const activityPager = useTablePager(filteredActivities, 10)

const courseTypeLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'REQUIRED') return '必修'
  if (code === 'ELECTIVE') return '选修'
  if (code === 'RETAKE') return '重修'
  if (code === 'RELEARN') return '再修'
  return raw || '-'
}

const moduleLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'MORAL') return '德育'
  if (code === 'INTEL_PRO_INNOV') return '智育'
  if (code === 'SPORT_ACTIVITY') return '体育'
  if (code === 'ART') return '美育'
  if (code === 'LABOR') return '劳育'
  return raw || '-'
}

const isAutoReason = (text) => {
  const value = String(text || '').trim().toUpperCase()
  return value === '辅导员APPROVE'.toUpperCase() || value === '辅导员REJECT'.toUpperCase()
}

const normalizeReason = (text) => {
  if (!text || isAutoReason(text)) return ''
  return String(text).trim()
}

const ensureDraft = (key) => {
  if (!drafts[key]) {
    drafts[key] = {
      reason: ''
    }
  }
}

const initDrafts = (detail) => {
  ;(detail.courses || []).forEach((course) => {
    const key = courseKey(course.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(course.reviewerComment)
  })

  ;(detail.activities || []).forEach((activity) => {
    const key = activityKey(activity.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(activity.reviewerComment)
  })
}

const loadTasks = async ({ keepPage = false } = {}) => {
  loadingTasks.value = true
  try {
    const { data } = await http.get('/reviews/tasks')
    tasks.value = data.data || []
    if (!keepPage) {
      taskPager.resetPage()
    }
  } finally {
    loadingTasks.value = false
  }
}

const resetDrawerFilters = () => {
  courseKeyword.value = ''
  courseTypeFilter.value = 'ALL'
  activityKeyword.value = ''
  activityModuleFilter.value = 'ALL'
  coursePager.resetPage()
  activityPager.resetPage()
}

const openTask = async (submissionId) => {
  loadingDetail.value = true
  try {
    const { data } = await http.get(`/submissions/${submissionId}`)
    current.value = data.data
    selectedSubmissionId.value = submissionId
    initDrafts(data.data)
    await hydrateEvidenceMetas()
    resetDrawerFilters()
    drawerOpen.value = true
  } finally {
    loadingDetail.value = false
  }
}

const closeDrawer = () => {
  drawerOpen.value = false
}

const reloadCurrent = async () => {
  if (!selectedSubmissionId.value) return
  await openTask(selectedSubmissionId.value)
}

const decide = async (itemType, itemId, action) => {
  const key = itemType === 'COURSE' ? courseKey(itemId) : activityKey(itemId)
  ensureDraft(key)

  const payload = {
    action,
    reason: (drafts[key].reason || '').trim()
  }

  isDeciding.value = true
  try {
    await http.post(`/reviews/items/${itemType}/${itemId}/decision`, payload)
    await reloadCurrent()
    await loadTasks({ keepPage: true })
    alert('审核操作已提交')
  } finally {
    isDeciding.value = false
  }
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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

const previewEvidence = async (fileId) => {
  await previewImageById(http, fileId, '证明材料预览')
}

const onTaskSearch = () => {
  taskPager.resetPage()
}

const onCourseSearch = () => {
  coursePager.resetPage()
}

const onActivitySearch = () => {
  activityPager.resetPage()
}

const resetTaskFilter = () => {
  taskKeyword.value = ''
  taskPager.resetPage()
}

const resetCourseFilter = () => {
  courseKeyword.value = ''
  courseTypeFilter.value = 'ALL'
  coursePager.resetPage()
}

const resetActivityFilter = () => {
  activityKeyword.value = ''
  activityModuleFilter.value = 'ALL'
  activityPager.resetPage()
}

watch([courseKeyword, courseTypeFilter], () => {
  coursePager.resetPage()
})

watch([activityKeyword, activityModuleFilter], () => {
  activityPager.resetPage()
})

const busyRef = computed(() => loadingTasks.value || loadingDetail.value || isDeciding.value)
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

.inline-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.activity-table .col-module,
.activity-table .col-title,
.activity-table .col-score,
.activity-table .col-evidence,
.activity-table .col-reason,
.activity-table .col-action {
  white-space: nowrap;
}
</style>
