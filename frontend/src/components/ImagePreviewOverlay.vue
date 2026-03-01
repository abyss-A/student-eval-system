<template>
  <div v-if="state.visible" class="image-preview-overlay" @click.self="close">
    <div class="image-preview-panel" role="dialog" aria-modal="true" aria-label="图片预览">
      <div class="image-preview-header">
        <div>
          <div class="image-preview-title">{{ state.title || '图片预览' }}</div>
          <p class="muted" style="margin-top: 4px;">
            {{ fileCaption }}
            <span v-if="showSwitcher" style="margin-left: 8px;">{{ pageCaption }}</span>
          </p>
        </div>
        <div class="image-preview-tools">
          <button
            class="image-preview-tool-btn"
            type="button"
            aria-label="缩小"
            title="缩小"
            :disabled="state.loading || Boolean(state.error)"
            @click="zoomOut"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 12h12" />
            </svg>
          </button>
          <button
            class="image-preview-tool-btn"
            type="button"
            aria-label="重置缩放"
            title="重置缩放"
            :disabled="state.loading || Boolean(state.error)"
            @click="resetScale"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 6v12M6 12h12" />
              <circle cx="12" cy="12" r="7.25" fill="none" />
            </svg>
          </button>
          <button
            class="image-preview-tool-btn"
            type="button"
            aria-label="放大"
            title="放大"
            :disabled="state.loading || Boolean(state.error)"
            @click="zoomIn"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 12h12M12 6v12" />
            </svg>
          </button>
          <button
            class="image-preview-tool-btn danger"
            type="button"
            aria-label="关闭预览"
            title="关闭"
            @click="close"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 7l10 10M17 7L7 17" />
            </svg>
          </button>
        </div>
      </div>

      <div class="image-preview-body" :class="{ 'has-switcher': showSwitcher }" @wheel.prevent="onWheel">
        <button
          v-if="showSwitcher"
          class="image-preview-nav prev"
          type="button"
          aria-label="上一张"
          :disabled="!canPrev"
          @click="prev"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M14.5 5.5L8 12l6.5 6.5" />
          </svg>
        </button>

        <div
          ref="canvasRef"
          class="image-preview-canvas"
          :class="{ 'can-pan': canPan, 'is-panning': dragging }"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerEnd"
          @pointercancel="onPointerEnd"
          @lostpointercapture="onPointerEnd"
        >
          <div v-if="state.loading" class="image-preview-message">图片加载中...</div>
          <div v-else-if="state.error" class="image-preview-message error">{{ state.error }}</div>
          <img
            v-else-if="state.imageUrl"
            ref="imageRef"
            :src="state.imageUrl"
            alt="预览图片"
            class="image-preview-image"
            :style="imageStyle"
            draggable="false"
            @dragstart.prevent
            @load="onImageLoad"
          />
          <div v-else class="image-preview-message">暂无可预览图片</div>
        </div>

        <button
          v-if="showSwitcher"
          class="image-preview-nav next"
          type="button"
          aria-label="下一张"
          :disabled="!canNext"
          @click="next"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9.5 5.5L16 12l-6.5 6.5" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useImagePreview } from '../composables/useImagePreview'

const preview = useImagePreview()
const { state } = preview

const canvasRef = ref(null)
const imageRef = ref(null)

const panX = ref(0)
const panY = ref(0)
const maxPanX = ref(0)
const maxPanY = ref(0)
const dragging = ref(false)
const dragPointerId = ref(null)
const startX = ref(0)
const startY = ref(0)
const basePanX = ref(0)
const basePanY = ref(0)

const showSwitcher = computed(() => state.galleryIds.length > 1)
const canPrev = computed(() => preview.canPrev())
const canNext = computed(() => preview.canNext())
const canPan = computed(() => state.scale > 1 && !state.loading && !state.error && Boolean(state.imageUrl))
const pageCaption = computed(() => `${state.currentIndex + 1} / ${state.galleryIds.length}`)
const fileCaption = computed(() => state.fileName || (state.currentId ? `附件#${state.currentId}` : ''))

const imageStyle = computed(() => ({
  transform: `translate3d(${panX.value}px, ${panY.value}px, 0) scale(${state.scale})`
}))

