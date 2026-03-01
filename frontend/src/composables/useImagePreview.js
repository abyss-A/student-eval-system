import { reactive } from 'vue'

const MIN_SCALE = 0.4
const MAX_SCALE = 3
const SCALE_STEP = 0.15

const state = reactive({
  visible: false,
  loading: false,
  error: '',
  title: '图片预览',
  currentId: null,
  galleryIds: [],
  currentIndex: 0,
  imageUrl: '',
  fileName: '',
  scale: 1
})

let httpClient = null
let requestToken = 0
let activeObjectUrl = ''
let fileNameMap = {}

const clampScale = (value) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return 1
  return Math.min(MAX_SCALE, Math.max(MIN_SCALE, num))
}

const normalizeIdList = (galleryIds, currentId) => {
  const out = []
  const seen = new Set()

  const push = (value) => {
    const id = Number(value)
    if (!Number.isFinite(id) || id <= 0 || seen.has(id)) return
    seen.add(id)
    out.push(id)
  }

  ;(galleryIds || []).forEach(push)
  push(currentId)

  return out.length ? out : [Number(currentId)]
}

const revokeObjectUrl = () => {
  if (activeObjectUrl) {
    URL.revokeObjectURL(activeObjectUrl)
    activeObjectUrl = ''
  }
  state.imageUrl = ''
}

const resolveCurrentId = () => {
  if (!state.galleryIds.length) return Number(state.currentId || 0)
  return Number(state.galleryIds[state.currentIndex] || 0)
}

const loadCurrent = async (throwOnError = true) => {
  const id = resolveCurrentId()
  if (!id) {
    state.error = '暂无可预览图片'
    state.loading = false
    return
  }

  if (!httpClient || typeof httpClient.get !== 'function') {
    state.error = '预览失败，请稍后重试'
    state.loading = false
    if (throwOnError) throw new Error('missing http client')
    return
  }

  const token = ++requestToken
  state.loading = true
  state.error = ''
  state.currentId = id
  state.fileName = fileNameMap[id] || `附件#${id}`
  revokeObjectUrl()

  try {
    const resp = await httpClient.get(`/files/${id}/download`, { responseType: 'blob' })
    if (token !== requestToken) return

    const headerType = String(resp?.headers?.['content-type'] || '').toLowerCase()
    const blob = resp?.data instanceof Blob
      ? resp.data
      : new Blob([resp?.data], { type: headerType || 'application/octet-stream' })
    const mimeType = String(blob.type || headerType || '').toLowerCase()

    if (!mimeType.startsWith('image/')) {
      state.error = '该文件非图片，无法预览'
      return
    }

    activeObjectUrl = URL.createObjectURL(blob)
    state.imageUrl = activeObjectUrl
  } catch (error) {
    if (token !== requestToken) return
    state.error = '预览失败，请重试'
    if (throwOnError) throw error
  } finally {
    if (token === requestToken) {
      state.loading = false
    }
  }
}

const resetScale = () => {
  state.scale = 1
}

const setScale = (value) => {
  state.scale = clampScale(value)
}

const zoomIn = () => {
  setScale(state.scale + SCALE_STEP)
}

const zoomOut = () => {
  setScale(state.scale - SCALE_STEP)
}

const canPrev = () => state.galleryIds.length > 1 && state.currentIndex > 0
const canNext = () => state.galleryIds.length > 1 && state.currentIndex < state.galleryIds.length - 1

const prev = async () => {
  if (!canPrev()) return
  state.currentIndex -= 1
  resetScale()
  await loadCurrent(false)
}

const next = async () => {
  if (!canNext()) return
  state.currentIndex += 1
  resetScale()
  await loadCurrent(false)
}

const close = () => {
  requestToken += 1
  state.visible = false
  state.loading = false
  state.error = ''
  state.title = '图片预览'
  state.currentId = null
  state.galleryIds = []
  state.currentIndex = 0
  state.fileName = ''
  resetScale()
  revokeObjectUrl()
}

const open = async ({ http, fileId, title = '图片预览', galleryIds = [], fileNameMap: nameMap = {} } = {}) => {
  const id = Number(fileId)
  if (!Number.isFinite(id) || id <= 0) return

  httpClient = http
  fileNameMap = nameMap || {}

  const normalized = normalizeIdList(galleryIds, id)

  state.visible = true
  state.title = title || '图片预览'
  state.galleryIds = normalized
  state.currentIndex = Math.max(0, normalized.indexOf(id))
  state.currentId = id
  resetScale()

  await loadCurrent(true)
}

const imagePreviewApi = {
  state,
  open,
  close,
  prev,
  next,
  canPrev,
  canNext,
  setScale,
  zoomIn,
  zoomOut,
  resetScale
}

export function useImagePreview() {
  return imagePreviewApi
}

