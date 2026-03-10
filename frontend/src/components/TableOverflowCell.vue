<template>
  <div ref="rootRef" class="table-overflow-cell" :class="{ 'is-open': isOpen }">
    <span ref="textRef" class="table-overflow-cell__text">{{ displayText }}</span>

    <el-popover
      v-if="isOverflow"
      :visible="isOpen"
      :teleported="true"
      :width="popoverWidthPx || undefined"
      :popper-style="popoverPopperStyle"
      trigger="click"
      placement="bottom-start"
      :fallback-placements="['top-start', 'bottom-end', 'top-end']"
      :show-arrow="false"
      popper-class="table-overflow-cell-popper"
      @update:visible="onPopoverVisibleChange"
    >
      <template #reference>
        <button
          class="table-overflow-cell__toggle"
          type="button"
          :aria-label="isOpen ? '收起全文' : '展开全文'"
        >
          <svg viewBox="0 0 20 20" aria-hidden="true">
            <path d="M5 7.5L10 12.5L15 7.5" />
          </svg>
        </button>
      </template>

      <div ref="popoverRef" class="table-overflow-cell__popover" @click.stop>
        <button class="table-overflow-cell__popover-close" type="button" aria-label="关闭" @click.stop="closeOpen">&times;</button>
        <div
          ref="popoverTextRef"
          class="table-overflow-cell__popover-text"
        >
          {{ displayText }}
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { overflowCellRuntime } from './tableOverflowCellState'

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

const closeActive = () => {
  if (overflowCellRuntime.activeKey.value) overflowCellRuntime.activeKey.value = ''
}

const onKeyDown = (event) => {
  if (event.key === 'Escape') {
    closeActive()
  }
}

const onAnyScroll = (event) => {
  const target = event?.target
  if (target && typeof target.closest === 'function' && target.closest('.table-overflow-cell-popper')) {
    return
  }
  closeActive()
}

const ensureListeners = () => {
  if (overflowCellRuntime.listenersReady || typeof document === 'undefined') return
  overflowCellRuntime.listenersReady = true
  document.addEventListener('keydown', onKeyDown)
  window.addEventListener('scroll', onAnyScroll, true)
  window.addEventListener('resize', onAnyScroll)
}

