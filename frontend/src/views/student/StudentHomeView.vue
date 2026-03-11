<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">
          欢迎回来，<b>{{ realName || '同学' }}</b>
        </p>
      </div>
      <div class="toolbar-row">
        <el-tag v-if="semesterName" type="info" effect="plain">{{ semesterName }}</el-tag>
      </div>
    </div>

    <div v-if="loading" class="empty" style="margin-top: 12px;">加载中...</div>
    <div v-else class="grid two workbench-grid" style="margin-top: 12px;">
      <div class="card workbench-card">
        <h3 class="workbench-card-title">本学期测评单</h3>
        <p class="muted workbench-meta">
          状态：
          <span class="badge" :class="statusBadge(statusCode)">{{ statusLabel(statusCode) }}</span>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          审核阶段：<b>{{ reviewPhaseLabel }}</b>
        </p>

        <div class="workbench-metrics">
          <div class="workbench-metric">
            <div class="muted">预览总分</div>
            <div class="workbench-metric-value">{{ toDisplay(previewTotalScore) }}</div>
          </div>
          <div class="workbench-metric">
            <div class="muted">审核进度</div>
            <div class="workbench-metric-value">{{ reviewDoneCount }}/{{ reviewTotalCount }}</div>
          </div>
        </div>

        <div class="action-row" style="margin-top: 12px;">
          <el-button type="primary" @click="go('/student/eval/submit')">进入综合成绩</el-button>
          <el-button type="default" @click="go('/student/eval/course')">课程成绩</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">待办提醒</h3>
        <ul v-if="todoTips.length" class="workbench-list">
          <li v-for="tip in todoTips" :key="tip">{{ tip }}</li>
        </ul>
        <p v-else class="muted" style="margin-top: 8px;">暂无待办。</p>

        <p v-if="openFeedbackCount > 0" class="muted workbench-inline-hint">
          未关闭反馈：<b>{{ openFeedbackCount }}</b>
        </p>

        <div class="action-row" style="margin-top: 12px;">
          <el-button type="default" @click="go('/student/feedback/mine')">我的反馈</el-button>
          <el-button type="default" @click="go('/student/feedback/create')">我要反馈</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">公告通知</h3>

        <div v-if="notices.length" class="workbench-notice-list">
          <div v-for="n in notices" :key="n.id" class="workbench-notice-row">
            <button class="link workbench-notice-title" type="button" :title="n.title" @click="openNotice(n)">
              {{ n.title }}
            </button>
            <span class="muted">{{ formatDate(pickNoticeTime(n)) }}</span>
          </div>
        </div>
        <div v-else class="empty" style="padding: 10px 0;">暂无公告</div>

        <div class="action-row" style="margin-top: 12px;">
          <el-button type="default" @click="go('/student/notices')">更多公告</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">快捷入口</h3>
        <div class="workbench-actions">
          <el-button type="default" @click="go('/student/eval/moral')">德育</el-button>
          <el-button type="default" @click="go('/student/eval/intel')">智育</el-button>
          <el-button type="default" @click="go('/student/eval/sport')">体育</el-button>
          <el-button type="default" @click="go('/student/eval/art')">美育</el-button>
          <el-button type="default" @click="go('/student/eval/labor')">劳育</el-button>
          <el-button type="default" @click="go('/student/ranking')">综合排名</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'
import submissionStore from '../../stores/submissionStore'
import { getRealName } from '../../utils/auth'

const router = useRouter()
const store = submissionStore

const loading = ref(false)
const notices = ref([])
const openFeedbackCount = ref(0)

const realName = computed(() => getRealName())
const semesterName = computed(() => store.state.detail?.semester?.name || '')
const statusCode = computed(() => String(store.state.status || store.state.detail?.submission?.status || '').trim().toUpperCase())
const score = computed(() => store.state.score || {})
const reviewPhaseCode = computed(() => String(score.value?.reviewPhase || '').trim().toUpperCase())

const pickScoreValue = (source, prefixedKey, fallbackKey) => {
  if (!source) return null
  if (source[prefixedKey] !== undefined && source[prefixedKey] !== null) return source[prefixedKey]
  if (fallbackKey && source[fallbackKey] !== undefined && source[fallbackKey] !== null) return source[fallbackKey]
  return null
}

