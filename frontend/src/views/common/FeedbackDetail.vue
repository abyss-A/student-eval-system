<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">
          状态：<span class="badge" :class="statusBadge(detail?.status)">{{ statusLabel(detail?.status) }}</span>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          提交人：<b>{{ detail?.creator_real_name || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          提交时间：<b>{{ formatDate(detail?.created_at) }}</b>
        </p>
      </div>
      <div class="toolbar-row">
        <RouterLink class="btn ghost" :to="backTo">返回</RouterLink>
      </div>
    </div>

    <div v-if="loading" class="empty" style="margin-top: 12px;">加载中...</div>
    <div v-else-if="!detail" class="empty" style="margin-top: 12px;">反馈不存在或无权限查看</div>
    <div v-else style="margin-top: 14px;" class="grid">
      <div>
        <h3 style="margin: 0;">{{ detail.title }}</h3>
        <div class="muted" style="margin-top: 8px;">更新时间：{{ formatDate(detail.updated_at) }}</div>
      </div>

      <div class="card" style="box-shadow: none; background: #fbfdff;">
        <div style="font-weight: 700;">反馈内容</div>
        <div class="text-block" style="margin-top: 10px;">{{ detail.content }}</div>
      </div>

      <div class="card" style="box-shadow: none; background: #fbfdff;">
        <div style="font-weight: 700;">截图</div>
        <div style="margin-top: 10px;">
          <ImageIdsUploader :model-value="detail.screenshot_file_ids || ''" :readonly="true" />
        </div>
      </div>

      <div v-if="detail.reply_content || detail.replied_at" class="card" style="box-shadow: none; background: #fbfdff;">
        <div style="font-weight: 700;">处理回复</div>
        <p class="muted" style="margin-top: 8px;">
          处理人：<b>{{ detail.handler_real_name || '-' }}</b>
          <span style="margin: 0 6px; color: #cbd5e1;">|</span>
          回复时间：<b>{{ formatDate(detail.replied_at) }}</b>
          <span v-if="detail.closed_at" style="margin: 0 6px; color: #cbd5e1;">|</span>
          <span v-if="detail.closed_at">关闭时间：<b>{{ formatDate(detail.closed_at) }}</b></span>
        </p>
        <div class="text-block" style="margin-top: 10px;">{{ detail.reply_content || '（无）' }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'
import ImageIdsUploader from '../../components/ImageIdsUploader.vue'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)

const backTo = computed(() => {
  const seg = String(route.path || '').split('/')[1] || 'student'
  if (seg === 'student' || seg === 'teacher') return `/${seg}/feedback/mine`
  return '/admin/feedback/handle'
})

const statusLabel = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return '待处理'
  if (s === 'REPLIED') return '已回复'
  if (s === 'CLOSED') return '已关闭'
  return raw || '-'
}

const statusBadge = (raw) => {
  const s = String(raw || '').trim().toUpperCase()
  if (s === 'NEW') return 'warning'
  if (s === 'REPLIED') return 'success'
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
    const { data } = await http.get(`/feedbacks/${route.params.id}`)
    detail.value = data.data
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
.text-block {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #0f172a;
  font-size: 14px;
}
</style>

