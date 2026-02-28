<template>
  <section class="card submit-panel">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">综合成绩与提交</h2>
        <p class="muted" style="margin-top: 6px;">
          测评单ID：<b>{{ submissionId || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          状态：<span class="badge">{{ statusLabel(status) }}</span>
        </p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" type="button" @click="reload" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn" type="button" @click="submitForm" :disabled="loading">提交审核</button>
      <button class="btn ghost" type="button" @click="exportFile('DOCX')" :disabled="loading">导出Word</button>
      <button class="btn ghost" type="button" @click="exportFile('PDF')" :disabled="loading">导出PDF</button>
    </div>

    <div class="score-block" style="margin-top: 14px;">
      <h3 style="margin: 0;">分数预览（计入总分）</h3>
      <p class="muted" style="margin-top: 6px;">以下德智体美劳显示的是“已乘权重后”的计入分。</p>

      <div v-if="score" class="score-grid">
        <div class="score-item">
          <span class="name">德育</span>
          <b class="value">{{ moduleValue('moralScore', 'moralRaw') }}</b>
        </div>
        <div class="score-item">
          <span class="name">智育</span>
          <b class="value">{{ moduleValue('intelScore', 'intelRaw') }}</b>
        </div>
        <div class="score-item">
          <span class="name">体育</span>
          <b class="value">{{ moduleValue('sportScore', 'sportRaw') }}</b>
        </div>
        <div class="score-item">
          <span class="name">美育</span>
          <b class="value">{{ moduleValue('artScore', 'artRaw') }}</b>
        </div>
        <div class="score-item">
          <span class="name">劳育</span>
          <b class="value">{{ moduleValue('laborScore', 'laborRaw') }}</b>
        </div>
        <div class="score-item total">
          <span class="name">总分</span>
          <b class="value">{{ score.totalScore }}</b>
        </div>
      </div>
      <p v-else class="muted" style="margin-top: 10px;">暂无分数，请先填写课程与活动并保存。</p>

      <div v-if="score" class="raw-line">
        原始分：德育 {{ score.moralRaw }}，智育 {{ score.intelRaw }}，体育 {{ score.sportRaw }}，美育 {{ score.artRaw }}，劳育 {{ score.laborRaw }}
      </div>
    </div>

    <div class="formula-line">
      总分公式：综合总分 = 德育计入分 + 智育计入分 + 体育计入分 + 美育计入分 + 劳育计入分 = 德育原始分×15% + 智育原始分×60% + 体育原始分×10% + 美育原始分×7.5% + 劳育原始分×7.5%
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

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
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

const moduleValue = (weightedKey, rawKey) => {
  const s = score.value || {}
  const weighted = s[weightedKey]
  const raw = s[rawKey]
  return toDisplay(weighted !== null && weighted !== undefined ? weighted : raw)
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
  loading.value = true
  try {
    await store.submit()
    alert('已提交审核')
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
