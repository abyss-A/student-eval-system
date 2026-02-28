import { ref } from 'vue'

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export default function useAutoSaveDraft(saveFn, options = {}) {
  const debounceMs = Number(options.debounceMs ?? 1200)

  const dirty = ref(false)
  const saving = ref(false)
  const error = ref('')
  const lastSavedAt = ref('')

  let timer = null
  let queued = false

  const clearTimer = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const formatTime = (date) => {
    const hh = String(date.getHours()).padStart(2, '0')
    const mm = String(date.getMinutes()).padStart(2, '0')
    const ss = String(date.getSeconds()).padStart(2, '0')
    return `${hh}:${mm}:${ss}`
  }

  const runSave = async () => {
    if (saving.value) {
      queued = true
      return
    }
    if (!dirty.value && !queued) return

    clearTimer()
    saving.value = true
    error.value = ''
    queued = false

    try {
      await saveFn()
      dirty.value = false
      lastSavedAt.value = formatTime(new Date())
    } catch (e) {
      const msg = e?.userMessage || e?.message || '自动保存失败'
      error.value = msg
      throw e
    } finally {
      saving.value = false
      if (dirty.value || queued) {
        queued = false
        schedule(200)
      }
    }
  }

  const schedule = (ms = debounceMs) => {
    clearTimer()
    timer = setTimeout(() => {
      runSave().catch(() => {})
    }, ms)
  }

  const markDirty = () => {
    dirty.value = true
    schedule()
  }

  const saveNow = async () => {
    queued = true
    await runSave()
  }

  const flushBeforeSubmit = async () => {
    clearTimer()

    if (saving.value) {
      for (let i = 0; i < 120 && saving.value; i += 1) {
        await sleep(50)
      }
    }

    if (dirty.value || queued) {
      await saveNow()
    }

    if (error.value) {
      throw new Error(error.value)
    }
  }

  const resetState = () => {
    clearTimer()
    dirty.value = false
    saving.value = false
    error.value = ''
    lastSavedAt.value = ''
    queued = false
  }

  return {
    dirty,
    saving,
    error,
    lastSavedAt,
    markDirty,
    saveNow,
    flushBeforeSubmit,
    resetState
  }
}
