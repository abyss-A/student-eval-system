<template>
  <section class="grid two">
    <article class="card">
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

      <p class="muted" style="margin-top: 10px;">
        公式：{{ score?.formula || 'MORAL*15% + INTEL*60% + SPORT*10% + ART*7.5% + LABOR*7.5%' }}
      </p>
    </article>

    <article class="card">
      <h3 style="margin: 0;">分数预览</h3>
      <div v-if="score" class="grid two" style="margin-top: 12px;">
        <div>德育：<b>{{ score.moralRaw }}</b></div>
        <div>智育：<b>{{ score.intelRaw }}</b></div>
        <div>体育：<b>{{ score.sportRaw }}</b></div>
        <div>美育：<b>{{ score.artRaw }}</b></div>
        <div>劳动：<b>{{ score.laborRaw }}</b></div>
        <div>总分：<b>{{ score.totalScore }}</b></div>
      </div>
      <p v-else class="muted" style="margin-top: 12px;">暂无分数，请先填写课程与活动。</p>
    </article>
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