const cleanupListeners = () => {
  if (!overflowCellRuntime.listenersReady || overflowCellRuntime.mountedCount > 0 || typeof document === 'undefined') return
  overflowCellRuntime.listenersReady = false
  document.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('scroll', onAnyScroll, true)
  window.removeEventListener('resize', onAnyScroll)
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
const isOpen = computed(() => overflowCellRuntime.activeKey.value === resolvedKey.value)

const rootRef = ref(null)
const textRef = ref(null)
const popoverRef = ref(null)
const popoverTextRef = ref(null)
const isOverflow = ref(false)
const popoverWidthPx = ref(0)

const TARGET_CHARS_PER_LINE = 20
const MIN_TEXT_WIDTH_PX = 160
const MIN_POPOVER_WIDTH_PX = 240

let rafId = 0
const cancelRaf = () => {
  if (!rafId) return
  cancelAnimationFrame(rafId)
  rafId = 0
}

const closeOpen = () => {
  if (isOpen.value) overflowCellRuntime.activeKey.value = ''
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

const updatePopoverLayout = () => {
  if (typeof window === 'undefined') return

  const popoverElement = popoverRef.value
  const popoverTextElement = popoverTextRef.value
  const popperElement =
    popoverElement?.closest?.('.table-overflow-cell-popper') || document.querySelector('.table-overflow-cell-popper')
  if (!popoverElement || !popoverTextElement || !popperElement) return

  const viewportWidth = window.innerWidth || 1440
  const isMobile = viewportWidth <= 768
  const maxOverallWidth = Math.floor((isMobile ? 0.94 : 0.88) * viewportWidth)
  const popoverStyle = window.getComputedStyle(popoverElement)
  const popperStyle = window.getComputedStyle(popperElement)
  const textStyle = window.getComputedStyle(popoverTextElement)
  const popoverChrome =
    Number.parseFloat(popoverStyle.paddingLeft || '0') +
    Number.parseFloat(popoverStyle.paddingRight || '0')
  const popperChrome =
    Number.parseFloat(popperStyle.paddingLeft || '0') +
    Number.parseFloat(popperStyle.paddingRight || '0') +
    Number.parseFloat(popperStyle.borderLeftWidth || '0') +
    Number.parseFloat(popperStyle.borderRightWidth || '0')

  const fontSizePx = Number.parseFloat(textStyle.fontSize || '14') || 14
  const targetTextWidth = Math.round(fontSizePx * TARGET_CHARS_PER_LINE)
  const maxTextWidthByViewport = Math.max(MIN_TEXT_WIDTH_PX, maxOverallWidth - popoverChrome - popperChrome)
  const maxTextWidth = Math.max(MIN_TEXT_WIDTH_PX, Math.min(targetTextWidth, maxTextWidthByViewport))

  const prevWhiteSpace = popoverTextElement.style.whiteSpace
  const prevOverflowWrap = popoverTextElement.style.overflowWrap
  const prevWordBreak = popoverTextElement.style.wordBreak
  popoverTextElement.style.whiteSpace = 'nowrap'
  popoverTextElement.style.overflowWrap = 'normal'
  popoverTextElement.style.wordBreak = 'normal'
  const singleLineWidth = Math.ceil(popoverTextElement.scrollWidth)
  popoverTextElement.style.whiteSpace = prevWhiteSpace
  popoverTextElement.style.overflowWrap = prevOverflowWrap
  popoverTextElement.style.wordBreak = prevWordBreak

  const effectiveTextWidth = Math.min(Math.max(singleLineWidth || 0, MIN_TEXT_WIDTH_PX), maxTextWidth)
  const totalWidth = effectiveTextWidth + popoverChrome + popperChrome
  popoverWidthPx.value = Math.min(Math.max(totalWidth, MIN_POPOVER_WIDTH_PX), maxOverallWidth)
}

const popoverPopperStyle = computed(() => {
  if (!popoverWidthPx.value) return {}
  return {
    width: `${popoverWidthPx.value}px`
  }
})

const onPopoverVisibleChange = (visible) => {
  if (visible) {
    if (!isOverflow.value) return
    overflowCellRuntime.activeKey.value = resolvedKey.value
    if (typeof window !== 'undefined') {
      const viewportWidth = window.innerWidth || 1440
      const isMobile = viewportWidth <= 768
      popoverWidthPx.value = Math.floor(Math.min((isMobile ? 0.94 : 0.88) * viewportWidth, 360))
    }
    nextTick(() => {
      requestAnimationFrame(() => {
        updatePopoverLayout()
      })
    })
    return
  }
  closeOpen()
}

let resizeObserver = null

onMounted(() => {
  overflowCellRuntime.mountedCount += 1
  ensureListeners()
  measureOverflow()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      measureOverflow()
      if (isOpen.value) updatePopoverLayout()
    })
    if (rootRef.value) resizeObserver.observe(rootRef.value)
    if (textRef.value) resizeObserver.observe(textRef.value)
  }
})

watch(() => props.text, () => {
  measureOverflow()
  if (isOpen.value) updatePopoverLayout()
})

watch(resolvedKey, (nextKey, prevKey) => {
  if (prevKey && prevKey !== nextKey && overflowCellRuntime.activeKey.value === prevKey) {
    overflowCellRuntime.activeKey.value = ''
  }
  if (nextKey && !isOverflow.value && overflowCellRuntime.activeKey.value === nextKey) {
    overflowCellRuntime.activeKey.value = ''
  }
})

onBeforeUnmount(() => {
  cancelRaf()
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  closeOpen()
  overflowCellRuntime.mountedCount = Math.max(0, overflowCellRuntime.mountedCount - 1)
  cleanupListeners()
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

:deep(.table-overflow-cell-popper) {
  min-width: 0 !important;
  max-width: min(420px, 88vw) !important;
  border-radius: 8px !important;
  border: 1px solid #d8e1ef !important;
  box-shadow: 0 10px 28px rgba(15, 39, 77, 0.18) !important;
  padding: 0 !important;
  background: #fff !important;
}

.table-overflow-cell__popover {
  position: relative;
  display: block;
  padding: 12px 52px 12px 12px;
  overflow: hidden;
  box-sizing: border-box;
}

.table-overflow-cell__popover-close {
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

.table-overflow-cell__popover-text {
  display: block;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: #334155;
  line-height: 1.55;
  max-height: 320px;
  overflow-y: auto;
  overflow-x: hidden;
}

@media (max-width: 768px) {
  .table-overflow-cell__popover-text {
    max-height: 50vh;
  }
}

@media (hover: none), (pointer: coarse) {
  .table-overflow-cell__toggle {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }
}
</style>
