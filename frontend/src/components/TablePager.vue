<template>
  <div class="table-footer">
    <div class="muted">共 {{ total }} 条</div>
    <div class="toolbar-row" style="gap: 6px;">
      <button class="btn secondary" type="button" :disabled="disabled || page <= 1" @click="$emit('change', page - 1)">
        上一页
      </button>
      <button
        v-for="p in displayPages"
        :key="p.key"
        class="btn secondary"
        type="button"
        :disabled="disabled || p.ellipsis || p.value === page"
        :class="p.value === page ? 'pager-active' : ''"
        @click="!p.ellipsis && $emit('change', p.value)"
      >
        {{ p.label }}
      </button>
      <button class="btn secondary" type="button" :disabled="disabled || page >= totalPages" @click="$emit('change', page + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  total: { type: Number, required: true },
  disabled: { type: Boolean, default: false }
})

defineEmits(['change'])

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
.pager-active {
  background: #e9f2ff !important;
  border-color: #bcd6ff !important;
  color: #0b3a82 !important;
  font-weight: 700;
}
</style>
