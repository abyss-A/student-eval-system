<template>
  <section class="card submit-panel">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">
          测评单ID：<b>{{ submissionId || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          状态：<span class="badge">{{ statusLabel(status) }}</span>
        </p>
      </div>
    </div>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn" type="button" @click="submitForm" :disabled="loading || !canSubmit">{{ submitButtonLabel }}</button>
      <button class="btn ghost" type="button" @click="exportFile('DOCX')" :disabled="loading">导出Word</button>
    </div>
    <p v-if="submitDisabledHint" class="muted" style="margin-top: 8px; color: #64748b;">{{ submitDisabledHint }}</p>

    <div class="score-block" style="margin-top: 14px;">
      <h3 style="margin: 0;">预览分数</h3>
      <p class="muted" style="margin-top: 6px;">以下德智体美劳显示的是“已乘权重后”的计入分。</p>

      <div v-if="previewScore" class="score-grid">
        <div class="score-item">
          <span class="name">德育</span>
          <b class="value">{{ toDisplay(previewScore.moralScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">智育</span>
          <b class="value">{{ toDisplay(previewScore.intelScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">体育</span>
          <b class="value">{{ toDisplay(previewScore.sportScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">美育</span>
          <b class="value">{{ toDisplay(previewScore.artScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">劳育</span>
          <b class="value">{{ toDisplay(previewScore.laborScore) }}</b>
        </div>
        <div class="score-item total">
          <span class="name">总分</span>
          <b class="value">{{ toDisplay(previewScore.totalScore) }}</b>
        </div>
      </div>
      <p v-else class="muted" style="margin-top: 10px;">暂无分数，请先填写课程与活动并保存。</p>

      <div v-if="previewScore" class="raw-line">
        原始分：德育 {{ toDisplay(previewScore.moralRaw) }}，智育 {{ toDisplay(previewScore.intelRaw) }}，体育 {{ toDisplay(previewScore.sportRaw) }}，美育 {{ toDisplay(previewScore.artRaw) }}，劳育 {{ toDisplay(previewScore.laborRaw) }}
      </div>
    </div>

    <div class="score-block" style="margin-top: 12px;">
      <h3 style="margin: 0;">审核分数</h3>
      <p class="muted" style="margin-top: 6px;">仅在辅导员完成整单审核后展示。</p>

      <div v-if="reviewReady && reviewedScore" class="score-grid">
        <div class="score-item">
          <span class="name">德育</span>
          <b class="value">{{ toDisplay(reviewedScore.moralScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">智育</span>
          <b class="value">{{ toDisplay(reviewedScore.intelScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">体育</span>
          <b class="value">{{ toDisplay(reviewedScore.sportScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">美育</span>
          <b class="value">{{ toDisplay(reviewedScore.artScore) }}</b>
        </div>
        <div class="score-item">
          <span class="name">劳育</span>
          <b class="value">{{ toDisplay(reviewedScore.laborScore) }}</b>
        </div>
        <div class="score-item total">
          <span class="name">总分</span>
          <b class="value">{{ toDisplay(reviewedScore.totalScore) }}</b>
        </div>
      </div>
      <p v-else class="muted" style="margin-top: 10px;">辅导员尚未完成审核，审核分数暂未开放。</p>
    </div>

    <div class="formula-line">
      总分公式：综合总分 = 德育计入分 + 智育计入分 + 体育计入分 + 美育计入分 + 劳育计入分
      <br />
      体育口径：体育原始分 = 大学体育分 × 85% + min(体育活动总分, 100) × 15%
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import submissionStore from '../../stores/submissionStore'

const store = submissionStore
const loading = ref(false)

const submissionId = computed(() => store.state.submissionId)
const status = computed(() => store.state.status || store.state.detail?.submission?.status || '')
const score = computed(() => store.state.score)
const statusCode = computed(() => String(status.value || '').trim().toUpperCase())
const reviewPhase = computed(() => String(score.value?.reviewPhase || '').trim().toUpperCase())
const canStudentResubmit = computed(() => Boolean(score.value?.canStudentResubmit))
const canSubmit = computed(() => {
  if (statusCode.value === 'DRAFT') return true
  if (statusCode.value === 'SUBMITTED') {
    return canStudentResubmit.value || reviewPhase.value === 'DONE_NEED_STUDENT_FIX'
  }
  return false
})
const submitButtonLabel = computed(() => {
  if (statusCode.value === 'SUBMITTED' && (canStudentResubmit.value || reviewPhase.value === 'DONE_NEED_STUDENT_FIX')) {
    return '再次提交'
  }
  return '提交审核'
})
const submitDisabledHint = computed(() => {
  if (canSubmit.value) return ''
  if (statusCode.value === 'SUBMITTED') {
    if (reviewPhase.value === 'DONE_ALL_PASS') return '已全部通过，无需再次提交。'
    if (reviewPhase.value === 'NOT_REVIEWED' || reviewPhase.value === 'IN_PROGRESS') return '辅导员审核中，暂不可再次提交。'
    return '当前阶段不可提交。'
  }
  if (statusCode.value === 'COUNSELOR_REVIEWED') return '测评单已提交管理员，当前不可提交。'
  if (statusCode.value === 'FINALIZED') return '测评单已终审，当前不可提交。'
  if (statusCode.value === 'PUBLISHED') return '测评单已公示，当前不可提交。'
  return ''
})
const reviewReady = computed(() => Boolean(score.value?.reviewReady))

const pickScoreValue = (source, prefixedKey, fallbackKey) => {
  if (!source) return null
  if (source[prefixedKey] !== undefined && source[prefixedKey] !== null) return source[prefixedKey]
  if (fallbackKey && source[fallbackKey] !== undefined && source[fallbackKey] !== null) return source[fallbackKey]
  return null
}

const previewScore = computed(() => {
  const s = score.value
  if (!s) return null
  return {
    moralRaw: pickScoreValue(s, 'previewMoralRaw', 'moralRaw'),
    intelRaw: pickScoreValue(s, 'previewIntelRaw', 'intelRaw'),
    sportRaw: pickScoreValue(s, 'previewSportRaw', 'sportRaw'),
    artRaw: pickScoreValue(s, 'previewArtRaw', 'artRaw'),
    laborRaw: pickScoreValue(s, 'previewLaborRaw', 'laborRaw'),
    moralScore: pickScoreValue(s, 'previewMoralScore', 'moralScore'),
    intelScore: pickScoreValue(s, 'previewIntelScore', 'intelScore'),
    sportScore: pickScoreValue(s, 'previewSportScore', 'sportScore'),
    artScore: pickScoreValue(s, 'previewArtScore', 'artScore'),
    laborScore: pickScoreValue(s, 'previewLaborScore', 'laborScore'),
    totalScore: pickScoreValue(s, 'previewTotalScore', 'totalScore')
  }
})

const reviewedScore = computed(() => {
  const s = score.value
  if (!s) return null
  return {
    moralRaw: pickScoreValue(s, 'reviewedMoralRaw', null),
    intelRaw: pickScoreValue(s, 'reviewedIntelRaw', null),
    sportRaw: pickScoreValue(s, 'reviewedSportRaw', null),
    artRaw: pickScoreValue(s, 'reviewedArtRaw', null),
    laborRaw: pickScoreValue(s, 'reviewedLaborRaw', null),
    moralScore: pickScoreValue(s, 'reviewedMoralScore', null),
    intelScore: pickScoreValue(s, 'reviewedIntelScore', null),
    sportScore: pickScoreValue(s, 'reviewedSportScore', null),
    artScore: pickScoreValue(s, 'reviewedArtScore', null),
    laborScore: pickScoreValue(s, 'reviewedLaborScore', null),
    totalScore: pickScoreValue(s, 'reviewedTotalScore', null)
  }
})

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'COUNSELOR_REVIEWED') return '已提交管理员'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const toDisplay = (v) => {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v)
  return n.toFixed(2)
}

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    await store.loadScore()
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  if (!canSubmit.value) return
  const actionLabel = submitButtonLabel.value
  loading.value = true
  try {
    await store.flushAutoSaveFlushers()
    await store.submit()
    if (actionLabel === '再次提交') {
      alert('已再次提交')
    } else {
      alert('已提交审核')
    }
  } catch (e) {
    const msg = e?.userMessage || e?.message || '保存失败，请点击保存后重试提交'
    alert(msg)
  } finally {
    loading.value = false
  }
}

const exportFile = async (format) => {
  loading.value = true
  try {
    await store.exportReport(format)
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.submit-panel {
  display: grid;
  gap: 0;
}

.score-block {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px;
  background: #f8fafc;
}

.score-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.score-item {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-item .name {
  color: #475569;
  font-size: 13px;
}

.score-item .value {
  font-size: 22px;
  color: #0f172a;
}

.score-item.total {
  background: #eef6ff;
  border-color: #bfd9ff;
}

.raw-line {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
}

.formula-line {
  margin-top: 12px;
  font-size: 13px;
  color: #475569;
}

@media (max-width: 960px) {
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>

