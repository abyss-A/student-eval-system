<template>
  <section class="card">
    <h2>辅导员审核</h2>
    <button class="btn" @click="loadTasks">刷新待审</button>
    <table class="table" style="margin-top:10px;">
      <thead><tr><th>测评单ID</th><th>学生</th><th>班级</th><th>总分</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="t in tasks" :key="t.id">
          <td>{{ t.id }}</td>
          <td>{{ t.real_name }}</td>
          <td>{{ t.class_name }}</td>
          <td>{{ t.total_score }}</td>
          <td><button class="btn secondary" @click="openTask(t.id)">打开</button></td>
        </tr>
      </tbody>
    </table>
  </section>

  <section class="card" style="margin-top:16px;" v-if="current">
    <h3>审核测评单 #{{ current.submission.id }}</h3>
    <h4>课程审核</h4>
    <table class="table">
      <thead><tr><th>课程</th><th>原分</th><th>审核分</th><th>动作</th></tr></thead>
      <tbody>
        <tr v-for="c in current.courses" :key="'c'+c.id">
          <td>{{ c.courseName }}</td>
          <td>{{ c.score }}</td>
          <td><input type="number" v-model.number="c._adjusted" /></td>
          <td>
            <button class="btn" @click="decide('COURSE', c.id, 'APPROVE')">通过</button>
            <button class="btn secondary" @click="decide('COURSE', c.id, 'ADJUST', c._adjusted)">改分</button>
            <button class="btn secondary" @click="decide('COURSE', c.id, 'REJECT')">驳回</button>
          </td>
        </tr>
      </tbody>
    </table>

    <h4 style="margin-top:14px;">活动审核</h4>
    <table class="table">
      <thead><tr><th>板块</th><th>标题</th><th>原分</th><th>审核分</th><th>动作</th></tr></thead>
      <tbody>
        <tr v-for="a in current.activities" :key="'a'+a.id">
          <td>{{ a.moduleType }}</td>
          <td>{{ a.title }}</td>
          <td>{{ a.selfScore }}</td>
          <td><input type="number" v-model.number="a._adjusted" /></td>
          <td>
            <button class="btn" @click="decide('ACTIVITY', a.id, 'APPROVE')">通过</button>
            <button class="btn secondary" @click="decide('ACTIVITY', a.id, 'ADJUST', a._adjusted)">改分</button>
            <button class="btn secondary" @click="decide('ACTIVITY', a.id, 'REJECT')">驳回</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const tasks = ref([])
const current = ref(null)

const loadTasks = async () => {
  const { data } = await http.get('/reviews/tasks')
  tasks.value = data.data || []
}

const openTask = async (id) => {
  const { data } = await http.get(`/submissions/${id}`)
  current.value = data.data
}

const decide = async (itemType, itemId, action, adjustedScore) => {
  const body = { action, reason: '辅导员审核' }
  if (action === 'ADJUST') body.adjustedScore = adjustedScore ?? 0
  await http.post(`/reviews/items/${itemType}/${itemId}/decision`, body)
  alert('已处理')
  if (current.value) {
    await openTask(current.value.submission.id)
  }
}

loadTasks()
</script>
