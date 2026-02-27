<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2>管理员终审</h2>
        <p class="muted">管理员可对已提交且已审核的测评单执行终审。</p>
      </div>
      <button class="btn" @click="loadTasks" :disabled="loadingTasks">
        {{ loadingTasks ? '刷新中...' : '刷新列表' }}
      </button>
    </div>

    <div class="stack" style="margin-top: 12px;">
      <label>手动终审（输入测评单 ID）</label>
      <div class="toolbar-row">
        <input v-model.number="manualSubmissionId" type="number" min="1" placeholder="例如 1" style="max-width: 220px;" />
        <button class="btn" @click="finalize(manualSubmissionId)" :disabled="isFinalizing || !manualSubmissionId">立即终审</button>
      </div>
    </div>

    <table class="table" style="margin-top: 16px;">
      <thead>
        <tr>
          <th>测评单ID</th>
          <th>学生</th>
          <th>班级</th>
          <th>状态</th>
          <th>当前总分</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>{{ task.id }}</td>
          <td>{{ task.real_name }}</td>
          <td>{{ task.class_name }}</td>
          <td><span class="badge">{{ statusLabel(task.status) }}</span></td>
          <td>{{ task.total_score ?? '-' }}</td>
          <td>
            <button class="btn" @click="finalize(task.id)" :disabled="isFinalizing">终审该测评单</button>
          </td>
        </tr>
        <tr v-if="!tasks.length">
          <td colspan="6" class="empty">暂无可操作数据</td>
        </tr>
      </tbody>
    </table>

    <p v-if="lastResult" class="muted" style="margin-top: 12px;">
      最近一次终审：测评单 #{{ lastResult.id }}，状态 {{ statusLabel(lastResult.status) }}，总分 {{ lastResult.totalScore }}
    </p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const tasks = ref([])
const loadingTasks = ref(false)
const isFinalizing = ref(false)
const manualSubmissionId = ref(1)
const lastResult = ref(null)

const statusLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'DRAFT') return '草稿'
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'FINALIZED') return '已终审'
  if (code === 'PUBLISHED') return '已公示'
  return raw || '-'
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

const finalize = async (submissionId) => {
  if (!submissionId) {
    alert('请先输入测评单 ID')
    return
  }

  isFinalizing.value = true
  try {
    const { data } = await http.post(`/admin/submissions/${submissionId}/finalize`)
    lastResult.value = data.data
    alert(`终审成功：测评单 #${submissionId}`)
    await loadTasks()
  } finally {
    isFinalizing.value = false
  }
}

loadTasks()
</script>
