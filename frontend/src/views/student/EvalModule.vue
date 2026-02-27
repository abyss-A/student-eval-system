<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">{{ label }}</h2>
        <p class="muted" style="margin-top: 6px;">
          测评单ID：<b>{{ submissionId || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          状态：<span class="badge">{{ statusLabel(status) }}</span>
        </p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" type="button" @click="reload" :disabled="loading">刷新</button>
        <button class="btn secondary" type="button" @click="addRow" :disabled="loading">新增活动</button>
        <button class="btn" type="button" @click="save" :disabled="loading">保存本模块</button>
      </div>
    </div>

    <p class="muted" style="margin-top: 10px;">每条活动最多 6 张 JPG/PNG 图片作为证明材料。</p>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th style="width: 200px;">活动标题</th>
          <th>说明</th>
          <th style="width: 120px;">自评分</th>
          <th style="width: 360px;">证明材料</th>
          <th style="width: 90px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(a, idx) in rows" :key="a._rowKey || idx">
          <td><input v-model.trim="a.title" placeholder="例如：主题班会" /></td>
          <td><input v-model.trim="a.description" placeholder="可填写简要说明" /></td>
          <td><input v-model.number="a.selfScore" type="number" min="0" step="0.5" /></td>
          <td>
            <ImageIdsUploader v-model="a.evidenceFileIds" :max="6" :hint="''" />
          </td>
          <td>
            <button class="btn secondary" type="button" @click="removeRow(idx)">删除</button>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td colspan="5" class="empty">暂无活动，请点击“新增活动”</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import submissionStore from '../../stores/submissionStore'
import ImageIdsUploader from '../../components/ImageIdsUploader.vue'

const props = defineProps({
  moduleType: { type: String, required: true },
  label: { type: String, required: true }
})

const store = submissionStore
const rows = ref([])
const loading = ref(false)

const submissionId = computed(() => store.state.submissionId)
const status = computed(() => store.state.status || store.state.detail?.submission?.status || '')

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const blankRow = () => ({
  moduleType: props.moduleType,
  title: '',
  description: '',
  selfScore: 0,
  evidenceFileIds: '',
  _rowKey: `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const mapActivity = (a) => ({
  id: a?.id,
  moduleType: props.moduleType,
  title: a?.title || '',
  description: a?.description || '',
  selfScore: Number(a?.selfScore ?? 0),
  evidenceFileIds: a?.evidenceFileIds || '',
  _rowKey: a?.id ? `act_${a.id}` : `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    const all = store.state.detail?.activities || []
    rows.value = all.filter((x) => String(x.moduleType || '').toUpperCase() === props.moduleType).map(mapActivity)
    if (!rows.value.length) rows.value.push(blankRow())
  } finally {
    loading.value = false
  }
}

const addRow = () => {
  rows.value.push(blankRow())
}

const removeRow = (idx) => {
  rows.value.splice(idx, 1)
  if (!rows.value.length) rows.value.push(blankRow())
}

const normalizeRows = () => {
  const out = []
  for (const a of rows.value) {
    const title = String(a.title || '').trim()
    const desc = String(a.description || '').trim()
    const score = Number(a.selfScore)
    const ev = String(a.evidenceFileIds || '').trim()

    const isBlank = !title && !desc && (!Number.isFinite(score) || score === 0) && !ev
    if (isBlank) continue

    if (!title) {
      alert('活动标题不能为空')
      return null
    }
    if (!Number.isFinite(score) || score < 0) {
      alert('自评分必须是大于等于0的数字')
      return null
    }

    out.push({
      id: a.id,
      moduleType: props.moduleType,
      title,
      description: desc,
      selfScore: score,
      evidenceFileIds: ev
    })
  }
  return out
}

const save = async () => {
  const items = normalizeRows()
  if (!items) return
  loading.value = true
  try {
    await store.saveActivitiesModule(props.moduleType, items)
    await reload()
    alert('本模块活动已保存')
  } finally {
    loading.value = false
  }
}

// Vue Router 会复用同一个组件实例（因为德育/美育/体育等都用的是同一个 EvalModule 组件）。
// 如果只在 mounted 时加载，会出现“切换页面仍显示上一个模块”的情况。
watch(
  () => props.moduleType,
  () => reload(),
  { immediate: true }
)
</script>
