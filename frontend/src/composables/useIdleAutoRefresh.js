import { onBeforeUnmount, onMounted, unref } from 'vue'

export default function useIdleAutoRefresh({
  refreshFn,
  intervalMs = 30000,
  isBusy = false,
  isPaused = false
}) {
  let timer = null
  let lastInputAt = Date.now()

  const markInput = () => {
    lastInputAt = Date.now()
  }

  const hasFocusedEditor = () => {
    const el = document.activeElement
    if (!el) return false
    const tag = String(el.tagName || '').toUpperCase()
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
    return Boolean(el.isContentEditable)
  }

  const canRun = () => {
    if (typeof document !== 'undefined' && document.hidden) return false
    if (unref(isBusy)) return false
    if (unref(isPaused)) return false
    if (hasFocusedEditor()) return false
    if (Date.now() - lastInputAt < 3000) return false
    return true
  }

  const tick = async () => {
    if (!canRun()) return
    try {
      await refreshFn()
    } catch (_) {
      // Keep silent. Existing page-level load handlers already expose errors where needed.
    }
  }

  const start = () => {
    if (timer) return
    timer = setInterval(tick, intervalMs)
  }

  const stop = () => {
    if (!timer) return
    clearInterval(timer)
    timer = null
  }

  onMounted(() => {
    window.addEventListener('keydown', markInput, true)
    window.addEventListener('input', markInput, true)
    start()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', markInput, true)
    window.removeEventListener('input', markInput, true)
    stop()
  })

  return {
    start,
    stop
  }
}
