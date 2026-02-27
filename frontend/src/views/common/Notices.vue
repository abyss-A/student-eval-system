<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">公告通知</h2>
        <p class="muted" style="margin-top: 6px;">发布通知、查看公告。学生仅能查看已发布公告。</p>
      </div>
      <div class="toolbar-row">
        <button v-if="canManage" class="btn" type="button" @click="openCreate">新建公告</button>
        <button class="btn secondary" type="button" @click="load" :disabled="loading">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
    </div>

    <div v-if="canManage" class="toolbar-row" style="margin-top: 12px;">
      <div class="toolbar-row" style="gap: 8px;">
        <button class="btn secondary" type="button" @click="setTab('DRAFT')" :disabled="loading">草稿</button>
        <button class="btn secondary" type="button" @click="setTab('PUBLISHED')" :disabled="loading">已发布</button>
        <button class="btn secondary" type="button" @click="setTab('OFFLINE')" :disabled="loading">已下线</button>
        <button class="btn ghost" type="button" @click="setTab('ALL')" :disabled="loading">全部</button>
        <span class="muted">当前：<b>{{ tabLabel(activeTab) }}</b></span>
      </div>
      <div class="toolbar-row" style="flex: 1; justify-content: flex-end;">
        <input v-model.trim="keyword" placeholder="按标题关键字搜索" style="max-width: 280px;" />
        <button class="btn secondary" type="button" @click="load" :disabled="loading">搜索</button>
      </div>
    </div>

    <div
      v-if="errorMsg"
      class="card"
      style="margin-top: 12px; border-color: #fecaca; background: #fef2f2; box-shadow: none;"
    >
      <div style="font-weight: 900; color: #b42318;">加载失败</div>
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
          <th style="width: 140px;">发布人</th>
          <th style="width: 170px;">发布时间</th>
          <th style="width: 170px;">更新时间</th>
          <th style="width: 250px;">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="n in rows" :key="n.id">
          <td>
            <div style="font-weight: 900; color: #0f172a;">{{ n.title }}</div>
          </td>
          <td>
            <span class="badge" :class="statusBadge(n.status)">{{ statusLabel(n.status) }}</span>
          </td>
          <td>{{ n.publisher_real_name || '-' }}</td>
          <td>{{ formatDate(n.published_at) }}</td>
          <td>{{ formatDate(n.updated_at) }}</td>
          <td>
            <div class="action-row">
              <button class="btn ghost" type="button" @click="goDetail(n.id)">查看</button>
              <button v-if="canManage && canEdit(n)" class="btn secondary" type="button" @click="openEdit(n)">编辑</button>
              <button v-if="canManage && canPublish(n)" class="btn" type="button" @click="publish(n.id)">发布</button>
              <button v-if="canManage && canOffline(n)" class="btn secondary" type="button" @click="offline(n.id)">下线</button>
              <button v-if="canManage && canDelete(n)" class="btn danger" type="button" @click="remove(n.id)">删除</button>
            </div>
          </td>
        </tr>
        <tr v-if="!rows.length && !errorMsg">
          <td colspan="6" class="empty">暂无公告</td>
        </tr>
      </tbody>
    </table>
  </section>

  <div v-if="drawer.open" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 900; font-size: 16px;">{{ drawer.mode === 'create' ? '新建公告' : '编辑公告' }}</div>
          <p class="muted" style="margin-top: 6px;">公告内容支持换行显示。已发布公告需先下线再修改。</p>
        </div>
        <button class="icon-btn" type="button" @click="closeDrawer" aria-label="关闭">✕</button>
      </div>

      <div class="drawer-body">
        <div class="stack">
          <label class="field">
            <span class="field-label">标题</span>
            <input v-model.trim="form.title" placeholder="请输入公告标题" />
          </label>
          <label class="field">
            <span class="field-label">内容</span>
            <textarea v-model.trim="form.content" rows="10" placeholder="请输入公告内容（支持换行）"></textarea>
          </label>
        </div>
      </div>

      <div class="drawer-footer">
        <button class="btn secondary" type="button" @click="closeDrawer" :disabled="drawer.saving">取消</button>
        <button class="btn" type="button" @click="saveDraft" :disabled="drawer.saving">
          {{ drawer.saving ? '保存中...' : '保存草稿' }}
        </button>
        <button v-if="drawer.mode === 'edit'" class="btn ghost" type="button" @click="publish(form.id)" :disabled="drawer.saving">
          发布
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import { getRole, getUserId } from '../../utils/auth'

const router = useRouter()
const route = useRoute()

const role = ref(getRole())
const myUserId = Number(getUserId() || 0)

const canManage = computed(() => role.value === 'COUNSELOR' || role.value === 'ADMIN')

const activeTab = ref('PUBLISHED')
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const errorMsg = ref('')

const drawer = reactive({
  open: false,
  mode: 'create', // create|edit
  saving: false
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  status: ''
})

const tabLabel = (t) => {
  if (t === 'DRAFT') return '草稿'
  if (t === 'PUBLISHED') return '已发布'
  if (t === 'OFFLINE') return '已下线'
  return '全部'
}

const setTab = (t) => {
  activeTab.value = t
  load()
}

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
  if (s === 'OFFLINE') return ''
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

const canEdit = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'DRAFT' && status !== 'OFFLINE') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canPublish = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'DRAFT' && status !== 'OFFLINE') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canOffline = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status !== 'PUBLISHED') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const canDelete = (n) => {
  const status = String(n.status || '').toUpperCase()
  if (status === 'PUBLISHED') return false
  if (role.value === 'ADMIN') return true
  return Number(n.publisher_id) === myUserId
}

const load = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {}
    if (canManage.value) {
      if (activeTab.value && activeTab.value !== 'ALL') params.status = activeTab.value
      if (keyword.value) params.keyword = keyword.value
    }
    const { data } = await http.get('/notices', { params, meta: { silent: true } })
    rows.value = data.data || []
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => {
  router.push(`${route.path}/${id}`)
}

const openCreate = () => {
  drawer.open = true
  drawer.mode = 'create'
  form.id = null
  form.title = ''
  form.content = ''
  form.status = 'DRAFT'
}

const openEdit = (n) => {
  drawer.open = true
  drawer.mode = 'edit'
  form.id = n.id
  form.title = n.title || ''
  form.content = n.content || ''
  form.status = n.status || ''
}

const closeDrawer = () => {
  drawer.open = false
  drawer.saving = false
}

const saveDraft = async () => {
  if (!canManage.value) return
  if (!form.title.trim() || !form.content.trim()) {
    alert('标题和内容不能为空')
    return
  }
  drawer.saving = true
  try {
    if (drawer.mode === 'create') {
      const { data } = await http.post('/notices', { title: form.title, content: form.content })
      form.id = data.data?.id
      drawer.mode = 'edit'
    } else {
      await http.put(`/notices/${form.id}`, { title: form.title, content: form.content })
    }
    await load()
    alert('已保存')
  } finally {
    drawer.saving = false
  }
}

const publish = async (id) => {
  if (!id) {
    alert('请先保存草稿')
    return
  }
  await http.post(`/notices/${id}/publish`)
  await load()
  alert('已发布')
}

const offline = async (id) => {
  await http.post(`/notices/${id}/offline`)
  await load()
  alert('已下线')
}

const remove = async (id) => {
  if (!confirm('确认删除该公告？')) return
  await http.delete(`/notices/${id}`)
  await load()
  alert('已删除')
}

load()
</script>
