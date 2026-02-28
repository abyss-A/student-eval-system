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
        <button class="btn" type="button" @click="save" :disabled="loading || !canEdit">保存</button>
      </div>
    </div>

    <p class="muted" style="margin-top: 10px;">
      每条活动最多上传 6 张 JPG/PNG 图片作为证明材料。审核后可查看每条活动的审核结论与审核理由。
    </p>
    <p v-if="!canEdit" class="muted" style="margin-top: 6px; color: #64748b;">
      当前测评单状态不可编辑，如需修改请联系管理员。
    </p>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th style="width: 180px;">活动标题</th>
          <th>说明</th>
          <th style="width: 110px;">分数</th>
          <th style="width: 320px;">证明材料</th>
          <th style="width: 110px;">审核结论</th>
          <th style="width: 200px;">审核理由</th>
          <th style="width: 90px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(a, idx) in rows" :key="a._rowKey || idx">
          <td>
            <input
              v-model.trim="a.title"
              placeholder="例如：主题团日"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <input
              v-model.trim="a.description"
              placeholder="可填写简要说明"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <input
              v-model.number="a.selfScore"
              type="number"
              min="0"
              step="0.5"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <ImageIdsUploader v-model="a.evidenceFileIds" :max="6" :hint="''" :readonly="!canEdit" />
          </td>
          <td>
            <span class="badge" :class="reviewResultBadge(activityReviewResult(a))">
              {{ activityReviewResult(a) }}
            </span>
          </td>
          <td>
            <div style="white-space: pre-wrap;">{{ displayReviewerComment(a.reviewerComment) }}</div>
          </td>
          <td>
            <button
              class="btn secondary"
              type="button"
              @click="removeRow(idx)"
              :disabled="loading || !canEdit"
            >
              删除
            </button>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td colspan="7" class="empty">暂无活动，请点击“新增”</td>
        </tr>
      </tbody>
    </table>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn secondary" type="button" @click="addRow" :disabled="loading || !canEdit">新增</button>
      <p class="muted">提示：保存时会自动忽略“完全空白”的行。</p>
    </div>

    <div class="formula-note">
      {{ moduleFormulaText }}
    </div>
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
const canEdit = computed(() => {
  const code = String(status.value || '').trim().toUpperCase()
  return code !== 'FINALIZED' && code !== 'PUBLISHED'
})

const moduleFormulaText = computed(() => {
  if (props.moduleType === 'MORAL') {
    return '德育计入分公式：德育计入分 = min(德育活动总分, 100) × 15%'
  }
  if (props.moduleType === 'INTEL_PRO_INNOV') {
    return '智育计入分公式：智育原始分 = 智育课程加权平均分 × 85% + min(智育活动总分, 100) × 15%；智育计入分 = 智育原始分 × 60%'
  }
  if (props.moduleType === 'SPORT_ACTIVITY') {
    return '体育计入分公式：体育原始分 = 体育课程加权平均分 × 85% + min(体育活动总分, 100) × 15%；体育计入分 = 体育原始分 × 10%'
  }
  if (props.moduleType === 'ART') {
    return '美育计入分公式：美育计入分 = min(美育活动总分, 100) × 7.5%'
  }
  if (props.moduleType === 'LABOR') {
    return '劳育计入分公式：劳育计入分 = min(劳育活动总分, 100) × 7.5%'
  }
  return ''
})

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const activityReviewResult = (a) => {
  const statusCode = String(a?.reviewStatus || '').trim().toUpperCase()
  if (statusCode === 'REJECTED') return '驳回'
  if (statusCode === 'APPROVED') return '通过'
  return '待审核'
}

const reviewResultBadge = (label) => {
  if (label === '通过') return 'success'
  if (label === '驳回') return 'danger'
  return ''
}

const isAutoReason = (text) => {
  const value = String(text || '').trim().toUpperCase()
  return value === '辅导员APPROVE' || value === '辅导员REJECT'
}

const displayReviewerComment = (text) => {
  if (!text || isAutoReason(text)) return '-'
  return text
}

const blankRow = () => ({
  moduleType: props.moduleType,
  title: '',
  description: '',
  selfScore: 0,
  reviewStatus: 'PENDING',
  reviewerComment: '',
  evidenceFileIds: '',
  _rowKey: `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const mapActivity = (a) => ({
  id: a?.id,
  moduleType: props.moduleType,
  title: a?.title || '',
  description: a?.description || '',
  selfScore: Number(a?.selfScore ?? 0),
  reviewStatus: a?.reviewStatus || 'PENDING',
  reviewerComment: a?.reviewerComment || '',
  evidenceFileIds: a?.evidenceFileIds || '',
  _rowKey: a?.id ? `act_${a.id}` : `tmp_${props.moduleType}_${Date.now()}_${Math.random()}`
})

const ensureEditable = () => {
  if (canEdit.value) return true
  alert('当前测评单状态不可编辑')
  return false
}

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    const all = store.state.detail?.activities || []
    rows.value = all
      .filter((x) => String(x.moduleType || '').toUpperCase() === props.moduleType)
      .map(mapActivity)
    if (!rows.value.length) rows.value.push(blankRow())
  } finally {
    loading.value = false
  }
}

const addRow = () => {
  if (!ensureEditable()) return
  rows.value.push(blankRow())
}

const removeRow = (idx) => {
  if (!ensureEditable()) return
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
      alert('活动分数必须是大于等于 0 的数字')
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
  if (!ensureEditable()) return
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

watch(
  () => props.moduleType,
  () => reload(),
  { immediate: true }
)
</script>

<style scoped>
.formula-note {
  margin-top: 10px;
  font-size: 13px;
  color: #475569;
}
</style>
