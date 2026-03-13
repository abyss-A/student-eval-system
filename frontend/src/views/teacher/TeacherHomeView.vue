<template>
  <section class="dash-page">
    <div class="card dash-hero dash-hero--teacher">
      <div class="dash-hero__top">
        <div>
          <p class="dash-hero__kicker">欢迎回来，<b>{{ realName || '老师' }}</b></p>
          <div class="dash-hero__headline">审核首页</div>
          <div class="dash-hero__meta">
            <el-tag type="info" effect="plain">总待办：{{ tasks.length }}</el-tag>
            <el-tag type="success" effect="plain">待提交：{{ countReadyToSubmit }}</el-tag>
            <el-tag type="success" effect="plain">已提交管理员：{{ countSubmitted }}</el-tag>
          </div>
        </div>

        <div class="dash-hero__actions">
          <el-button type="primary" @click="go('/teacher/review/tasks')">进入待审核列表</el-button>
        </div>
      </div>

      <div v-if="loading" class="dash-empty" style="padding: 12px 0;">加载中...</div>
      <div v-else class="dash-hero__bottom dash-hero__bottom--single">
        <div class="dash-progress">
          <div class="dash-progress__head">
            <div class="dash-progress__title">阶段分布</div>
            <div class="muted">未提交管理员：{{ actionableCount }}</div>
          </div>
          <div style="margin-top: 10px;">
            <div class="dash-segments" aria-label="审核阶段分布">
              <div v-if="countUnreviewed" class="dash-seg dash-seg--danger" :style="{ flexGrow: countUnreviewed, flexBasis: 0 }" />
              <div v-if="countInProgress" class="dash-seg dash-seg--info" :style="{ flexGrow: countInProgress, flexBasis: 0 }" />
              <div v-if="countReviewed" class="dash-seg dash-seg--warning" :style="{ flexGrow: countReviewed, flexBasis: 0 }" />
              <div v-if="countReadyToSubmit" class="dash-seg dash-seg--success" :style="{ flexGrow: countReadyToSubmit, flexBasis: 0 }" />
            </div>
          </div>
          <div class="dash-progress__hint">
            未审核 <b>{{ countUnreviewed }}</b> · 审核中 <b>{{ countInProgress }}</b> · 待复审 <b>{{ countReviewed }}</b> · 待提交 <b>{{ countReadyToSubmit }}</b>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="dash-cols">
      <div class="dash-col dash-col--fill">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">待办列表预览</h3>
            <el-button type="default" size="small" @click="go('/teacher/review/tasks')">查看全部</el-button>
          </div>

          <div v-if="topTasks.length" class="dash-list">
            <div v-for="t in topTasks" :key="t.id" class="dash-row">
              <div class="dash-row__main">
                <div class="dash-row__title">{{ t.real_name || '-' }}（{{ t.account_no || t.accountNo || '-' }}）</div>
                <div class="dash-row__meta">
                  {{ t.class_name || '-' }} · {{ pickTaskDoneCount(t) }}/{{ pickTaskTotalCount(t) }} · {{ formatDate(pickTaskSubmittedAt(t)) }}
                </div>
              </div>
              <div class="dash-row__side">
                <el-tag :type="taskTagType(t)" effect="plain">{{ taskProgressLabel(t) }}</el-tag>
                <el-button size="small" type="primary" @click="openTaskFromHome(t)">继续审核</el-button>
              </div>
            </div>
          </div>
          <div v-else class="dash-empty">暂无待办</div>
        </div>
      </div>

      <div class="dash-col dash-col--fill">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">班级提交情况</h3>
            <el-button size="small" type="default" @click="classDialogOpen = true">更多</el-button>
          </div>

          <div v-if="classPreview.length" class="dash-table-wrap">
            <table class="table dash-table">
              <thead>
                <tr>
                  <th style="width: 36%;">班级</th>
                  <th class="dash-table-num">未提交</th>
                  <th class="dash-table-num">未审核</th>
                  <th class="dash-table-num">审核中</th>
                  <th class="dash-table-num">待复审</th>
                  <th class="dash-table-num">待提交</th>
                  <th class="dash-table-num">已提交</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in classPreview" :key="c.className" class="dash-table-row" @click="openClassTasks(c)">
                  <td>
                    <span class="dash-table-ellipsis" :title="c.className">{{ c.className }}</span>
                  </td>
                  <td class="dash-table-num">{{ c.notSubmittedCount }}</td>
                  <td class="dash-table-num">{{ c.unreviewedCount }}</td>
                  <td class="dash-table-num">{{ c.inProgressCount }}</td>
                  <td class="dash-table-num">{{ c.reviewedCount }}</td>
                  <td class="dash-table-num">{{ c.readyToSubmitCount }}</td>
                  <td class="dash-table-num">{{ c.submittedToAdminCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="dash-empty">暂无数据</div>
        </div>
      </div>
    </div>
  </section>

  <el-dialog v-model="classDialogOpen" width="980px" top="6vh">
    <template #header>
      <div class="dash-dialog-head">
        <h3 class="dash-dialog-title">班级提交情况</h3>
        <span class="muted">共 {{ classOverview.length }} 个班级</span>
      </div>
    </template>

    <div class="dash-dialog-scroll">
      <table class="table dash-table">
        <thead>
          <tr>
            <th style="width: 36%;">班级</th>
            <th class="dash-table-num">未提交</th>
            <th class="dash-table-num">未审核</th>
            <th class="dash-table-num">审核中</th>
            <th class="dash-table-num">待复审</th>
            <th class="dash-table-num">待提交</th>
            <th class="dash-table-num">已提交</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in classOverview" :key="`dialog_${c.className}`" class="dash-table-row" @click="openClassTasks(c)">
            <td>
              <span class="dash-table-ellipsis" :title="c.className">{{ c.className }}</span>
            </td>
            <td class="dash-table-num">{{ c.notSubmittedCount }}</td>
            <td class="dash-table-num">{{ c.unreviewedCount }}</td>
            <td class="dash-table-num">{{ c.inProgressCount }}</td>
            <td class="dash-table-num">{{ c.reviewedCount }}</td>
            <td class="dash-table-num">{{ c.readyToSubmitCount }}</td>
            <td class="dash-table-num">{{ c.submittedToAdminCount }}</td>
          </tr>
          <tr v-if="!classOverview.length">
            <td colspan="7" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <template #footer>
      <div style="display: flex; justify-content: flex-end;">
        <el-button type="default" @click="classDialogOpen = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'
import { getRealName } from '../../utils/auth'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const classOverview = ref([])
const classDialogOpen = ref(false)

const realName = computed(() => getRealName())

const normalizeUpper = (value) => String(value || '').trim().toUpperCase()
const taskStatus = (task) => normalizeUpper(task?.status)

const pickTaskDoneCount = (task) => {
  const raw = task?.reviewDoneCount ?? task?.review_done_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskTotalCount = (task) => {
  const raw = task?.reviewTotalCount ?? task?.review_total_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskRejectedCount = (task) => {
  const raw = task?.reviewRejectedCount ?? task?.review_rejected_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskPhase = (task) => {
  const raw = normalizeUpper(task?.reviewPhase || task?.review_phase || task?.review_progress || '')
  if (raw === 'DONE') {
    return pickTaskRejectedCount(task) > 0 ? 'REVIEWED' : 'READY_TO_SUBMIT'
  }
  if (!raw || raw === 'NOT_REVIEWED') return 'NOT_REVIEWED'
  return raw
}

const isSubmittedToAdmin = (task) => taskStatus(task) === 'COUNSELOR_REVIEWED'

const taskProgressCode = (task) => {
  if (isSubmittedToAdmin(task)) return 'SUBMITTED'
  const phase = pickTaskPhase(task)
  if (phase === 'READY_TO_SUBMIT') return 'READY_TO_SUBMIT'
  if (phase === 'REVIEWED') return 'REVIEWED'
  if (phase === 'IN_PROGRESS') return 'IN_PROGRESS'
  return 'UNREVIEWED'
}

const taskProgressLabel = (task) => {
  const code = taskProgressCode(task)
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'READY_TO_SUBMIT') return '待提交'
  if (code === 'REVIEWED') return '待复审'
  if (code === 'IN_PROGRESS') return '正在审核'
  return '未审核'
}

const canSubmitTask = (task) => {
  if (!task) return false
  if (isSubmittedToAdmin(task)) return false
  const progress = pickTaskPhase(task)
  return progress === 'READY_TO_SUBMIT' && taskStatus(task) === 'SUBMITTED'
}

const taskTagType = (task) => {
  const code = taskProgressCode(task)
  if (code === 'READY_TO_SUBMIT') return 'success'
  if (code === 'REVIEWED') return 'warning'
  if (code === 'IN_PROGRESS') return 'info'
  if (code === 'UNREVIEWED') return 'danger'
  return 'success'
}

const countSubmitted = computed(() => tasks.value.filter((t) => isSubmittedToAdmin(t)).length)
const countReadyToSubmit = computed(() => tasks.value.filter((t) => canSubmitTask(t)).length)
const countReviewed = computed(() => tasks.value.filter((t) => !isSubmittedToAdmin(t) && taskProgressCode(t) === 'REVIEWED').length)
const countInProgress = computed(() => tasks.value.filter((t) => !isSubmittedToAdmin(t) && taskProgressCode(t) === 'IN_PROGRESS').length)
const countUnreviewed = computed(() => tasks.value.filter((t) => !isSubmittedToAdmin(t) && taskProgressCode(t) === 'UNREVIEWED').length)
const actionableCount = computed(() => tasks.value.filter((t) => !isSubmittedToAdmin(t)).length)

const pickTaskSubmittedAt = (task) => task?.submitted_at || task?.submittedAt || task?.submitted_time || ''

const stageRank = (task) => {
  const code = taskProgressCode(task)
  if (code === 'UNREVIEWED') return 0
  if (code === 'IN_PROGRESS') return 1
  if (code === 'REVIEWED') return 2
  if (code === 'READY_TO_SUBMIT') return 3
  return 99
}

const topTasks = computed(() => {
  const items = tasks.value.filter((t) => !isSubmittedToAdmin(t))
  return items
    .slice()
    .sort((a, b) => {
      const ra = stageRank(a)
      const rb = stageRank(b)
      if (ra !== rb) return ra - rb
      const ta = new Date(pickTaskSubmittedAt(a) || 0).getTime()
      const tb = new Date(pickTaskSubmittedAt(b) || 0).getTime()
      if (Number.isFinite(tb) && Number.isFinite(ta) && tb !== ta) return tb - ta
      return Number(b?.id || 0) - Number(a?.id || 0)
    })
    .slice(0, 3)
})

const classPreview = computed(() => classOverview.value.slice(0, 4))

const go = (path, query = undefined) => {
  if (query) router.push({ path, query })
  else router.push(path)
}

const openTaskFromHome = (task) => {
  const id = task?.id
  if (!id) return
  router.push({ path: '/teacher/review/tasks', query: { open: String(id) } })
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return String(raw)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const loadTasks = async () => {
  const { data } = await http.get('/reviews/tasks', { meta: { silent: true } })
  tasks.value = Array.isArray(data.data) ? data.data : []
}

const loadClassOverview = async () => {
  const { data } = await http.get('/reviews/class-overview', { meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  classOverview.value = rows.map((row) => ({
    className: row?.className || row?.class_name || '-',
    notSubmittedCount: Number(row?.notSubmittedCount ?? row?.not_submitted_count ?? 0),
    unreviewedCount: Number(row?.unreviewedCount ?? row?.unreviewed_count ?? 0),
    inProgressCount: Number(row?.inProgressCount ?? row?.in_progress_count ?? 0),
    reviewedCount: Number(row?.reviewedCount ?? row?.reviewed_count ?? 0),
    readyToSubmitCount: Number(row?.readyToSubmitCount ?? row?.ready_to_submit_count ?? 0),
    submittedToAdminCount: Number(row?.submittedToAdminCount ?? row?.submitted_to_admin_count ?? 0)
  }))
}

const openClassTasks = (row) => {
  const className = String(row?.className || '').trim()
  if (!className || className === '-') return
  classDialogOpen.value = false
  router.push({ path: '/teacher/review/tasks', query: { kw: className } })
}

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadTasks(), loadClassOverview()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.dash-table-wrap {
  margin-top: 6px;
}

.dash-table-row {
  cursor: pointer;
}

.dash-dialog-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.dash-dialog-title {
  margin: 0;
  font-size: 16px;
  font-weight: 850;
  color: #0f2c53;
}

.dash-dialog-scroll {
  max-height: 62vh;
  overflow: auto;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: #fff;
}

.dash-table {
  width: 100%;
}

.dash-table :deep(th),
.dash-table :deep(td) {
  padding: 7px 8px;
  font-size: 12px;
}

.dash-table-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.dash-table-ellipsis {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
}
</style>