const previewTotalScore = computed(() => Number(pickScoreValue(score.value, 'previewTotalScore', 'totalScore') || 0))
const reviewTotalCount = computed(() => Number(score.value?.reviewTotalCount || 0))
const reviewDoneCount = computed(() => Number(score.value?.reviewDoneCount || 0))
const canStudentResubmit = computed(() => Boolean(score.value?.canStudentResubmit))

const reviewPhaseLabel = computed(() => {
  const code = reviewPhaseCode.value
  if (code === 'NOT_REVIEWED') return '未审核'
  if (code === 'IN_PROGRESS') return '正在审核'
  if (code === 'DONE_NEED_STUDENT_FIX') return '待学生修改'
  if (code === 'DONE_ALL_PASS') return '全部通过'
  return code || '-'
})

const todoTips = computed(() => {
  const status = statusCode.value
  const phase = reviewPhaseCode.value
  const tips = []

  if (status === 'DRAFT') {
    tips.push('当前为草稿状态：请先填写课程与活动成绩，完成后提交辅导员审核。')
    tips.push('建议先从“课程成绩”开始，再补齐德智体美劳活动。')
    return tips.slice(0, 3)
  }

  if (status === 'SUBMITTED') {
    if (canStudentResubmit.value) {
      tips.push('辅导员已驳回部分条目：请根据驳回原因修改后再次提交。')
      tips.push('可前往“综合成绩”查看整单审核状态与提交按钮。')
      return tips.slice(0, 3)
    }

    if (phase === 'NOT_REVIEWED') tips.push('已提交：辅导员尚未开始审核，请耐心等待。')
    else if (phase === 'IN_PROGRESS') tips.push('已提交：辅导员正在审核中。')
    else tips.push('已提交：当前处于审核流程中。')
    return tips.slice(0, 3)
  }

  if (status === 'COUNSELOR_REVIEWED') {
    tips.push('辅导员已提交管理员：你现在可以导出正式 Word。')
    tips.push('如需查看分数明细，可进入“综合成绩”。')
    return tips.slice(0, 3)
  }

  if (status === 'FINALIZED') {
    tips.push('测评单已终审：可查看历史分数与排名。')
    return tips.slice(0, 3)
  }

  if (status === 'PUBLISHED') {
    tips.push('测评单已公示：可查看排名结果。')
    return tips.slice(0, 3)
  }

  return tips.slice(0, 3)
})

const statusLabel = (code) => {
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return code || '-'
}

const statusBadge = (code) => {
  if (code === 'DRAFT') return ''
  if (code === 'SUBMITTED') return 'warning'
  if (code === 'COUNSELOR_REVIEWED') return 'success'
  if (code === 'FINALIZED') return 'success'
  if (code === 'PUBLISHED') return 'success'
  return ''
}

const toDisplay = (v) => {
  const n = Number(v || 0)
  if (!Number.isFinite(n)) return String(v ?? '')
  return n.toFixed(2)
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

const pickNoticeTime = (notice) => notice?.published_at || notice?.updated_at || notice?.created_at || ''

const openNotice = (notice) => {
  const id = notice?.id
  if (!id) return
  router.push(`/student/notices/${id}`)
}

const go = (path) => {
  router.push(path)
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

const loadMyFeedbackCount = async () => {
  const { data } = await http.get('/feedbacks/my', { meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  openFeedbackCount.value = rows.filter((item) => String(item?.status || '').trim().toUpperCase() !== 'CLOSED').length
}

const load = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await Promise.all([loadNotices(), loadMyFeedbackCount()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.workbench-grid .workbench-card {
  box-shadow: none;
  background: #fbfdff;
  border-color: #e5e7eb;
}

.workbench-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.workbench-meta {
  margin-top: 8px;
  line-height: 1.7;
}

.workbench-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.workbench-metric {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px 12px;
  background: #ffffff;
}

.workbench-metric-value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.workbench-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #0f172a;
  line-height: 1.7;
}

.workbench-inline-hint {
  margin-top: 10px;
}

.workbench-notice-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.workbench-notice-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.workbench-notice-title {
  font-size: 13px;
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.workbench-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>

