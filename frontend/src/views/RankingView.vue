<template>
  <section class="card">
    <h2>排名查询</h2>
    <div style="display:flex;gap:10px;align-items:center;">
      <input v-model.number="semesterId" type="number" placeholder="学期ID" style="max-width:180px;" />
      <button class="btn" @click="load">查询</button>
    </div>
    <table class="table" style="margin-top:10px;">
      <thead>
        <tr><th>学生</th><th>班级</th><th>专业</th><th>总分</th><th>班级排名</th><th>专业排名</th></tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r.id">
          <td>{{ r.real_name }}</td>
          <td>{{ r.class_name }}</td>
          <td>{{ r.major_name }}</td>
          <td>{{ r.total_score }}</td>
          <td>{{ r.rankClass }}</td>
          <td>{{ r.rankMajor }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const semesterId = ref(1)
const rows = ref([])

const load = async () => {
  const { data } = await http.get('/rankings', { params: { semesterId: semesterId.value } })
  rows.value = data.data || []
}

load()
</script>
