import { reactive } from 'vue'
import http from '../api/http'

const state = reactive({
  submissionId: null,
  status: '',
  detail: null,
  score: null,
  loading: false
})

async function ensureSubmission() {
  if (state.submissionId) return state.submissionId
  state.loading = true
  try {
    const { data } = await http.post('/submissions')
    const sub = data.data
    state.submissionId = sub?.id || null
    state.status = sub?.status || ''
    await loadDetail()
    await loadScore()
    return state.submissionId
  } finally {
    state.loading = false
  }
}

async function loadDetail() {
  if (!state.submissionId) return null
  const { data } = await http.get(`/submissions/${state.submissionId}`)
  state.detail = data.data
  state.status = data.data?.submission?.status || ''
  return state.detail
}

async function loadScore() {
  if (!state.submissionId) return null
  const { data } = await http.get(`/submissions/${state.submissionId}/score`)
  state.score = data.data
  state.status = data.data?.status || state.status
  return state.score
}

async function saveCourses(items) {
  await ensureSubmission()
  await http.put(`/submissions/${state.submissionId}/courses/batch`, { items })
  await loadDetail()
  await loadScore()
}

async function saveActivitiesModule(moduleType, items) {
  await ensureSubmission()
  await http.put(`/submissions/${state.submissionId}/activities/module/${moduleType}`, { items })
  await loadDetail()
  await loadScore()
}

async function submit() {
  await ensureSubmission()
  await http.post(`/submissions/${state.submissionId}/submit`)
  await loadDetail()
  await loadScore()
}

async function exportReport(format) {
  await ensureSubmission()
  const resp = await http.post(
    `/submissions/${state.submissionId}/report/export`,
    { format },
    { responseType: 'blob' }
  )
  const blob = new Blob([resp.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = format === 'PDF' ? `综合测评报告_${state.submissionId}.pdf` : `综合测评报告_${state.submissionId}.docx`
  a.click()
  URL.revokeObjectURL(url)
}

export default {
  state,
  ensureSubmission,
  loadDetail,
  loadScore,
  saveCourses,
  saveActivitiesModule,
  submit,
  exportReport
}

