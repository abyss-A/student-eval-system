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
        >«</button>

        <button
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page <= 1"
          aria-label="上一页"
          title="上一页"
          @click="emit('change', page - 1)"
        >‹</button>

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
        >›</button>

        <button
          v-if="showBoundaryButtons"
          class="pager-btn pager-icon"
          type="button"
          :disabled="disabled || page >= totalPages"
          aria-label="末页"
          title="末页"
          @click="emit('change', totalPages)"
        >»</button>
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
  const total = Math.max(1, Number(props.totalPages) || 1)
  const current = Math.min(Math.max(Number(props.page) || 1, 1), total)
  return [{ key: `p_${current}`, value: current, label: String(current), ellipsis: false }]
})
</script>

<style scoped>
.pager-skin {
  background: #edf0f4;
}

.table-pager-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  overflow-x: auto;
  padding: 1px 0;
}

.table-pager-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.pager-btn {
  height: 30px;
  min-width: 30px;
  padding: 0 8px;
  border: 1px solid #d2d9e3;
  border-radius: 5px;
  background: #f8fafc;
  color: #99a3b4;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  vertical-align: middle;
  white-space: nowrap;
  transition: background 0.12s ease, border-color 0.12s ease, color 0.12s ease, box-shadow 0.12s ease;
  box-shadow: none;
}

.pager-btn:hover:not(:disabled) {
  border-color: #bec8d8;
  background: #f3f6fa;
  color: #5f6c82;
}

.pager-btn:disabled {
  opacity: 1;
  cursor: not-allowed;
  color: #c1c8d4;
  background: #f4f6f9;
}

.pager-btn.pager-icon {
  width: 30px;
  min-width: 30px;
  height: 30px;
  padding: 0;
  font-size: 16px;
  font-weight: 400;
}

.pager-btn.pager-page {
  min-width: 38px;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
}

.pager-btn.pager-page.active {
  background: #7395c0;
  border-color: #7395c0;
  color: #ffffff;
  opacity: 1;
  box-shadow: none;
}

.table-pager-jumper {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
}

.pager-label {
  color: #606a7a;
  font-size: 12px;
}

.pager-input {
  width: 56px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #d2d9e3;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #111827;
  vertical-align: middle;
}

.pager-input:focus {
  outline: none;
  border-color: #b6c2d4;
  box-shadow: none;
}

.pager-confirm {
  height: 30px;
  min-width: 56px;
  padding: 0 12px;
  border: 1px solid #d2d9e3;
  border-radius: 4px;
  background: #fff;
  color: #0f172a;
  font-size: 12px;
  font-family: inherit;
  font-weight: 400;
  cursor: pointer;
  flex: 0 0 auto;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  vertical-align: middle;
  white-space: nowrap;
}

.pager-confirm:hover:not(:disabled) {
  border-color: #bec8d8;
  background: #f8fafe;
}

.pager-confirm:disabled {
  opacity: 1;
  color: #9ca3af;
  background: #f3f5f8;
  cursor: not-allowed;
}

.pager-total {
  color: #1f2937;
  font-size: 12px;
  white-space: nowrap;
  flex: 0 0 auto;
}

.table-pager-size {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}

.table-pager-size select {
  width: 112px;
  height: 30px;
  padding: 0 26px 0 8px;
  border: 1px solid #d2d9e3;
  border-radius: 5px;
  background: #fff;
  font-size: 12px;
  color: #111827;
  vertical-align: middle;
}

.table-pager-size select:focus {
  outline: none;
  border-color: #b6c2d4;
  box-shadow: none;
}

@media (max-width: 900px) {
  .table-pager-wrap {
    gap: 8px;
    flex-wrap: wrap;
  }

  .table-pager-actions,
  .table-pager-jumper {
    gap: 6px;
  }

  .pager-btn,
  .pager-confirm,
  .pager-input,
  .table-pager-size select {
    height: 30px;
    font-size: 12px;
  }

  .pager-label,
  .pager-total {
    font-size: 12px;
  }

  .table-pager-size select {
    width: 112px;
  }
}
</style>
