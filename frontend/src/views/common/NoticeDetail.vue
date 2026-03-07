<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">
          状态：<span class="badge" :class="statusBadge(notice?.status)">{{ statusLabel(notice?.status) }}</span>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          发布人：<b>{{ notice?.publisher_real_name || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          发布时间：<b>{{ formatDate(notice?.published_at) }}</b>
        </p>
      </div>
      <div class="toolbar-row">
        <RouterLink :to="backTo">
          <el-button type="default">返回列表</el-button>
        </RouterLink>
      </div>
    </div>

    <div v-if="loading" class="empty" style="margin-top: 12px;">加载中...</div>
    <div v-else-if="!notice" class="empty" style="margin-top: 12px;">公告不存在或无权限查看</div>
    <div v-else style="margin-top: 14px;">
      <h1 style="margin: 0; font-size: 20px; color: #0f172a;">{{ notice.title }}</h1>
      <div class="muted" style="margin-top: 10px;">更新时间：{{ formatDate(notice.updated_at) }}</div>
      <div class="notice-content" style="margin-top: 14px;">{{ notice.content }}</div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'

const route = useRoute()
const loading = ref(false)
const notice = ref(null)

const backTo = computed(() => String(route.path || '').replace(/\/[^/]+$/, ''))

const statusLabel = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'DRAFT') return '草稿'
  if (s === 'PUBLISHED') return '已发布'
  if (s === 'OFFLINE') return '已下线'
  return raw || '-'
}

const statusBadge = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'PUBLISHED') return 'success'
  if (s === 'DRAFT') return 'warning'
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

const load = async () => {
  loading.value = true
  try {
    const { data } = await http.get(`/notices/${route.params.id}`)
    notice.value = data.data
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  () => load(),
  { immediate: true }
)
</script>

<style scoped>
.notice-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #0f172a;
  font-size: 14px;
  padding: 12px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fbfdff;
}
</style>

