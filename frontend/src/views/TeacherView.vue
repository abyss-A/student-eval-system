<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">辅导员审核</h2>
        <p class="muted" style="margin-top: 6px;">先选择待审核测评单，再逐项审核课程和活动。</p>
      </div>
      <button class="btn" @click="loadTasks" :disabled="loadingTasks">
        {{ loadingTasks ? '刷新中...' : '刷新待审' }}
      </button>
    </div>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th>学号</th>
          <th>学生</th>
          <th>班级</th>
          <th>总分</th>
          <th>提交时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>{{ task.student_no || '-' }}</td>
          <td>{{ task.real_name }}</td>
          <td>{{ task.class_name }}</td>
          <td>{{ task.total_score ?? '-' }}</td>
          <td>{{ formatDate(task.submitted_at) }}</td>
          <td>
            <button class="btn secondary" @click="openTask(task.id)" :disabled="loadingDetail">
              {{ selectedSubmissionId === task.id ? '已打开' : '打开审核' }}
            </button>
          </td>
        </tr>
        <tr v-if="!tasks.length">
          <td colspan="6" class="empty">暂无待审核测评单</td>
        </tr>
      </tbody>
    </table>
  </section>

  <div v-if="drawerOpen && current" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel drawer-wide">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">审核测评单 #{{ current.submission.id }}</div>
          <p class="muted" style="margin-top: 6px;">
            学号：<b>{{ current.student.studentNo || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            学生：<b>{{ current.student.realName }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            状态：<span class="badge">{{ current.submission.status }}</span>
          </p>
        </div>
        <div class="toolbar-row">
          <button class="btn secondary" type="button" @click="reloadCurrent" :disabled="loadingDetail">刷新详情</button>
          <button class="icon-btn" type="button" @click="closeDrawer" aria-label="关闭">X</button>
        </div>
      </div>

      <div class="drawer-body">
        <h4 class="section-title">课程审核</h4>
        <table class="table">
          <thead>
            <tr>
              <th class="nowrap">课程</th>
              <th class="nowrap">类型</th>
              <th class="nowrap">原分</th>
              <th>理由</th>
              <th class="nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="course in current.courses" :key="`course_${course.id}`">
              <td class="nowrap">{{ course.courseName }}</td>
              <td class="nowrap">{{ courseTypeLabel(course.courseType) }}</td>
              <td class="nowrap">{{ course.score }}</td>
              <td>
                <input v-model.trim="drafts[courseKey(course.id)].reason" placeholder="可填写审核理由（选填）" />
              </td>
              <td>
                <div class="action-row inline-actions">
                  <button class="btn" type="button" @click="decide('COURSE', course.id, 'APPROVE')" :disabled="isDeciding">通过</button>
                  <button class="btn danger" type="button" @click="decide('COURSE', course.id, 'REJECT')" :disabled="isDeciding">驳回</button>
                </div>
              </td>
            </tr>
            <tr v-if="!current.courses?.length">
              <td colspan="5" class="empty">暂无课程数据</td>
            </tr>
          </tbody>
        </table>

        <h4 class="section-title">活动审核</h4>
        <table class="table activity-table">
          <thead>
            <tr>
              <th class="nowrap col-module">模块</th>
              <th class="nowrap col-title">标题</th>
              <th class="nowrap col-score">分数</th>
              <th class="nowrap col-evidence">证明图片</th>
              <th class="nowrap col-reason">理由</th>
              <th class="nowrap col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="activity in current.activities" :key="`act_${activity.id}`">
              <td class="nowrap">{{ moduleLabel(activity.moduleType) }}</td>
              <td class="nowrap">{{ activity.title }}</td>
              <td class="nowrap">{{ activity.selfScore }}</td>
              <td>
                <div v-if="activity._evidenceMetas && activity._evidenceMetas.length" class="chip-list">
                  <span v-for="m in activity._evidenceMetas" :key="m.id" class="chip">
                    <button class="link" type="button" @click="previewEvidence(m.id)">{{ m.fileName || ('附件#' + m.id) }}</button>
                  </span>
                </div>
                <span v-else class="muted" style="font-size:12px;">未上传</span>
              </td>
              <td>
                <input v-model.trim="drafts[activityKey(activity.id)].reason" placeholder="可填写审核理由（选填）" />
              </td>
              <td>
                <div class="action-row inline-actions">
                  <button class="btn" type="button" @click="decide('ACTIVITY', activity.id, 'APPROVE')" :disabled="isDeciding">通过</button>
                  <button class="btn danger" type="button" @click="decide('ACTIVITY', activity.id, 'REJECT')" :disabled="isDeciding">驳回</button>
                </div>
              </td>
            </tr>
            <tr v-if="!current.activities?.length">
              <td colspan="6" class="empty">暂无活动数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="drawer-footer">
        <button class="btn secondary" type="button" @click="closeDrawer">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import http from '../api/http'
import { previewImageById } from '../utils/imagePreview'

const tasks = ref([])
const current = ref(null)
const selectedSubmissionId = ref(null)

const loadingTasks = ref(false)
const loadingDetail = ref(false)
const isDeciding = ref(false)
const drawerOpen = ref(false)

const drafts = reactive({})
const evidenceMetaCache = reactive({})

const courseKey = (id) => `COURSE_${id}`
const activityKey = (id) => `ACTIVITY_${id}`

const courseTypeLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'REQUIRED') return '必修'
  if (code === 'ELECTIVE') return '选修'
  if (code === 'RETAKE') return '重修'
  if (code === 'RELEARN') return '再修'
  return raw || '-'
}

const moduleLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'MORAL') return '德育'
  if (code === 'INTEL_PRO_INNOV') return '智育'
  if (code === 'SPORT_ACTIVITY') return '体育'
  if (code === 'ART') return '美育'
  if (code === 'LABOR') return '劳育'
  return raw || '-'
}

