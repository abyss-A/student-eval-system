import { computed, ref, unref, watch } from 'vue'

const normalizeSize = (value) => {
  const size = Number(value)
  if (!Number.isFinite(size) || size <= 0) return 10
  return Math.floor(size)
}

export default function useTablePager(rowsRef, initialPageSize = 10) {
  const page = ref(1)
  const pageSize = ref(normalizeSize(unref(initialPageSize)))

  watch(
    () => unref(initialPageSize),
    (next) => {
      pageSize.value = normalizeSize(next)
    }
  )

  const rows = computed(() => {
    const value = unref(rowsRef)
    return Array.isArray(value) ? value : []
  })

  const total = computed(() => rows.value.length)
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

  const clampPage = (value) => {
    const n = Number(value)
    if (!Number.isFinite(n)) return 1
    return Math.min(Math.max(Math.floor(n), 1), totalPages.value)
  }

  const goPage = (value) => {
    page.value = clampPage(value)
  }

  const jumpPage = (value) => {
    goPage(value)
  }

  const setPageSize = (size) => {
    const next = normalizeSize(size)
    if (next === pageSize.value) return
    pageSize.value = next
    page.value = 1
  }

  const goPrev = () => goPage(page.value - 1)
  const goNext = () => goPage(page.value + 1)
  const resetPage = () => goPage(1)

  const pagedRows = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return rows.value.slice(start, start + pageSize.value)
  })

  watch([rows, pageSize], () => {
    if (page.value > totalPages.value) {
      page.value = totalPages.value
    }
  })

  return {
    page,
    pageSize,
    total,
    totalPages,
    pagedRows,
    goPage,
    jumpPage,
    setPageSize,
    goPrev,
    goNext,
    resetPage
  }
}
