<template>
  <div ref="rootRef" class="table-overflow-cell" :class="{ 'is-open': isOpen }">
    <span ref="textRef" class="table-overflow-cell__text">{{ displayText }}</span>

    <button
      v-if="isOverflow"
      class="table-overflow-cell__toggle"
      type="button"
      :aria-label="isOpen ? '收起全文' : '展开全文'"
      @click.stop="toggleOpen"
    >
      <svg viewBox="0 0 20 20" aria-hidden="true">
        <path d="M5 7.5L10 12.5L15 7.5" />
      </svg>
    </button>

    <div
      v-if="isOpen"
      ref="popupRef"
      class="table-overflow-cell__popup"
      :class="{ 'table-overflow-cell__popup--right': popupAlignRight }"
      @click.stop
    >
      <button class="table-overflow-cell__popup-close" type="button" aria-label="关闭" @click.stop="closeOpen">×</button>
      <div class="table-overflow-cell__popup-text">{{ displayText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  text: {
    type: [String, Number],
    default: ''
  },
  cellKey: {
    type: String,
    default: ''
  },
  emptyText: {
    type: String,
    default: '-'
  }
})

const rootMap = new Map()
const activeKey = ref('')

let listenersReady = false
const onPointerDown = (event) => {
  const key = activeKey.value
  if (!key) return
  const root = rootMap.get(key)
  if (!root || !root.contains(event.target)) {
    activeKey.value = ''
  }
}
const onKeyDown = (event) => {
  if (event.key === 'Escape') {
    activeKey.value = ''
  }
}
const onAnyScroll = () => {
  if (activeKey.value) {
    activeKey.value = ''
  }
}
const ensureListeners = () => {
  if (listenersReady || typeof document === 'undefined') return
  listenersReady = true
  document.addEventListener('pointerdown', onPointerDown, true)
  document.addEventListener('keydown', onKeyDown)
  window.addEventListener('scroll', onAnyScroll, true)
  window.addEventListener('resize', onAnyScroll)
}

let uid = 0
const localKey = `table_overflow_cell_${++uid}`

const displayText = computed(() => {
  const raw = props.text
  if (raw === null || raw === undefined) return props.emptyText
  const text = String(raw).trim()
  return text || props.emptyText
})

const resolvedKey = computed(() => String(props.cellKey || localKey))
const isOpen = computed(() => activeKey.value === resolvedKey.value)

const rootRef = ref(null)
const textRef = ref(null)
const popupRef = ref(null)
const isOverflow = ref(false)
const popupAlignRight = ref(false)

let rafId = 0
const cancelRaf = () => {
  if (!rafId) return
  cancelAnimationFrame(rafId)
  rafId = 0
}

const closeOpen = () => {
  if (isOpen.value) activeKey.value = ''
}

const measureOverflow = () => {
  cancelRaf()
  rafId = requestAnimationFrame(() => {
    rafId = 0
    const el = textRef.value
    if (!el) {
      isOverflow.value = false
      closeOpen()
      return
    }
    const overflow = (el.scrollWidth - el.clientWidth) > 1
    isOverflow.value = overflow
    if (!overflow) {
      closeOpen()
    }
  })
}

const updatePopupAlign = () => {
  popupAlignRight.value = false
  const popup = popupRef.value
  if (!popup) return
  const rect = popup.getBoundingClientRect()
  popupAlignRight.value = rect.right > (window.innerWidth - 12)
}

const toggleOpen = async () => {
  if (!isOverflow.value) return
  if (isOpen.value) {
    activeKey.value = ''
    return
  }
  activeKey.value = resolvedKey.value
  await nextTick()
  updatePopupAlign()
}

const unregisterKey = (key) => {
  rootMap.delete(key)
  if (activeKey.value === key) {
    activeKey.value = ''
  }
}

let resizeObserver = null

onMounted(() => {
  ensureListeners()
  rootMap.set(resolvedKey.value, rootRef.value)
  measureOverflow()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      measureOverflow()
      if (isOpen.value) {
        nextTick(updatePopupAlign)
      }
    })
    if (rootRef.value) resizeObserver.observe(rootRef.value)
    if (textRef.value) resizeObserver.observe(textRef.value)
  }
})

watch(() => props.text, () => {
  measureOverflow()
})

watch(resolvedKey, (nextKey, prevKey) => {
  if (prevKey && prevKey !== nextKey) {
    unregisterKey(prevKey)
  }
  rootMap.set(nextKey, rootRef.value)
})

watch(isOpen, async (opened) => {
  if (!opened) {
    popupAlignRight.value = false
    return
  }
  await nextTick()
  updatePopupAlign()
})

onBeforeUnmount(() => {
  cancelRaf()
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  unregisterKey(resolvedKey.value)
})
</script>

<style scoped>
.table-overflow-cell {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 6px;
}

.table-overflow-cell__text {
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.table-overflow-cell__toggle {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #7a8597;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: background-color 0.12s ease, color 0.12s ease, opacity 0.12s ease;
}

.table-overflow-cell__toggle:hover {
  background: #edf2fa;
  color: #375a8a;
}

.table-overflow-cell:hover .table-overflow-cell__toggle,
.table-overflow-cell:focus-within .table-overflow-cell__toggle,
.table-overflow-cell.is-open .table-overflow-cell__toggle {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.table-overflow-cell__toggle svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 0.12s ease;
}

.table-overflow-cell.is-open .table-overflow-cell__toggle svg {
  transform: rotate(180deg);
}

.table-overflow-cell__popup {
  position: absolute;
  left: 0;
  top: calc(100% + 6px);
  min-width: min(360px, 56vw);
  max-width: min(560px, 72vw);
  background: #ffffff;
  border: 1px solid #d8e1ef;
  box-shadow: 0 10px 28px rgba(15, 39, 77, 0.18);
  border-radius: 8px;
  z-index: 44;
  padding: 12px 42px 12px 12px;
}

.table-overflow-cell__popup--right {
  left: auto;
  right: 0;
}

.table-overflow-cell__popup-close {
  position: absolute;
  top: 9px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #cfd6e2;
  background: #6b7280;
  color: #fff;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  cursor: pointer;
}

.table-overflow-cell__popup-text {
  white-space: normal;
  word-break: break-word;
  color: #334155;
  line-height: 1.6;
}

@media (hover: none), (pointer: coarse) {
  .table-overflow-cell__toggle {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }
}

@media (max-width: 768px) {
  .table-overflow-cell__popup {
    min-width: min(300px, 86vw);
    max-width: min(420px, 92vw);
  }
}
</style>
