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
        <button class="btn" type="button" @click="save" :disabled="loading">保存课程</button>
      </div>
    </div>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th style="width: 220px;">课程名称</th>
          <th style="width: 140px;">类型</th>
          <th style="width: 120px;">成绩</th>
          <th style="width: 120px;">学分</th>
          <th style="width: 90px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(c, idx) in courses" :key="idx">
          <td><input v-model.trim="c.courseName" placeholder="例如：高等代数" /></td>
          <td>
            <select v-model="c.courseType">
              <option value="REQUIRED">必修</option>
              <option value="ELECTIVE">选修</option>
              <option value="RETAKE">重修</option>
              <option value="RELEARN">再修</option>
            </select>
          </td>
          <td><input v-model.number="c.score" type="number" min="0" step="0.5" placeholder="0-100" /></td>
          <td><input v-model.number="c.credit" type="number" min="0" step="0.5" placeholder="例如：2" /></td>
          <td>
            <button class="btn secondary" type="button" @click="removeRow(idx)">删除</button>
          </td>
        </tr>
        <tr v-if="!courses.length">
          <td colspan="5" class="empty">暂无课程，请点击“新增课程”</td>
        </tr>
      </tbody>
    </table>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn secondary" type="button" @click="addRow">新增课程</button>
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

const statusLabel = (raw) => {
  const code = String(raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
}

const mapCourse = (c) => ({
  courseName: c?.courseName || '',
  courseType: (c?.courseType || 'REQUIRED').toUpperCase(),
  score: c?.score ?? 0,
  credit: c?.credit ?? 0,
  evidenceFileId: c?.evidenceFileId ?? null
})

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
  courses.value.push(mapCourse({}))
}

const removeRow = (idx) => {
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
      alert('课程成绩必须是大于等于0的数字')
      return null
    }
    if (!Number.isFinite(credit) || credit < 0) {
      alert('课程学分必须是大于等于0的数字')
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

