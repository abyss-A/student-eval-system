<template>
  <section class="grid two">
    <article class="card">
      <h2>学生端填报</h2>
      <p>当前用户：{{ realName }}（{{ role }}）</p>
      <div class="grid two">
        <button class="btn" @click="initSubmission">创建/加载我的测评单</button>
        <button class="btn secondary" @click="loadScore" :disabled="!submissionId">刷新计分</button>
      </div>
      <p v-if="submissionId">测评单ID：{{ submissionId }}，状态：{{ status }}</p>
    </article>

    <article class="card">
      <h3>分数预览</h3>
      <div class="grid two" v-if="score">
        <div>德育：{{ score.moralRaw }}</div>
        <div>智育：{{ score.intelRaw }}</div>
        <div>体育：{{ score.sportRaw }}</div>
        <div>美育：{{ score.artRaw }}</div>
        <div>劳动：{{ score.laborRaw }}</div>
        <div>总分：{{ score.totalScore }}</div>
      </div>
      <p v-else>暂无分数</p>
    </article>
  </section>

  <section class="card" style="margin-top:16px;">
    <h3>课程成绩（可编辑）</h3>
    <table class="table">
      <thead>
        <tr><th>课程名</th><th>类型</th><th>成绩</th><th>学分</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="(c, idx) in courses" :key="idx">
          <td><input v-model="c.courseName" /></td>
          <td>
            <select v-model="c.courseType">
              <option value="REQUIRED">必修</option>
              <option value="ELECTIVE">选修</option>
              <option value="RETAKE">重修</option>
              <option value="RELEARN">再修</option>
            </select>
          </td>
          <td><input type="number" v-model.number="c.score" /></td>
          <td><input type="number" v-model.number="c.credit" step="0.5" /></td>
          <td><button class="btn secondary" @click="courses.splice(idx,1)">删</button></td>
        </tr>
      </tbody>
    </table>
    <div style="display:flex;gap:10px;margin-top:10px;">
      <button class="btn secondary" @click="addCourse">新增课程</button>
      <button class="btn" @click="saveCourses" :disabled="!submissionId">保存课程</button>
    </div>
  </section>

  <section class="card" style="margin-top:16px;">
    <h3>活动条目（德智体美劳）</h3>
    <table class="table">
      <thead>
        <tr><th>板块</th><th>标题</th><th>说明</th><th>自评分</th><th>附件ID串</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="(a, idx) in activities" :key="idx">
          <td>
            <select v-model="a.moduleType">
              <option value="MORAL">德育</option>
              <option value="INTEL_PRO_INNOV">智育(专创)</option>
              <option value="SPORT_ACTIVITY">体育活动</option>
              <option value="ART">美育</option>
              <option value="LABOR">劳动</option>
            </select>
          </td>
          <td><input v-model="a.title" /></td>
          <td><input v-model="a.description" /></td>
          <td><input type="number" v-model.number="a.selfScore" step="0.5" /></td>
          <td><input v-model="a.evidenceFileIds" placeholder="如 12,15" /></td>
          <td><button class="btn secondary" @click="activities.splice(idx,1)">删</button></td>
        </tr>
      </tbody>
    </table>
    <div style="display:flex;gap:10px;margin-top:10px;flex-wrap:wrap;">
      <button class="btn secondary" @click="addActivity">新增活动</button>
      <button class="btn" @click="saveActivities" :disabled="!submissionId">保存活动</button>
      <button class="btn" @click="submitForm" :disabled="!submissionId">提交审核</button>
      <button class="btn" @click="exportFile('DOCX')" :disabled="!submissionId">导出Word</button>
      <button class="btn" @click="exportFile('PDF')" :disabled="!submissionId">导出PDF</button>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const role = localStorage.getItem('role') || 'UNKNOWN'
const realName = localStorage.getItem('realName') || '未登录'

const submissionId = ref(null)
const status = ref('DRAFT')
const score = ref(null)

const courses = ref([
  { courseName: '高等代数', courseType: 'REQUIRED', score: 88, credit: 4 },
  { courseName: '大学体育', courseType: 'REQUIRED', score: 80, credit: 2 }
])
const activities = ref([
  { moduleType: 'MORAL', title: '主题团日', description: '参与活动', selfScore: 5, evidenceFileIds: '' }
])

const initSubmission = async () => {
  const { data } = await http.post('/submissions')
  submissionId.value = data.data.id
  status.value = data.data.status
  await loadDetail()
}

const loadDetail = async () => {
  if (!submissionId.value) return
  const { data } = await http.get(`/submissions/${submissionId.value}`)
  const d = data.data
  courses.value = d.courses?.length ? d.courses : courses.value
  activities.value = d.activities?.length ? d.activities : activities.value
  status.value = d.submission.status
}

const loadScore = async () => {
  if (!submissionId.value) return
  const { data } = await http.get(`/submissions/${submissionId.value}/score`)
  score.value = data.data
}

const addCourse = () => {
  courses.value.push({ courseName: '', courseType: 'REQUIRED', score: 0, credit: 0 })
}
const addActivity = () => {
  activities.value.push({ moduleType: 'MORAL', title: '', description: '', selfScore: 0, evidenceFileIds: '' })
}

const saveCourses = async () => {
  await http.put(`/submissions/${submissionId.value}/courses/batch`, { items: courses.value })
  alert('课程已保存')
}

const saveActivities = async () => {
  await http.put(`/submissions/${submissionId.value}/activities/batch`, { items: activities.value })
  alert('活动已保存')
}

const submitForm = async () => {
  await http.post(`/submissions/${submissionId.value}/submit`)
  alert('已提交审核')
  await loadDetail()
  await loadScore()
}

const exportFile = async (format) => {
  const resp = await http.post(`/submissions/${submissionId.value}/report/export`, { format }, { responseType: 'blob' })
  const blob = new Blob([resp.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = format === 'PDF' ? `report_${submissionId.value}.pdf` : `report_${submissionId.value}.docx`
  a.click()
  URL.revokeObjectURL(url)
}
</script>
