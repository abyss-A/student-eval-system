<template>
  <div class="table-footer pager-skin">
    <div class="table-pager-wrap">
      <div class="table-pager-actions">
        <el-button
          class="pager-btn pager-icon"
          type="default"
          :disabled="disabled || page <= 1"
          aria-label="上一页"
          title="上一页"
          @click="emit('change', page - 1)"
        >‹</el-button>

        <el-button
          v-for="p in displayPages"
          :key="p.key"
          class="pager-btn pager-page"
          type="default"
          :disabled="disabled || p.ellipsis || p.value === page"
          :class="p.value === page ? 'active' : ''"
          @click="!p.ellipsis && emit('change', p.value)"
        >
          {{ p.label }}
        </el-button>

        <el-button
          class="pager-btn pager-icon"
          type="default"
          :disabled="disabled || page >= totalPages"
          aria-label="下一页"
          title="下一页"
          @click="emit('change', page + 1)"
        >›</el-button>
      </div>

      <div v-if="showQuickJumper" class="table-pager-jumper">
        <span class="pager-label">到第</span>
        <el-input
          v-model="quickValue"
          class="pager-input"
          :disabled="disabled"
          inputmode="numeric"
          @keyup.enter="applyQuickJump"
        />
        <span class="pager-label">页</span>
        <el-button class="pager-confirm" type="default" :disabled="disabled" @click="applyQuickJump">确定</el-button>
      </div>

      <div class="pager-total">共 {{ total }} 条</div>

      <div class="table-pager-size">
        <el-select :model-value="currentPageSize" :disabled="disabled" @change="onPageSizeChange">
          <el-option v-for="size in normalizedPageSizeOptions" :key="`size_${size}`" :label="`${size} 条/页`" :value="size" />
        </el-select>
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
  showQuickJumper: { type: Boolean, default: true }
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

const onPageSizeChange = (nextValue) => {
  const next = Number(nextValue)
  if (!Number.isFinite(next) || next <= 0) return
  emit('page-size-change', Math.floor(next))
}

const displayPages = computed(() => {
  const total = Math.max(1, Number(props.totalPages) || 1)
  const current = Math.min(Math.max(Number(props.page) || 1, 1), total)
  const pages = []
  const pushPage = (value) => {
    if (pages.some((item) => item.value === value && !item.ellipsis)) return
    pages.push({ key: `p_${value}`, value, label: String(value), ellipsis: false })
  }
  const pushEllipsis = (key) => {
    if (pages.length && pages[pages.length - 1].ellipsis) return
    pages.push({ key, value: -1, label: '…', ellipsis: true })
  }

  if (total <= 7) {
    for (let p = 1; p <= total; p += 1) pushPage(p)
    return pages
  }

  pushPage(1)
  if (current > 4) pushEllipsis('left_ellipsis')

  const windowStart = Math.max(2, current - 1)
  const windowEnd = Math.min(total - 1, current + 1)
  for (let p = windowStart; p <= windowEnd; p += 1) pushPage(p)

  if (current < total - 3) pushEllipsis('right_ellipsis')
  pushPage(total)
  return pages
})
</script>

<style scoped>
.pager-skin {
  background: #f5f8fd;
}

.table-pager-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
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
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.pager-btn.pager-icon {
  width: 30px;
  min-width: 30px;
  padding: 0;
  font-size: 16px;
  font-weight: 400;
}

.pager-btn.pager-page {
  min-width: 38px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
}

.pager-btn.pager-page.active {
  background: #2f6db8;
  border-color: #2f6db8;
  color: #fff;
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
  min-width: 56px;
}

.pager-input:deep(.el-input__wrapper) {
  height: 30px;
  padding: 0 8px;
}

.pager-input:deep(.el-input__inner) {
  text-align: center;
}

.pager-confirm {
  min-width: 56px;
  font-size: 12px;
  font-weight: 400;
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

.table-pager-size :deep(.el-select) {
  width: 116px;
}

.table-pager-size :deep(.el-select__wrapper) {
  min-height: 30px;
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
  .pager-confirm {
    height: 30px;
    font-size: 12px;
  }

  .pager-label,
  .pager-total {
    font-size: 12px;
  }
}
</style>
