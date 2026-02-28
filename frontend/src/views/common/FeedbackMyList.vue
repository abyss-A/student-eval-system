<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">我的反馈</h2>
        <p class="muted" style="margin-top: 6px;">查看我提交的反馈与处理结果。</p>
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
    </div>

    <div
      v-if="errorMsg"
      class="card"
      style="margin-top: 12px; border-color: #fecaca; background: #fef2f2; box-shadow: none;"
    >
      <div style="font-weight: 700; color: #b42318;">加载失败</div>
      <div class="muted" style="margin-top: 6px; white-space: pre-wrap;">{{ errorMsg }}</div>
      <div class="toolbar-row" style="margin-top: 10px;">
        <button class="btn secondary" type="button" @click="load" :disabled="loading">重试</button>
      </div>
    </div>

    <table class="table" style="margin-top: 12px;">
      <thead>
        <tr>
          <th>标题</th>
          <th style="width: 110px;">状态</th>
          <th style="width: 170px;">提交时间</th>
          <th style="width: 170px;">更新时间</th>
          <th style="width: 120px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="f in rows" :key="f.id">
          <td style="font-weight: 700; color: #0f172a;">{{ f.title }}</td>
          <td>
            <span class="badge" :class="statusBadge(f.status)">{{ statusLabel(f.status) }}</span>
          </td>
          <td>{{ formatDate(f.created_at) }}</td>
          <td>{{ formatDate(f.updated_at) }}</td>
          <td>
            <button class="btn ghost" type="button" @click="goDetail(f.id)">查看</button>
          </td>
        </tr>
        <tr v-if="!rows.length && !errorMsg">
          <td colspan="5" class="empty">暂无反馈</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'

const router = useRouter()
const route = useRoute()

const rows = ref([])
const loading = ref(false)
const activeStatus = ref('ALL')
const errorMsg = ref('')

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
  errorMsg.value = ''
  try {
    const params = {}
    if (activeStatus.value && activeStatus.value !== 'ALL') {
      params.status = activeStatus.value
    }
    const { data } = await http.get('/feedbacks/my', { params, meta: { silent: true } })
    rows.value = data.data || []
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => {
  const seg = String(route.path || '').split('/')[1] || 'student'
  router.push(`/${seg}/feedback/${id}`)
}

load()
</script>
