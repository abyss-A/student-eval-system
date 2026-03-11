<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">
          欢迎回来，<b>{{ realName || '管理员' }}</b>
        </p>
      </div>
      <div class="toolbar-row">
        <el-tag type="info" effect="plain">管理工作台</el-tag>
      </div>
    </div>

    <div v-if="loading" class="empty" style="margin-top: 12px;">加载中...</div>
    <div v-else class="grid two workbench-grid" style="margin-top: 12px;">
      <div class="card workbench-card">
        <h3 class="workbench-card-title">当前学期概览</h3>
        <p class="muted workbench-meta">
          当前学期：<b>{{ activeSemesterName || '-' }}</b>
        </p>

        <div class="workbench-stat-grid">
          <div class="workbench-stat">
            <div class="muted">待审核（SUBMITTED）</div>
            <div class="workbench-stat-value">{{ submittedPendingCount }}</div>
          </div>
          <div class="workbench-stat">
            <div class="muted">反馈待处理</div>
            <div class="workbench-stat-value">{{ feedbackNewCount }}</div>
          </div>
        </div>

        <div class="action-row" style="margin-top: 12px;">
          <el-button type="primary" @click="go('/admin/submissions')">测评单查看</el-button>
          <el-button type="default" @click="go('/admin/semesters')">学期管理</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">反馈待处理</h3>
        <p class="muted workbench-meta">
          当前共有 <b>{{ feedbackNewCount }}</b> 条待处理反馈（NEW）。
        </p>
        <div class="action-row" style="margin-top: 12px;">
          <el-button type="primary" @click="go('/admin/feedback/handle')">进入反馈处理</el-button>
          <el-button type="default" @click="go('/admin/notices')">公告管理</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">公告通知</h3>

        <div v-if="notices.length" class="workbench-notice-list">
          <div v-for="n in notices" :key="n.id" class="workbench-notice-row">
            <button class="link workbench-notice-title" type="button" :title="n.title" @click="openNotice(n)">
              {{ n.title }}
            </button>
            <span class="muted">{{ formatDate(pickNoticeTime(n)) }}</span>
          </div>
        </div>
        <div v-else class="empty" style="padding: 10px 0;">暂无公告</div>

        <div class="action-row" style="margin-top: 12px;">
          <el-button type="default" @click="go('/admin/notices')">更多公告</el-button>
        </div>
      </div>

      <div class="card workbench-card">
        <h3 class="workbench-card-title">快捷入口</h3>
        <div class="workbench-actions">
          <el-button type="default" @click="go('/admin/accounts')">账号管理</el-button>
          <el-button type="default" @click="go('/admin/counselor/scopes')">班级权限</el-button>
          <el-button type="default" @click="go('/admin/feedback/handle')">反馈处理</el-button>
          <el-button type="default" @click="go('/admin/ranking')">综合排名</el-button>
          <el-button type="default" @click="go('/admin/me/profile')">账号中心</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'
import { getRealName } from '../../utils/auth'

const router = useRouter()
const loading = ref(false)

const activeSemesterName = ref('')
const submittedPendingCount = ref(0)
const feedbackNewCount = ref(0)
const notices = ref([])

const realName = computed(() => getRealName())

const go = (path) => {
  router.push(path)
}

const pickNoticeTime = (notice) => notice?.published_at || notice?.updated_at || notice?.created_at || ''

const openNotice = (notice) => {
  const id = notice?.id
  if (!id) return
  router.push(`/admin/notices/${id}`)
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

const loadSemesterOverview = async () => {
  const { data } = await http.get('/admin/semesters', { meta: { silent: true } })
  const payload = data.data || {}
  activeSemesterName.value = payload?.activeSemester?.name || ''
  submittedPendingCount.value = Number(payload?.submittedPendingCount || 0)
}

const loadFeedbackNewCount = async () => {
  const { data } = await http.get('/feedbacks', { params: { status: 'NEW' }, meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  feedbackNewCount.value = rows.length
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

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadSemesterOverview(), loadFeedbackNewCount(), loadNotices()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.workbench-grid .workbench-card {
  box-shadow: none;
  background: #fbfdff;
  border-color: #e5e7eb;
}

.workbench-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.workbench-meta {
  margin-top: 8px;
  line-height: 1.7;
}

.workbench-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.workbench-stat {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px 12px;
  background: #ffffff;
}

.workbench-stat-value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.workbench-notice-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.workbench-notice-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.workbench-notice-title {
  font-size: 13px;
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.workbench-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>

