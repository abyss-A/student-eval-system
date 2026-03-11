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
          <el-button type="default" @click="go('/teacher/notices')">公告管理</el-button>
        </div>
      </div>

      <div v-if="loading" class="dash-empty" style="padding: 12px 0;">加载中...</div>
      <div v-else class="dash-hero__bottom">
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
          <div class="dash-progress__hint" style="margin-top: 10px;">
            未审核 <b>{{ countUnreviewed }}</b> · 审核中 <b>{{ countInProgress }}</b> · 待复审 <b>{{ countReviewed }}</b> · 待提交 <b>{{ countReadyToSubmit }}</b>
          </div>
        </div>

        <div class="dash-metric">
          <div class="dash-metric__label">待提交管理员</div>
          <div class="dash-metric__value">{{ countReadyToSubmit }}</div>
          <div class="muted" style="font-size: 12px;">可在列表中批量提交管理员。</div>
          <div style="margin-top: 8px;">
            <el-button size="small" type="primary" @click="go('/teacher/review/tasks', { preset: 'READY_TO_SUBMIT' })">去批量提交</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="dash-grid">
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

      <div class="dash-stack">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">可提交管理员</h3>
            <p class="dash-card__desc">READY_TO_SUBMIT 且已提交</p>
          </div>
          <p class="muted" style="margin-top: 0; line-height: 1.7;">
            当前共有 <b>{{ countReadyToSubmit }}</b> 份测评单已完成审核，可批量提交管理员。
          </p>
          <div class="dash-hero__actions" style="margin-top: 12px;">
            <el-button type="primary" @click="go('/teacher/review/tasks', { preset: 'READY_TO_SUBMIT' })">去批量提交</el-button>
            <el-button type="default" @click="go('/teacher/feedback/create')">我要反馈</el-button>
          </div>
        </div>

        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">公告通知</h3>
            <el-button type="default" size="small" @click="go('/teacher/notices')">更多</el-button>
          </div>
          <div v-if="notices.length" class="dash-list">
            <div v-for="n in notices" :key="n.id" class="dash-row">
              <div class="dash-row__main">
                <button class="link dash-row__title" type="button" :title="n.title" @click="openNotice(n)">{{ n.title }}</button>
                <div class="dash-row__meta">{{ formatDate(pickNoticeTime(n)) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="dash-empty">暂无公告</div>
        </div>

        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">快捷入口</h3>
            <p class="dash-card__desc">常用功能</p>
          </div>
          <div class="dash-tiles">
            <button class="dash-tile" type="button" @click="go('/teacher/review/tasks')">
              <div class="dash-tile__title">待审核列表</div>
              <div class="dash-tile__desc">查看并处理审核任务</div>
            </button>
            <button class="dash-tile" type="button" @click="go('/teacher/feedback/mine')">
              <div class="dash-tile__title">我的反馈</div>
              <div class="dash-tile__desc">查看反馈处理进度</div>
            </button>
            <button class="dash-tile" type="button" @click="go('/teacher/ranking')">
              <div class="dash-tile__title">综合排名</div>
              <div class="dash-tile__desc">查看学期排名</div>
            </button>
            <button class="dash-tile" type="button" @click="go('/teacher/me/profile')">
              <div class="dash-tile__title">账号中心</div>
              <div class="dash-tile__desc">个人信息与密码</div>
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'
import { getRealName } from '../../utils/auth'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const notices = ref([])

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
    .slice(0, 6)
})

const go = (path, query = undefined) => {
  if (query) router.push({ path, query })
  else router.push(path)
}

const openTaskFromHome = (task) => {
  const id = task?.id
  if (!id) return
  router.push({ path: '/teacher/review/tasks', query: { open: String(id) } })
}

const pickNoticeTime = (notice) => notice?.published_at || notice?.updated_at || notice?.created_at || ''

const openNotice = (notice) => {
  const id = notice?.id
  if (!id) return
  router.push(`/teacher/notices/${id}`)
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

const loadNotices = async () => {
  const { data } = await http.get('/notices', { meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  notices.value = rows.slice(0, 3).map((item) => ({
    id: item?.id,
    title: item?.title || '-',
    published_at: item?.published_at,
    updated_at: item?.updated_at,
    created_at: item?.created_at
  }))
}

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadTasks(), loadNotices()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.dash-stack {
  display: grid;
  gap: var(--app-gap-4);
}
</style>
