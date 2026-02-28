<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">课程成绩</h2>
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
      说明：辅导员审核后，可在本页查看每条课程的审核结论和审核理由。
    </p>
    <p v-if="!canEdit" class="muted" style="margin-top: 6px; color: #64748b;">
      当前测评单状态不可编辑，如需修改请联系管理员。
    </p>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th style="width: 200px;">课程名称</th>
          <th style="width: 110px;">类型</th>
          <th style="width: 110px;">分数</th>
          <th style="width: 90px;">学分</th>
          <th style="width: 110px;">审核结论</th>
          <th>审核理由</th>
          <th style="width: 90px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(c, idx) in courses" :key="c.id || idx">
          <td>
            <input
              v-model.trim="c.courseName"
              placeholder="例如：高等代数"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <select v-model="c.courseType" :disabled="loading || !canEdit">
              <option value="REQUIRED">必修</option>
              <option value="ELECTIVE">选修</option>
              <option value="RETAKE">重修</option>
              <option value="RELEARN">再修</option>
            </select>
          </td>
          <td>
            <input
              v-model.number="c.score"
              type="number"
              min="0"
              step="0.5"
              placeholder="0-100"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <input
              v-model.number="c.credit"
              type="number"
              min="0"
              step="0.5"
              placeholder="例如：3"
              :disabled="loading || !canEdit"
            />
          </td>
          <td>
            <span class="badge" :class="reviewResultBadge(courseReviewResult(c))">
              {{ courseReviewResult(c) }}
            </span>
          </td>
          <td>
            <div style="white-space: pre-wrap;">{{ displayReviewerComment(c.reviewerComment) }}</div>
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
        <tr v-if="!courses.length">
          <td colspan="7" class="empty">暂无课程，请点击“新增”</td>
        </tr>
      </tbody>
    </table>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn secondary" type="button" @click="addRow" :disabled="loading || !canEdit">新增</button>
      <p class="muted">提示：保存时会自动忽略“完全空白”的行。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import submissionStore from '../../stores/submissionStore'

const store = submissionStore
const courses = ref([])
const loading = ref(false)

const submissionId = computed(() => store.state.submissionId)
const status = computed(() => store.state.status || store.state.detail?.submission?.status || '')
const canEdit = computed(() => {
  const code = String(status.value || '').trim().toUpperCase()
  return code !== 'FINALIZED' && code !== 'PUBLISHED'
})

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const courseReviewResult = (c) => {
  const statusCode = String(c?.reviewStatus || '').trim().toUpperCase()
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

const mapCourse = (c) => ({
  id: c?.id,
  courseName: c?.courseName || '',
  courseType: (c?.courseType || 'REQUIRED').toUpperCase(),
  score: c?.score ?? 0,
  credit: c?.credit ?? 0,
  evidenceFileId: c?.evidenceFileId ?? null,
  reviewStatus: c?.reviewStatus || 'PENDING',
  reviewerComment: c?.reviewerComment || ''
})

const ensureEditable = () => {
  if (canEdit.value) return true
  alert('当前测评单状态不可编辑')
  return false
}

const ensureAtLeastOneRow = () => {
  if (!courses.value.length) {
    courses.value.push(mapCourse({}))
  }
}

const reload = async () => {
  loading.value = true
  try {
    await store.ensureSubmission()
    await store.loadDetail()
    courses.value = (store.state.detail?.courses || []).map(mapCourse)
    ensureAtLeastOneRow()
  } finally {
    loading.value = false
  }
}

const addRow = () => {
  if (!ensureEditable()) return
  courses.value.push(mapCourse({}))
}

const removeRow = (idx) => {
  if (!ensureEditable()) return
  courses.value.splice(idx, 1)
  ensureAtLeastOneRow()
}

const normalizeCourses = () => {
  const out = []
  for (const c of courses.value) {
    const name = String(c.courseName || '').trim()
    const type = String(c.courseType || 'REQUIRED').trim()
    const score = Number(c.score)
    const credit = Number(c.credit)

    const isBlankRow = !name && !Number.isFinite(score) && !Number.isFinite(credit)
    if (isBlankRow) continue

    if (!name) {
      alert('课程名称不能为空')
      return null
    }
    if (!type) {
      alert('课程类型不能为空')
      return null
    }
    if (!Number.isFinite(score) || score < 0) {
      alert('课程分数必须是大于等于 0 的数字')
      return null
    }
    if (!Number.isFinite(credit) || credit < 0) {
      alert('课程学分必须是大于等于 0 的数字')
      return null
    }

    out.push({
      courseName: name,
      courseType: type,
      score,
      credit,
      evidenceFileId: c.evidenceFileId || null
    })
  }
  return out
}

const save = async () => {
  if (!ensureEditable()) return
  const items = normalizeCourses()
  if (!items) return
  loading.value = true
  try {
    await store.saveCourses(items)
    await reload()
    alert('课程已保存')
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>