let prevBodyOverflow = ''

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

const recalcPanBoundary = () => {
  const canvas = canvasRef.value
  const image = imageRef.value
  if (!canvas || !image) {
    maxPanX.value = 0
    maxPanY.value = 0
    return
  }

  const canvasW = canvas.clientWidth
  const canvasH = canvas.clientHeight
  const imageW = image.offsetWidth
  const imageH = image.offsetHeight
  const scale = Number(state.scale) || 1

  maxPanX.value = Math.max(0, (imageW * scale - canvasW) / 2)
  maxPanY.value = Math.max(0, (imageH * scale - canvasH) / 2)

  panX.value = clamp(panX.value, -maxPanX.value, maxPanX.value)
  panY.value = clamp(panY.value, -maxPanY.value, maxPanY.value)
}

const resetPan = () => {
  panX.value = 0
  panY.value = 0
  maxPanX.value = 0
  maxPanY.value = 0
}

const close = () => {
  preview.close()
}

const prev = async () => {
  await preview.prev()
}

const next = async () => {
  await preview.next()
}

const zoomIn = () => preview.zoomIn()
const zoomOut = () => preview.zoomOut()

const resetScale = () => {
  preview.resetScale()
  resetPan()
}

const onWheel = (e) => {
  if (state.loading || state.error) return
  if (e.deltaY < 0) {
    preview.zoomIn()
    return
  }
  preview.zoomOut()
}

const onPointerDown = (e) => {
  if (!canPan.value) return
  if (e.button !== 0) return

  const canvas = canvasRef.value
  if (!canvas) return

  dragging.value = true
  dragPointerId.value = e.pointerId
  startX.value = e.clientX
  startY.value = e.clientY
  basePanX.value = panX.value
  basePanY.value = panY.value
  canvas.setPointerCapture(e.pointerId)
}

const onPointerMove = (e) => {
  if (!dragging.value) return
  if (dragPointerId.value !== null && e.pointerId !== dragPointerId.value) return

  const nextX = basePanX.value + (e.clientX - startX.value)
  const nextY = basePanY.value + (e.clientY - startY.value)
  panX.value = clamp(nextX, -maxPanX.value, maxPanX.value)
  panY.value = clamp(nextY, -maxPanY.value, maxPanY.value)
}

const onPointerEnd = (e) => {
  if (!dragging.value) return
  if (dragPointerId.value !== null && e.pointerId !== dragPointerId.value) return

  const canvas = canvasRef.value
  if (canvas && dragPointerId.value !== null && canvas.hasPointerCapture(dragPointerId.value)) {
    canvas.releasePointerCapture(dragPointerId.value)
  }
  dragging.value = false
  dragPointerId.value = null
}

const onImageLoad = () => {
  resetPan()
  nextTick(() => {
    recalcPanBoundary()
  })
}

const handleKeydown = async (e) => {
  if (!state.visible) return

  if (e.key === 'Escape') {
    e.preventDefault()
    close()
    return
  }
  if (e.key === 'ArrowLeft' && showSwitcher.value) {
    e.preventDefault()
    await prev()
    return
  }
  if (e.key === 'ArrowRight' && showSwitcher.value) {
    e.preventDefault()
    await next()
  }
}

const handleResize = () => {
  if (!state.visible) return
  recalcPanBoundary()
}

watch(
  () => state.visible,
  (visible) => {
    if (visible) {
      prevBodyOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      nextTick(() => recalcPanBoundary())
      return
    }
    document.body.style.overflow = prevBodyOverflow || ''
    dragging.value = false
    dragPointerId.value = null
    resetPan()
  }
)

watch(
  () => state.scale,
  (scale) => {
    if (!state.visible) return
    if (!scale || scale <= 1) {
      resetPan()
      return
    }
    nextTick(() => recalcPanBoundary())
  }
)

watch(
  () => state.currentId,
  () => {
    resetPan()
  }
)

watch(
  () => state.imageUrl,
  () => {
    if (!state.imageUrl) {
      resetPan()
    }
  }
)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleResize)
  document.body.style.overflow = prevBodyOverflow || ''
})
</script>

