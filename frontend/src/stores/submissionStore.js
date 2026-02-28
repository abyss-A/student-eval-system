import { reactive } from 'vue'
import http from '../api/http'
import { getUserId } from '../utils/auth'

const state = reactive({
  ownerUserId: '',
  submissionId: null,
  status: '',
  detail: null,
  score: null,
  loading: false
})

const autoSaveFlushers = new Map()

function reset() {
  state.submissionId = null
  state.status = ''
  state.detail = null
  state.score = null
  state.loading = false
  autoSaveFlushers.clear()
}

function syncOwner() {
  const currentUserId = String(getUserId() || '')
  if (state.ownerUserId !== currentUserId) {
    reset()
    state.ownerUserId = currentUserId
  }
  return currentUserId
}

async function ensureSubmission() {
  const userId = syncOwner()
  if (!userId) return null
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
  const userId = syncOwner()
  if (!userId || !state.submissionId) return null

  const { data } = await http.get(`/submissions/${state.submissionId}`)
  state.detail = data.data
  state.status = data.data?.submission?.status || ''
  return state.detail
}

async function loadScore() {
  const userId = syncOwner()
  if (!userId || !state.submissionId) return null

  const { data } = await http.get(`/submissions/${state.submissionId}/score`)
  state.score = data.data
  state.status = data.data?.status || state.status
  return state.score
}

async function saveCourses(items, options = {}) {
  const syncAfterSave = options.syncAfterSave !== false
  await ensureSubmission()
  await http.put(`/submissions/${state.submissionId}/courses/batch`, { items })
  if (syncAfterSave) {
    await loadDetail()
    await loadScore()
  }
}

async function saveActivitiesModule(moduleType, items, options = {}) {
  const syncAfterSave = options.syncAfterSave !== false
  await ensureSubmission()
  await http.put(`/submissions/${state.submissionId}/activities/module/${moduleType}`, { items })
  if (syncAfterSave) {
    await loadDetail()
    await loadScore()
  }
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
  a.download = resolveDownloadName(resp, format, state.submissionId)
  a.click()
  URL.revokeObjectURL(url)
}

function registerAutoSaveFlusher(key, flusher) {
  if (!key || typeof flusher !== 'function') return
  autoSaveFlushers.set(String(key), flusher)
}

async function flushAutoSaveFlushers() {
  const jobs = Array.from(autoSaveFlushers.values())
  for (const fn of jobs) {
    await fn()
  }
}

function resolveDownloadName(resp, format, submissionId) {
  const fallback = format === 'PDF'
    ? `综合奖学金申请表_${submissionId}.pdf`
    : `综合奖学金申请表_${submissionId}.docx`

  const disposition = resp?.headers?.['content-disposition'] || resp?.headers?.['Content-Disposition']
  if (!disposition) return fallback

  const utf8Match = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (_) {
      return utf8Match[1]
    }
  }

  const normalMatch = disposition.match(/filename\s*=\s*"?([^";]+)"?/i)
  if (normalMatch && normalMatch[1]) {
    return normalMatch[1]
  }

  return fallback
}

export default {
  state,
  reset,
  ensureSubmission,
  loadDetail,
  loadScore,
  saveCourses,
  saveActivitiesModule,
  registerAutoSaveFlusher,
  flushAutoSaveFlushers,
  submit,
  exportReport
}
