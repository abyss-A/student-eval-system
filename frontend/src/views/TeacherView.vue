<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2>辅导员审核</h2>
        <p class="muted">先选择待审测评单，再逐项审核课程和活动。</p>
      </div>
      <button class="btn" @click="loadTasks" :disabled="loadingTasks">
        {{ loadingTasks ? '刷新中...' : '刷新待审' }}
      </button>
    </div>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th>测评单ID</th>
          <th>学生</th>
          <th>班级</th>
          <th>总分</th>
          <th>提交时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>{{ task.id }}</td>
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
          <td colspan="6" class="empty">暂无待审测评单</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section class="card" style="margin-top: 16px;" v-if="current">
    <div class="toolbar">
      <div>
        <h3>审核测评单 #{{ current.submission.id }}</h3>
        <p class="muted">
          学生：{{ current.student.realName }}（{{ current.student.studentNo }}）
          ｜状态：<span class="badge">{{ current.submission.status }}</span>
        </p>
      </div>
      <button class="btn secondary" @click="reloadCurrent" :disabled="loadingDetail">刷新详情</button>
    </div>

    <h4 class="section-title">课程审核</h4>
    <table class="table">
      <thead>
        <tr>
          <th>课程</th>
          <th>类型</th>
          <th>原分</th>
          <th>调整分</th>
          <th>理由</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="course in current.courses" :key="`course_${course.id}`">
          <td>{{ course.courseName }}</td>
          <td>{{ course.courseType }}</td>
          <td>{{ course.score }}</td>
          <td>
            <input type="number" v-model.number="drafts[courseKey(course.id)].adjustedScore" min="0" step="0.5" />
          </td>
          <td>
            <input v-model.trim="drafts[courseKey(course.id)].reason" placeholder="可填写审核理由" />
          </td>
          <td>
            <div class="action-row">
              <button class="btn" @click="decide('COURSE', course.id, 'APPROVE')" :disabled="isDeciding">通过</button>
              <button class="btn secondary" @click="decide('COURSE', course.id, 'ADJUST')" :disabled="isDeciding">改分</button>
              <button class="btn danger" @click="decide('COURSE', course.id, 'REJECT')" :disabled="isDeciding">驳回</button>
            </div>
          </td>
        </tr>
        <tr v-if="!current.courses?.length">
          <td colspan="6" class="empty">暂无课程数据</td>
        </tr>
      </tbody>
    </table>

    <h4 class="section-title">活动审核</h4>
    <table class="table">
      <thead>
        <tr>
          <th>模块</th>
          <th>标题</th>
          <th>原分</th>
          <th>调整分</th>
          <th>理由</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="activity in current.activities" :key="`act_${activity.id}`">
          <td>{{ activity.moduleType }}</td>
          <td>{{ activity.title }}</td>
          <td>{{ activity.selfScore }}</td>
          <td>
            <input type="number" v-model.number="drafts[activityKey(activity.id)].adjustedScore" min="0" step="0.5" />
          </td>
          <td>
            <input v-model.trim="drafts[activityKey(activity.id)].reason" placeholder="可填写审核理由" />
          </td>
          <td>
            <div class="action-row">
              <button class="btn" @click="decide('ACTIVITY', activity.id, 'APPROVE')" :disabled="isDeciding">通过</button>
              <button class="btn secondary" @click="decide('ACTIVITY', activity.id, 'ADJUST')" :disabled="isDeciding">改分</button>
              <button class="btn danger" @click="decide('ACTIVITY', activity.id, 'REJECT')" :disabled="isDeciding">驳回</button>
            </div>
          </td>
        </tr>
        <tr v-if="!current.activities?.length">
          <td colspan="6" class="empty">暂无活动数据</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import http from '../api/http'

const tasks = ref([])
const current = ref(null)
const selectedSubmissionId = ref(null)

const loadingTasks = ref(false)
const loadingDetail = ref(false)
const isDeciding = ref(false)

const drafts = reactive({})

const courseKey = (id) => `COURSE_${id}`
const activityKey = (id) => `ACTIVITY_${id}`

const ensureDraft = (key) => {
  if (!drafts[key]) {
    drafts[key] = {
      adjustedScore: null,
      reason: ''
    }
  }
}

const initDrafts = (detail) => {
  ;(detail.courses || []).forEach((course) => {
    const key = courseKey(course.id)
    ensureDraft(key)
    drafts[key].adjustedScore = Number(course.score)
  })

  ;(detail.activities || []).forEach((activity) => {
    const key = activityKey(activity.id)
    ensureDraft(key)
    drafts[key].adjustedScore = Number(activity.selfScore)
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
  } finally {
    loadingDetail.value = false
  }
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
    reason: drafts[key].reason || `辅导员${action}`
  }

  if (action === 'ADJUST') {
    const adjusted = Number(drafts[key].adjustedScore)
    if (Number.isNaN(adjusted)) {
      alert('改分时请填写有效分数')
      return
    }
    payload.adjustedScore = adjusted
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

loadTasks()
</script>