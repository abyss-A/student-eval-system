const STORAGE_PREFIX = 'table_col_widths:v1'
const MIN_COL_WIDTH = 72
const DESKTOP_MIN_WIDTH = 768

const enhancedTables = new WeakSet()

let observer = null
let scanRaf = 0

const isDesktopPointer = () => {
  if (typeof window === 'undefined') return false
  if (window.innerWidth < DESKTOP_MIN_WIDTH) return false
  if (typeof window.matchMedia !== 'function') return true
  return window.matchMedia('(pointer:fine)').matches
}

const clampWidth = (value) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return MIN_COL_WIDTH
  return Math.max(MIN_COL_WIDTH, Math.round(parsed))
}

const isFixedRightLockedHeader = (table, th) => {
  if (!table.classList.contains('table-fixed-right')) return false
  return th.classList.contains('col-status') || th.classList.contains('col-action')
}

const buildStorageKey = (table) => {
  const resizeKey = String(table.dataset.resizeKey || '').trim()
  if (!resizeKey) return ''
  return `${STORAGE_PREFIX}:${window.location.pathname}:${resizeKey}`
}

const readStoredWidths = (storageKey, count) => {
  if (!storageKey) return null
  try {
    const raw = localStorage.getItem(storageKey)
    if (!raw) return null
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr) || arr.length !== count) return null
    const widths = arr.map((item) => clampWidth(item))
    if (widths.some((item) => !Number.isFinite(item))) return null
    return widths
  } catch (_) {
    return null
  }
}

const persistWidths = (storageKey, cols) => {
  if (!storageKey) return
  try {
    const widths = cols.map((col) => {
      const current = parseFloat(col.style.width || '0')
      return clampWidth(current)
    })
    localStorage.setItem(storageKey, JSON.stringify(widths))
  } catch (_) {
    // localStorage unavailable or quota exceeded: ignore and keep runtime behavior.
  }
}

const ensureColgroup = (table, count) => {
  let colgroup = table.querySelector('colgroup[data-resize-colgroup="1"]')
  if (!colgroup) {
    colgroup = document.createElement('colgroup')
    colgroup.dataset.resizeColgroup = '1'
    table.insertBefore(colgroup, table.firstChild)
  }
  while (colgroup.children.length < count) {
    colgroup.appendChild(document.createElement('col'))
  }
  while (colgroup.children.length > count) {
    colgroup.removeChild(colgroup.lastElementChild)
  }
  return Array.from(colgroup.children)
}

const collectHeaderWidths = (ths) =>
  ths.map((th) => clampWidth(th.getBoundingClientRect().width || th.offsetWidth || MIN_COL_WIDTH))

const applyWidths = (cols, widths) => {
  cols.forEach((col, index) => {
    const width = clampWidth(widths[index])
    col.style.width = `${width}px`
  })
}

const createResizer = ({ table, th, col, storageKey }) => {
  th.classList.add('table-resize-th')

  const handle = document.createElement('span')
  handle.className = 'table-col-resizer'
  handle.setAttribute('aria-hidden', 'true')
  th.appendChild(handle)

  const onMouseDown = (event) => {
    if (event.button !== 0) return
    if (!isDesktopPointer()) return

    event.preventDefault()
    event.stopPropagation()

    const startX = event.clientX
    const startWidth = clampWidth(parseFloat(col.style.width || '0') || th.getBoundingClientRect().width)

    table.classList.add('is-resizing-columns')
    handle.classList.add('active')

    const prevUserSelect = document.body.style.userSelect
    document.body.style.userSelect = 'none'

    const onMouseMove = (moveEvent) => {
      const delta = moveEvent.clientX - startX
      const next = clampWidth(startWidth + delta)
      col.style.width = `${next}px`
    }

    const stopDrag = () => {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', stopDrag)
      handle.classList.remove('active')
      table.classList.remove('is-resizing-columns')
      document.body.style.userSelect = prevUserSelect
      persistWidths(storageKey, Array.from(table.querySelectorAll('colgroup[data-resize-colgroup="1"] > col')))
    }

    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', stopDrag)
  }

  handle.addEventListener('mousedown', onMouseDown)
}

const initTable = (table) => {
  if (!(table instanceof HTMLTableElement)) return
  if (enhancedTables.has(table)) return
  if (!isDesktopPointer()) return

  const ths = Array.from(table.querySelectorAll('thead th'))
  if (!ths.length) return

  const cols = ensureColgroup(table, ths.length)
  const storageKey = buildStorageKey(table)
  const lockedIndexes = new Set()
  ths.forEach((th, index) => {
    if (isFixedRightLockedHeader(table, th)) {
      lockedIndexes.add(index)
    }
  })

  const stored = readStoredWidths(storageKey, ths.length)
  const headerWidths = collectHeaderWidths(ths)
  const widths = stored
    ? headerWidths.map((fallbackWidth, index) => (
      lockedIndexes.has(index)
        ? fallbackWidth
        : clampWidth(stored[index] ?? fallbackWidth)
    ))
    : headerWidths
  applyWidths(cols, widths)

  table.classList.add('table-resizable')

  ths.forEach((th, index) => {
    if (lockedIndexes.has(index)) return
    createResizer({
      table,
      th,
      col: cols[index],
      storageKey
    })
  })

  enhancedTables.add(table)
}

const scanTables = () => {
  if (!isDesktopPointer()) return
  const tables = document.querySelectorAll('table.table')
  tables.forEach((table) => initTable(table))
}

const scheduleScan = () => {
  if (scanRaf) return
  scanRaf = window.requestAnimationFrame(() => {
    scanRaf = 0
    scanTables()
  })
}

export const initTableColumnResize = () => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  if (observer) return

  scanTables()

  observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === 'childList' && (mutation.addedNodes.length || mutation.removedNodes.length)) {
        scheduleScan()
        return
      }
    }
  })

  observer.observe(document.body, {
    childList: true,
    subtree: true
  })

  window.addEventListener('resize', scheduleScan, { passive: true })
}