const isAutoReason = (text) => {
  const value = String(text || '').trim().toUpperCase()
  return value === '辅导员APPROVE'.toUpperCase() || value === '辅导员REJECT'.toUpperCase()
}

const normalizeReason = (text) => {
  if (!text || isAutoReason(text)) return ''
  return String(text).trim()
}

const ensureDraft = (key) => {
  if (!drafts[key]) {
    drafts[key] = {
      reason: ''
    }
  }
}

const initDrafts = (detail) => {
  ;(detail.courses || []).forEach((course) => {
    const key = courseKey(course.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(course.reviewerComment)
  })

  ;(detail.activities || []).forEach((activity) => {
    const key = activityKey(activity.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(activity.reviewerComment)
  })
}

const loadTasks = async () => {
  loadingTasks.value = true
  try {
    const { data } = await http.get('/reviews/tasks')
    tasks.value = data.data || []
  } finally {
    loadingTasks.value = false
  }
}

const openTask = async (submissionId) => {
  loadingDetail.value = true
  try {
    const { data } = await http.get(`/submissions/${submissionId}`)
    current.value = data.data
    selectedSubmissionId.value = submissionId
    initDrafts(data.data)
    await hydrateEvidenceMetas()
    drawerOpen.value = true
  } finally {
    loadingDetail.value = false
  }
}

const closeDrawer = () => {
  drawerOpen.value = false
}

const reloadCurrent = async () => {
  if (!selectedSubmissionId.value) return
  await openTask(selectedSubmissionId.value)
}

const decide = async (itemType, itemId, action) => {
  const key = itemType === 'COURSE' ? courseKey(itemId) : activityKey(itemId)
  ensureDraft(key)

  const payload = {
    action,
    reason: (drafts[key].reason || '').trim()
  }

  isDeciding.value = true
  try {
    await http.post(`/reviews/items/${itemType}/${itemId}/decision`, payload)
    await reloadCurrent()
    await loadTasks()
    alert('审核操作已提交')
  } finally {
    isDeciding.value = false
  }
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const parseEvidenceIds = (raw) => {
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => Number.isFinite(n) && n > 0)
}

const hydrateEvidenceMetas = async () => {
  if (!current.value?.activities) return

  const ids = []
  for (const a of current.value.activities) {
    ids.push(...parseEvidenceIds(a.evidenceFileIds))
  }
  const uniqueIds = Array.from(new Set(ids))
  if (!uniqueIds.length) {
    for (const a of current.value.activities) {
      a._evidenceMetas = []
    }
    return
  }

  const { data } = await http.post('/files/metas', { ids: uniqueIds })
  const metas = data.data || []
  const map = {}
  for (const m of metas) {
    map[m.id] = m
    evidenceMetaCache[m.id] = m
  }

  for (const a of current.value.activities) {
    const aIds = parseEvidenceIds(a.evidenceFileIds)
    a._evidenceMetas = aIds.map((id) => map[id] || evidenceMetaCache[id] || { id, fileName: `附件#${id}` })
  }
}

const previewEvidence = async (fileId) => {
  await previewImageById(http, fileId, '证明材料预览')
}

loadTasks()
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}

.inline-actions {
  flex-wrap: nowrap;
  align-items: center;
}

.inline-actions .btn {
  flex: 0 0 auto;
  min-width: 84px;
  white-space: nowrap;
  padding-left: 14px;
  padding-right: 14px;
}

.activity-table .col-module {
  width: 90px;
}

.activity-table .col-title {
  width: 150px;
}

.activity-table .col-score {
  width: 90px;
}

.activity-table .col-evidence {
  width: 280px;
}

.activity-table .col-reason {
  width: 220px;
}

.activity-table .col-action {
  width: 210px;
}
</style>
