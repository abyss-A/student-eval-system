import { computed, ref } from 'vue'

const normalizeKey = (key) => String(key ?? '').trim()

export default function useTableSelection() {
  const selectedKeys = ref(new Set())

  const selectedCount = computed(() => selectedKeys.value.size)
  const selectedList = computed(() => Array.from(selectedKeys.value))

  const isSelected = (key) => {
    const normalized = normalizeKey(key)
    if (!normalized) return false
    return selectedKeys.value.has(normalized)
  }

  const toggle = (key) => {
    const normalized = normalizeKey(key)
    if (!normalized) return
    const next = new Set(selectedKeys.value)
    if (next.has(normalized)) next.delete(normalized)
    else next.add(normalized)
    selectedKeys.value = next
  }

  const clear = () => {
    if (!selectedKeys.value.size) return
    selectedKeys.value = new Set()
  }

  const toggleAll = (keysOnPage) => {
    const keys = Array.isArray(keysOnPage)
      ? keysOnPage.map((item) => normalizeKey(item)).filter(Boolean)
      : []

    if (!keys.length) {
      clear()
      return
    }

    const next = new Set(selectedKeys.value)
    const allChecked = keys.every((key) => next.has(key))
    if (allChecked) {
      keys.forEach((key) => next.delete(key))
    } else {
      keys.forEach((key) => next.add(key))
    }
    selectedKeys.value = next
  }

  const isAllCheckedOnPage = (keysOnPage) => {
    const keys = Array.isArray(keysOnPage)
      ? keysOnPage.map((item) => normalizeKey(item)).filter(Boolean)
      : []
    if (!keys.length) return false
    return keys.every((key) => selectedKeys.value.has(key))
  }

  const isIndeterminateOnPage = (keysOnPage) => {
    const keys = Array.isArray(keysOnPage)
      ? keysOnPage.map((item) => normalizeKey(item)).filter(Boolean)
      : []
    if (!keys.length) return false
    const checked = keys.filter((key) => selectedKeys.value.has(key)).length
    return checked > 0 && checked < keys.length
  }

  return {
    selectedKeys,
    selectedList,
    selectedCount,
    isSelected,
    toggle,
    toggleAll,
    clear,
    isAllCheckedOnPage,
    isIndeterminateOnPage
  }
}
