<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">反馈处理</h2>
        <p class="muted" style="margin-top: 6px;">辅导员/管理员查看反馈、回复并关闭。</p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" type="button" @click="load" :disabled="loading">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
    </div>

    <div class="toolbar-row" style="margin-top: 12px;">
      <button class="btn secondary" type="button" @click="setStatus('ALL')" :disabled="loading">全部</button>
      <button class="btn secondary" type="button" @click="setStatus('NEW')" :disabled="loading">待处理</button>
      <button class="btn secondary" type="button" @click="setStatus('REPLIED')" :disabled="loading">已回复</button>
      <button class="btn secondary" type="button" @click="setStatus('CLOSED')" :disabled="loading">已关闭</button>
      <span class="muted">当前：<b>{{ statusLabel(activeStatus) }}</b></span>
      <div class="toolbar-row" style="flex: 1; justify-content: flex-end;">
        <input v-model.trim="keyword" placeholder="搜索标题/内容关键词" style="max-width: 320px;" />
        <button class="btn secondary" type="button" @click="load" :disabled="loading">搜索</button>
      </div>
    </div>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th>标题</th>
          <th style="width: 110px;">状态</th>
          <th style="width: 160px;">提交人</th>
          <th style="width: 180px;">班级</th>
          <th style="width: 170px;">提交时间</th>
          <th style="width: 140px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="f in rows" :key="f.id">
          <td style="font-weight: 700; color: #0f172a;">{{ f.title }}</td>
          <td>
            <span class="badge" :class="statusBadge(f.status)">{{ statusLabel(f.status) }}</span>
          </td>
          <td>{{ f.creator_real_name || '-' }}</td>
          <td>{{ f.class_name || '-' }}</td>
          <td>{{ formatDate(f.created_at) }}</td>
          <td>
            <button class="btn ghost" type="button" @click="openDrawer(f.id)">处理</button>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td colspan="6" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </section>

  <div v-if="drawer.open" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel drawer-wide">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">反馈处理</div>
          <p class="muted" style="margin-top: 6px;">
            状态：<b>{{ statusLabel(drawer.detail?.status) }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            提交人：<b>{{ drawer.detail?.creator_real_name || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            提交时间：<b>{{ formatDate(drawer.detail?.created_at) }}</b>
          </p>
        </div>
        <button class="icon-btn" type="button" @click="closeDrawer" aria-label="关闭">✕</button>
      </div>

      <div class="drawer-body">
        <div v-if="drawer.loading" class="empty">加载中...</div>
        <div v-else-if="!drawer.detail" class="empty">无数据</div>
        <div v-else class="grid">
          <div class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">标题</div>
            <div style="margin-top: 8px; font-weight: 700; color: #0f172a;">{{ drawer.detail.title }}</div>
          </div>

          <div class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">反馈内容</div>
            <div class="text-block" style="margin-top: 10px;">{{ drawer.detail.content }}</div>
          </div>

          <div class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">截图</div>
            <div style="margin-top: 10px;">
              <ImageIdsUploader :model-value="drawer.detail.screenshot_file_ids || ''" :readonly="true" />
            </div>
          </div>

          <div v-if="drawer.detail.reply_content || drawer.detail.replied_at" class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">历史回复</div>
            <p class="muted" style="margin-top: 8px;">
              处理人：<b>{{ drawer.detail.handler_real_name || '-' }}</b>
              <span style="margin: 0 6px; color: #cbd5e1;">|</span>
              回复时间：<b>{{ formatDate(drawer.detail.replied_at) }}</b>
              <span v-if="drawer.detail.closed_at" style="margin: 0 6px; color: #cbd5e1;">|</span>
              <span v-if="drawer.detail.closed_at">关闭时间：<b>{{ formatDate(drawer.detail.closed_at) }}</b></span>
            </p>
            <div class="text-block" style="margin-top: 10px;">{{ drawer.detail.reply_content || '（无）' }}</div>
          </div>

          <div v-if="String(drawer.detail.status || '').toUpperCase() !== 'CLOSED'" class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">处理操作</div>
            <div class="stack" style="margin-top: 10px;">
              <label class="field">
                <span class="field-label">回复内容（回复时必填，关闭时可选）</span>
                <textarea v-model.trim="drawer.replyContent" rows="5" placeholder="请输入回复内容"></textarea>
              </label>
              <div class="action-row">
                <button class="btn" type="button" @click="reply" :disabled="drawer.handling">
                  {{ drawer.handling ? '处理中...' : '回复' }}
                </button>
                <button class="btn secondary" type="button" @click="closeIt" :disabled="drawer.handling">关闭</button>
                <RouterLink class="btn ghost" :to="detailLink(drawer.detail.id)">详情页</RouterLink>
              </div>
            </div>
          </div>

          <div v-else class="card" style="box-shadow: none; background: #fbfdff;">
            <div style="font-weight: 700;">该反馈已关闭</div>
            <div class="toolbar-row" style="margin-top: 10px;">
              <RouterLink class="btn ghost" :to="detailLink(drawer.detail.id)">查看详情页</RouterLink>
            </div>
          </div>
        </div>
      </div>

      <div class="drawer-footer">
        <button class="btn secondary" type="button" @click="closeDrawer">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'
import ImageIdsUploader from '../../components/ImageIdsUploader.vue'

const route = useRoute()

const rows = ref([])
const loading = ref(false)
const activeStatus = ref('NEW')
const keyword = ref('')

const drawer = reactive({
  open: false,
  loading: false,
  handling: false,
  detail: null,
  replyContent: ''
})

const statusLabel = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return '待处理'
  if (s === 'REPLIED') return '已回复'
  if (s === 'CLOSED') return '已关闭'
  if (s === 'ALL') return '全部'
  return raw || '-'
}

const statusBadge = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return 'warning'
  if (s === 'REPLIED') return 'success'
  if (s === 'CLOSED') return ''
  return ''
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return String(raw)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const setStatus = (s) => {
  activeStatus.value = s
  load()
}

const load = async () => {
  loading.value = true
  try {
    const params = {}
    if (activeStatus.value && activeStatus.value !== 'ALL') params.status = activeStatus.value
    if (keyword.value) params.keyword = keyword.value
    const { data } = await http.get('/feedbacks', { params })
    rows.value = data.data || []
  } finally {
    loading.value = false
  }
}

const openDrawer = async (id) => {
  drawer.open = true
  drawer.loading = true
  drawer.handling = false
  drawer.replyContent = ''
  try {
    const { data } = await http.get(`/feedbacks/${id}`)
    drawer.detail = data.data
  } finally {
    drawer.loading = false
  }
}

const closeDrawer = () => {
  drawer.open = false
  drawer.detail = null
  drawer.replyContent = ''
  drawer.loading = false
  drawer.handling = false
}

const reply = async () => {
  if (!drawer.detail?.id) return
  if (!drawer.replyContent.trim()) {
    alert('回复内容不能为空')
    return
  }
  drawer.handling = true
  try {
    await http.post(`/feedbacks/${drawer.detail.id}/handle`, { action: 'REPLY', replyContent: drawer.replyContent })
    await openDrawer(drawer.detail.id)
    await load()
    alert('已回复')
  } finally {
    drawer.handling = false
  }
}

const closeIt = async () => {
  if (!drawer.detail?.id) return
  if (!confirm('确认关闭该反馈？')) return
  drawer.handling = true
  try {
    await http.post(`/feedbacks/${drawer.detail.id}/handle`, { action: 'CLOSE', replyContent: drawer.replyContent || '' })
    await openDrawer(drawer.detail.id)
    await load()
    alert('已关闭')
  } finally {
    drawer.handling = false
  }
}

const detailLink = (id) => {
  return `/admin/feedback/${id}`
}

load()
</script>

<style scoped>
.text-block {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #0f172a;
  font-size: 14px;
}
</style>
