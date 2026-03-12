<template>
  <section class="dash-page">
    <div class="card dash-hero dash-hero--admin">
      <div class="dash-hero__top">
        <div>
          <p class="dash-hero__kicker">欢迎回来，<b>{{ realName || '管理员' }}</b></p>
          <div class="dash-hero__headline">管理首页</div>
          <div class="dash-hero__meta">
            <el-tag type="info" effect="plain">{{ activeSemesterName || '未设置当前学期' }}</el-tag>
            <el-tag type="warning" effect="plain">待审核：{{ submittedPendingCount }}</el-tag>
            <el-tag type="warning" effect="plain">待处理反馈：{{ feedbackNewCount }}</el-tag>
          </div>
        </div>

        <div class="dash-hero__actions">
          <el-button type="primary" @click="go('/admin/submissions')">测评单查看</el-button>
          <el-button type="default" @click="go('/admin/feedback/handle', { preset: 'NEW' })">反馈处理</el-button>
          <el-button type="default" @click="go('/admin/accounts')">账号管理</el-button>
        </div>
      </div>

      <div v-if="loading" class="dash-empty" style="padding: 12px 0;">加载中...</div>
      <div v-else class="dash-hero__bottom dash-hero__bottom--single">
        <div class="dash-progress">
          <div class="dash-progress__head">
            <div class="dash-progress__title">当前学期概览</div>
            <div class="muted">最近动态</div>
          </div>

          <div class="admin-metrics" style="margin-top: 10px;">
            <div class="dash-metric">
              <div class="dash-metric__label">学生已提交待审核</div>
              <div class="dash-metric__value">{{ submittedPendingCount }}</div>
            </div>
            <div class="dash-metric">
              <div class="dash-metric__label">可查看测评单</div>
              <div class="dash-metric__value">{{ adminTaskCount }}</div>
            </div>
            <div class="dash-metric">
              <div class="dash-metric__label">待处理反馈</div>
              <div class="dash-metric__value">{{ feedbackNewCount }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="dash-cols">
      <div class="dash-col dash-col--fill">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">反馈预览</h3>
            <el-button type="default" size="small" @click="go('/admin/feedback/handle', { preset: 'NEW' })">查看全部</el-button>
          </div>

          <div v-if="feedbackPreview.length" class="dash-list">
            <div v-for="f in feedbackPreview" :key="f.id" class="dash-row">
              <div class="dash-row__main">
                <div class="dash-row__title" :title="f.title">{{ f.title }}</div>
                <div class="dash-row__meta">
                  {{ f.creator_real_name || '-' }} · {{ f.class_name || '-' }} · {{ formatDate(f.created_at) }}
                </div>
              </div>
              <div class="dash-row__side">
                <el-button size="small" type="primary" @click="openFeedbackFromHome(f)">立即处理</el-button>
              </div>
            </div>
          </div>
          <div v-else class="dash-empty">暂无反馈</div>
        </div>
      </div>

      <div class="dash-col dash-col--fill">
        <div class="card">
          <div class="dash-card__head">
            <h3 class="dash-card__title">测评单动态预览</h3>
            <el-button type="default" size="small" @click="go('/admin/submissions')">查看全部</el-button>
          </div>

          <div v-if="taskPreview.length" class="dash-list">
            <div v-for="t in taskPreview" :key="t.id" class="dash-row">
              <div class="dash-row__main">
                <div class="dash-row__title">{{ t.real_name || '-' }}（{{ t.account_no || t.accountNo || '-' }}）</div>
                <div class="dash-row__meta">
                  {{ t.class_name || '-' }} · 总分 {{ t.total_score ?? '-' }} · {{ formatDate(t.passTime) }}
                </div>
              </div>
              <div class="dash-row__side">
                <el-button size="small" type="primary" @click="openSubmissionFromHome(t)">查看</el-button>
              </div>
            </div>
          </div>
          <div v-else class="dash-empty">暂无数据</div>
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
const feedbackNewList = ref([])
const adminTasks = ref([])

const realName = computed(() => getRealName())
const feedbackNewCount = computed(() => feedbackNewList.value.length)
const adminTaskCount = computed(() => adminTasks.value.length)

const feedbackPreview = computed(() => {
  const rows = feedbackNewList.value.slice()
  rows.sort((a, b) => new Date(b?.created_at || 0).getTime() - new Date(a?.created_at || 0).getTime())
  return rows.slice(0, 3)
})

const taskPreview = computed(() => {
  const rows = adminTasks.value.slice()
  rows.sort((a, b) => new Date(b?.passTime || b?.pass_time || 0).getTime() - new Date(a?.passTime || a?.pass_time || 0).getTime())
  return rows.slice(0, 3)
})

const go = (path, query = undefined) => {
  if (query) router.push({ path, query })
  else router.push(path)
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

const loadFeedbackNewList = async () => {
  const { data } = await http.get('/feedbacks', { params: { status: 'NEW' }, meta: { silent: true } })
  const rows = Array.isArray(data.data) ? data.data : []
  feedbackNewList.value = rows
}

const loadAdminTasks = async () => {
  const { data } = await http.get('/admin/tasks', { meta: { silent: true } })
  adminTasks.value = Array.isArray(data.data) ? data.data : []
}

const openFeedbackFromHome = (row) => {
  const id = row?.id
  if (!id) return
  router.push({ path: '/admin/feedback/handle', query: { open: String(id), preset: 'NEW' } })
}

const openSubmissionFromHome = (row) => {
  const id = row?.id
  if (!id) return
  router.push({ path: '/admin/submissions', query: { open: String(id) } })
}

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadSemesterOverview(), loadFeedbackNewList(), loadAdminTasks()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.admin-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1080px) {
  .admin-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
