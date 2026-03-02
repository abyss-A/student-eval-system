<template>
  <div class="table-footer pager-skin">
    <div class="table-pager-wrap">
      <div class="table-pager-actions">
        <button
          v-if="showBoundaryButtons"
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page <= 1"
          aria-label="首页"
          title="首页"
          @click="emit('change', 1)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M18 6l-6 6 6 6M12 6l-6 6 6 6" />
          </svg>
        </button>

        <button
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page <= 1"
          aria-label="上一页"
          title="上一页"
          @click="emit('change', page - 1)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>

        <button
          v-for="p in displayPages"
          :key="p.key"
          class="pager-btn pager-page"
          type="button"
          :disabled="disabled || p.ellipsis || p.value === page"
          :class="p.value === page ? 'active' : ''"
          @click="!p.ellipsis && emit('change', p.value)"
        >
          {{ p.label }}
        </button>

        <button
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page >= totalPages"
          aria-label="下一页"
          title="下一页"
          @click="emit('change', page + 1)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>

        <button
          v-if="showBoundaryButtons"
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page >= totalPages"
          aria-label="末页"
          title="末页"
          @click="emit('change', totalPages)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6l6 6-6 6M12 6l6 6-6 6" />
          </svg>
        </button>
      </div>

      <div v-if="showQuickJumper" class="table-pager-jumper">
        <span class="pager-label">到第</span>
        <input
          v-model="quickValue"
          class="pager-input"
          type="number"
          min="1"
          :max="Math.max(1, totalPages)"
          :disabled="disabled"
          @keyup.enter="applyQuickJump"
        />
        <span class="pager-label">页</span>
        <button class="pager-confirm" type="button" :disabled="disabled" @click="applyQuickJump">确定</button>
      </div>

      <div class="pager-total">共 {{ total }} 条</div>

      <div class="table-pager-size">
        <select :value="currentPageSize" :disabled="disabled" @change="onPageSizeChange">
          <option v-for="size in normalizedPageSizeOptions" :key="`size_${size}`" :value="size">{{ size }} 条/页</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  total: { type: Number, required: true },
  disabled: { type: Boolean, default: false },
  pageSize: { type: Number, default: 10 },
  pageSizeOptions: { type: Array, default: () => [10, 20, 50] },
  showQuickJumper: { type: Boolean, default: true },
  showBoundaryButtons: { type: Boolean, default: false }
})

const emit = defineEmits(['change', 'page-size-change', 'quick-jump'])

const quickValue = ref(String(props.page || 1))

watch(
  () => props.page,
  (next) => {
    quickValue.value = String(next || 1)
  }
)

const normalizedPageSizeOptions = computed(() => {
  const out = []
  for (const value of props.pageSizeOptions || []) {
    const size = Number(value)
    if (!Number.isFinite(size) || size <= 0) continue
    const normalized = Math.floor(size)
    if (!out.includes(normalized)) out.push(normalized)
  }
  return out.length ? out : [10, 20, 50]
})

const currentPageSize = computed(() => {
  const size = Number(props.pageSize)
  if (!Number.isFinite(size) || size <= 0) return normalizedPageSizeOptions.value[0]
  return Math.floor(size)
})

const applyQuickJump = () => {
  const raw = String(quickValue.value || '').trim()
  const parsed = Number.parseInt(raw, 10)
  const totalPages = Math.max(1, Number(props.totalPages) || 1)
  const target = Number.isFinite(parsed)
    ? Math.min(Math.max(parsed, 1), totalPages)
    : Math.min(Math.max(Number(props.page) || 1, 1), totalPages)

  quickValue.value = String(target)
  emit('quick-jump', target)
  emit('change', target)
}

const onPageSizeChange = (event) => {
  const next = Number(event?.target?.value)
  if (!Number.isFinite(next) || next <= 0) return
  emit('page-size-change', Math.floor(next))
}

const displayPages = computed(() => {
  const pages = []
  const total = Math.max(1, Number(props.totalPages) || 1)
  const current = Math.min(Math.max(Number(props.page) || 1, 1), total)

  const pushPage = (value) => {
    pages.push({ key: `p_${value}`, value, label: String(value), ellipsis: false })
  }
  const pushEllipsis = (key) => {
    pages.push({ key: `e_${key}`, value: -1, label: '...', ellipsis: true })
  }

  if (total <= 7) {
    for (let i = 1; i <= total; i += 1) pushPage(i)
    return pages
  }

  pushPage(1)
  if (current > 4) pushEllipsis('l')

  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let i = start; i <= end; i += 1) pushPage(i)

  if (current < total - 3) pushEllipsis('r')
  pushPage(total)
  return pages
})
</script>

<style scoped>
.pager-skin {
  background: #f6f7f9;
}

.table-pager-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-pager-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pager-btn {
  height: 38px;
  min-width: 38px;
  padding: 0 12px;
  border: 1px solid #d2d8e1;
  border-radius: 6px;
  background: #ffffff;
  color: #4a5568;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.12s ease, border-color 0.12s ease;
}

.pager-btn:hover:not(:disabled) {
  border-color: #bac4d2;
  background: #fdfefe;
}

.pager-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.pager-btn.pager-icon {
  width: 38px;
  padding: 0;
}

.pager-btn.pager-icon svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.pager-btn.pager-page {
  min-width: 42px;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.pager-btn.pager-page.active {
  background: #6f8fb6;
  border-color: #6f8fb6;
  color: #ffffff;
  opacity: 1;
}

.table-pager-jumper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pager-label {
  color: #4b5563;
  font-size: 14px;
}

.pager-input {
  width: 72px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid #d2d8e1;
  border-radius: 6px;
  background: #fff;
  font-size: 14px;
}

.pager-input:focus {
  outline: none;
  border-color: #9db3d4;
  box-shadow: 0 0 0 3px rgba(111, 143, 182, 0.15);
}

.pager-confirm {
  height: 38px;
  min-width: 66px;
  padding: 0 16px;
  border: 1px solid #d2d8e1;
  border-radius: 6px;
  background: #fff;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.pager-confirm:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.pager-total {
  color: #111827;
  font-size: 14px;
  white-space: nowrap;
}

.table-pager-size {
  display: inline-flex;
  align-items: center;
}

.table-pager-size select {
  width: 160px;
  height: 38px;
  padding: 0 34px 0 12px;
  border: 1px solid #d2d8e1;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
}

@media (max-width: 900px) {
  .table-pager-wrap {
    gap: 8px;
  }

  .table-pager-actions,
  .table-pager-jumper {
    gap: 6px;
  }

  .pager-btn,
  .pager-confirm,
  .pager-input,
  .table-pager-size select {
    height: 34px;
    font-size: 13px;
  }

  .pager-label,
  .pager-total {
    font-size: 13px;
  }

  .table-pager-size select {
    width: 128px;
  }
}
</style>
