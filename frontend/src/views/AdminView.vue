<template>
  <section class="card">
    <h2>管理员终审</h2>
    <div style="display:flex;gap:10px;align-items:center;">
      <input v-model.number="submissionId" type="number" placeholder="输入测评单ID" style="max-width:220px;" />
      <button class="btn" @click="finalize">终审</button>
    </div>
    <p style="margin-top:10px;color:#5f5f5f;">提示：辅导员审核后，管理员可对测评单执行终审。</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const submissionId = ref(1)

const finalize = async () => {
  await http.post(`/admin/submissions/${submissionId.value}/finalize`)
  alert('终审成功')
}
</script>
