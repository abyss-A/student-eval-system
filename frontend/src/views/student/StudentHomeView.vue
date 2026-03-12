<template>
  <section class="dash-page">
    <div class="card dash-hero dash-hero--student">
      <div class="dash-hero__top">
        <div>
          <p class="dash-hero__kicker">欢迎回来，<b>{{ realName || '同学' }}</b></p>
          <div class="dash-hero__headline">
            {{ semesterName ? `${semesterName}测评单` : '本学期测评单' }}
          </div>
          <div class="dash-hero__meta">
            <el-tag :type="statusTagType" effect="plain">{{ statusLabel(statusCode) }}</el-tag>
            <span class="dash-dot" aria-hidden="true" />
            <span class="muted">审核阶段：<b>{{ reviewPhaseLabel }}</b></span>
          </div>
        </div>

        <div class="dash-hero__actions">
          <el-button type="primary" @click="go('/student/eval/submit')">进入综合成绩</el-button>
          <el-button type="default" @click="go('/student/eval/course')">课程成绩</el-button>
        </div>
      </div>

      <div v-if="loading" class="dash-empty" style="padding: 12px 0;">加载中...</div>
      <div v-else class="dash-hero__bottom">
        <div class="dash-progress">
          <div class="dash-progress__head">
            <div class="dash-progress__title">审核进度</div>
            <div class="muted">{{ reviewDoneCount }}/{{ reviewTotalCount }}</div>
          </div>
          <div style="margin-top: 8px;">
            <el-progress :percentage="reviewProgressPercent" :stroke-width="10" :show-text="false" />
          </div>
          <div class="dash-progress__hint">{{ progressHint }}</div>
        </div>

        <div class="dash-metric">
          <div class="dash-metric__label">预览总分（口径：预览）</div>
          <div class="dash-metric__value">{{ toDisplay(previewTotalScore) }}</div>
          <div class="muted" style="font-size: 12px;">完整明细请进入“综合成绩”。</div>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="dash-cols">
      <div class="dash-col">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">待办提醒</h3>
            <p class="dash-card__desc">下一步建议</p>
          </div>
          <ul v-if="todoTips.length" class="todo-list">
            <li v-for="tip in todoTips" :key="tip">{{ tip }}</li>
          </ul>
          <p v-else class="muted" style="margin-top: 8px;">暂无待办。</p>
          <p v-if="openFeedbackCount > 0" class="muted todo-hint">未关闭反馈：<b>{{ openFeedbackCount }}</b></p>
          <div class="dash-hero__actions" style="margin-top: 12px;">
            <el-button type="default" @click="go('/student/feedback/mine')">我的反馈</el-button>
            <el-button type="default" @click="go('/student/feedback/create')">我要反馈</el-button>
          </div>
        </div>
      </div>

      <div class="dash-col">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">公告通知</h3>
            <el-button type="default" size="small" @click="go('/student/notices')">更多</el-button>
          </div>
          <div v-if="notices.length" class="dash-list">
            <div v-for="n in notices" :key="n.id" class="dash-row">
              <div class="dash-row__main">
                <button class="link dash-row__title" type="button" :title="n.title" @click="openNotice(n)">
                  {{ n.title }}
                </button>
                <div class="dash-row__meta">{{ formatDate(pickNoticeTime(n)) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="dash-empty">暂无公告</div>
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

const reviewProgressPercent = computed(() => {
  const total = reviewTotalCount.value
  if (total <= 0) return 0
  const done = reviewDoneCount.value
  const pct = Math.round((done / total) * 100)
  return Math.max(0, Math.min(100, pct))
})

const reviewPhaseLabel = computed(() => {
  const code = reviewPhaseCode.value
  if (code === 'NOT_REVIEWED') return '未审核'
  if (code === 'IN_PROGRESS') return '正在审核'
  if (code === 'DONE_NEED_STUDENT_FIX') return '待学生修改'
  if (code === 'DONE_ALL_PASS') return '全部通过'
  return code || '-'
})

const statusTagType = computed(() => {
  const code = statusCode.value
  if (code === 'DRAFT') return 'info'
  if (code === 'SUBMITTED') return 'warning'
  if (code === 'COUNSELOR_REVIEWED') return 'success'
  if (code === 'FINALIZED') return 'success'
  if (code === 'PUBLISHED') return 'success'
  return 'info'
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

const progressHint = computed(() => {
  const status = statusCode.value
  const phase = reviewPhaseCode.value
  if (status === 'DRAFT') return '当前为草稿：建议先填“课程成绩”，再补齐各模块活动。'
  if (status === 'SUBMITTED' && canStudentResubmit.value) return '存在驳回项：请修订后再次提交。'
  if (status === 'SUBMITTED' && (phase === 'NOT_REVIEWED' || phase === 'IN_PROGRESS')) return '辅导员审核中：你可查看审核进度与驳回原因。'
  if (status === 'COUNSELOR_REVIEWED') return '已提交管理员：可导出正式 Word 或查看历史排名。'
  if (status === 'FINALIZED' || status === 'PUBLISHED') return '历史测评单：可查看分数与排名结果。'
  return '进入“综合成绩”查看明细与操作。'
})

const statusLabel = (code) => {
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return code || '-'
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
.todo-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--app-text);
  line-height: 1.7;
}

.todo-hint {
  margin-top: 10px;
}
</style>
