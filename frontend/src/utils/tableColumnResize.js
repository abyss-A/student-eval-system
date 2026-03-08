const STORAGE_PREFIX = 'table_col_widths:v1'
const MIN_COL_WIDTH = 72
const MIN_STATUS_COL_WIDTH = 94
const DEFAULT_ACTION_BTN_WIDTH = 72
const DEFAULT_ACTION_GAP = 4
const DEFAULT_ACTION_PADDING_X = 4
const DESKTOP_MIN_WIDTH = 768

const enhancedTables = new WeakSet()
const tableStates = new WeakMap()

let observer = null
let scanRaf = 0

const isDesktopPointer = () => {
  if (typeof window === 'undefined') return false
  if (window.innerWidth < DESKTOP_MIN_WIDTH) return false
  if (typeof window.matchMedia !== 'function') return true
  return window.matchMedia('(pointer:fine)').matches
}

const clampWidth = (value, minWidth = MIN_COL_WIDTH) => {
  const parsed = Number(value)
  const floor = Math.max(MIN_COL_WIDTH, Math.round(minWidth))
  if (!Number.isFinite(parsed)) return floor
  return Math.max(floor, Math.round(parsed))
}

const readCssPx = (table, varName, fallback) => {
  if (!table || typeof window === 'undefined') return fallback
  try {
    const raw = window.getComputedStyle(table).getPropertyValue(varName).trim()
    if (!raw) return fallback
    const value = parseFloat(raw)
    return Number.isFinite(value) ? value : fallback
  } catch (_) {
    return fallback
  }
}

const resolveMaxActionButtonCount = (table) => {
  const cells = table.querySelectorAll('tbody td.col-action')
  let maxCount = 1
  cells.forEach((cell) => {
    const count = cell.querySelectorAll('.el-button, .btn').length
    if (count > maxCount) {
      maxCount = count
    }
  })
  return maxCount
}

const resolveColumnMinWidth = (table, th) => {
  if (!th) return MIN_COL_WIDTH

  if (th.classList.contains('col-status')) {
    const configured = readCssPx(table, '--sticky-status-min-w', MIN_STATUS_COL_WIDTH)
    return Math.max(MIN_COL_WIDTH, configured)
  }

  if (th.classList.contains('col-action')) {
    const buttonWidth = readCssPx(table, '--fixed-action-btn-w', DEFAULT_ACTION_BTN_WIDTH)
    const buttonGap = readCssPx(table, '--fixed-action-gap', DEFAULT_ACTION_GAP)
    const paddingX = readCssPx(table, '--fixed-action-padding-x', DEFAULT_ACTION_PADDING_X)
    const buttonCount = Math.max(1, resolveMaxActionButtonCount(table))
    const autoMin = (buttonCount * buttonWidth) + ((buttonCount - 1) * buttonGap) + (paddingX * 2)
    const configured = readCssPx(table, '--sticky-action-min-w', autoMin)
    return Math.max(MIN_COL_WIDTH, configured, autoMin)
  }

  return MIN_COL_WIDTH
}

const buildStorageKey = (table) => {
  const resizeKey = String(table.dataset.resizeKey || '').trim()
  if (!resizeKey) return ''
  return `${STORAGE_PREFIX}:${window.location.pathname}:${resizeKey}`
}

const readStoredWidths = (storageKey, minWidths) => {
  if (!storageKey) return null
  const count = minWidths.length
  try {
    const raw = localStorage.getItem(storageKey)
    if (!raw) return null
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr) || arr.length !== count) return null
    const widths = arr.map((item, index) => clampWidth(item, minWidths[index]))
    if (widths.some((item) => !Number.isFinite(item))) return null
    return widths
  } catch (_) {
    return null
  }
}

const persistWidths = (storageKey, cols, minWidths) => {
  if (!storageKey) return
  try {
    const widths = cols.map((col, index) => {
      const current = parseFloat(col.style.width || '0')
      return clampWidth(current, minWidths[index])
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

const findHeaderIndex = (ths, className) => ths.findIndex((th) => th.classList.contains(className))

const resolveColumnWidth = (th, col, minWidth) => {
  const fromCol = parseFloat(col?.style?.width || '0')
  if (Number.isFinite(fromCol) && fromCol > 0) {
    return clampWidth(fromCol, minWidth)
  }
  return clampWidth(th?.getBoundingClientRect().width || th?.offsetWidth || minWidth, minWidth)
}

const syncFixedRightVars = (table, ths, cols, minWidths) => {
  if (!table.classList.contains('table-fixed-right')) return

  const actionIndex = findHeaderIndex(ths, 'col-action')
  const statusIndex = findHeaderIndex(ths, 'col-status')

  if (actionIndex >= 0) {
    const actionWidth = resolveColumnWidth(ths[actionIndex], cols[actionIndex], minWidths[actionIndex])
    table.style.setProperty('--sticky-action-w', `${actionWidth}px`)
  }

  if (statusIndex >= 0) {
    const statusWidth = resolveColumnWidth(ths[statusIndex], cols[statusIndex], minWidths[statusIndex])
    table.style.setProperty('--sticky-status-w', `${statusWidth}px`)
  }
}

const collectHeaderWidths = (ths, minWidths) =>
  ths.map((th, index) => clampWidth(th.getBoundingClientRect().width || th.offsetWidth || MIN_COL_WIDTH, minWidths[index]))

const applyWidths = (cols, widths, minWidths) => {
  let changed = false
  cols.forEach((col, index) => {
    const width = clampWidth(widths[index], minWidths[index])
    const prev = parseFloat(col.style.width || '0')
    if (!Number.isFinite(prev) || Math.abs(prev - width) > 0.5) {
      changed = true
      col.style.width = `${width}px`
    }
  })
  return changed
}

const computeMinWidths = (table, ths) => ths.map((th) => resolveColumnMinWidth(table, th))

const refreshTableState = (state, { persist = false } = {}) => {
  const { table, ths, cols } = state
  const minWidths = computeMinWidths(table, ths)
  state.minWidths = minWidths

  const currentWidths = cols.map((col, index) => {
    const current = parseFloat(col.style.width || '0')
    if (Number.isFinite(current) && current > 0) return current
    return ths[index].getBoundingClientRect().width || ths[index].offsetWidth || MIN_COL_WIDTH
  })

  const changed = applyWidths(cols, currentWidths, minWidths)
  syncFixedRightVars(table, ths, cols, minWidths)

  if (persist || changed) {
    persistWidths(state.storageKey, cols, minWidths)
  }
}

const createResizer = ({ state, index }) => {
  const { table, ths, cols, storageKey } = state
  const th = ths[index]
  const col = cols[index]

  th.classList.add('table-resize-th')

  const handle = document.createElement('span')
  handle.className = 'table-col-resizer'
  handle.setAttribute('aria-hidden', 'true')
  th.appendChild(handle)

  const getMinWidth = () => state.minWidths[index] || MIN_COL_WIDTH

  const onMouseDown = (event) => {
    if (event.button !== 0) return
    if (!isDesktopPointer()) return

    event.preventDefault()
    event.stopPropagation()

    const startX = event.clientX
    const startWidth = clampWidth(parseFloat(col.style.width || '0') || th.getBoundingClientRect().width, getMinWidth())

    table.classList.add('is-resizing-columns')
    handle.classList.add('active')

    const prevUserSelect = document.body.style.userSelect
    document.body.style.userSelect = 'none'

    const onMouseMove = (moveEvent) => {
      const delta = moveEvent.clientX - startX
      const next = clampWidth(startWidth + delta, getMinWidth())
      col.style.width = `${next}px`
      syncFixedRightVars(table, state.ths, state.cols, state.minWidths)
    }

    const stopDrag = () => {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', stopDrag)
      handle.classList.remove('active')
      table.classList.remove('is-resizing-columns')
      document.body.style.userSelect = prevUserSelect
      refreshTableState(state, { persist: true })
    }

    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', stopDrag)
  }

  handle.addEventListener('mousedown', onMouseDown)
}

const hasHeaderChanged = (state, latestThs) => {
  if (state.ths.length !== latestThs.length) return true
  for (let i = 0; i < latestThs.length; i += 1) {
    if (state.ths[i] !== latestThs[i]) return true
  }
  return false
}

const initTable = (table) => {
  if (!(table instanceof HTMLTableElement)) return
  if (!isDesktopPointer()) return

  const ths = Array.from(table.querySelectorAll('thead th'))
  if (!ths.length) return

  const cols = ensureColgroup(table, ths.length)
  const minWidths = computeMinWidths(table, ths)
  const storageKey = buildStorageKey(table)

  const stored = readStoredWidths(storageKey, minWidths)
  const headerWidths = collectHeaderWidths(ths, minWidths)
  const initialWidths = stored
    ? headerWidths.map((fallbackWidth, index) => clampWidth(stored[index] ?? fallbackWidth, minWidths[index]))
    : headerWidths

  applyWidths(cols, initialWidths, minWidths)

  const state = {
    table,
    ths,
    cols,
    storageKey,
    minWidths
  }

  table.classList.add('table-resizable')
  ths.forEach((_, index) => createResizer({ state, index }))

  tableStates.set(table, state)
  enhancedTables.add(table)
  refreshTableState(state, { persist: true })
}

const refreshTable = (table) => {
  const state = tableStates.get(table)
  if (!state) {
    initTable(table)
    return
  }

  const latestThs = Array.from(table.querySelectorAll('thead th'))
  if (!latestThs.length) return

  if (hasHeaderChanged(state, latestThs)) {
    tableStates.delete(table)
    enhancedTables.delete(table)
    initTable(table)
    return
  }

  state.cols = ensureColgroup(table, latestThs.length)
  refreshTableState(state)
}

const scanTables = () => {
  if (!isDesktopPointer()) return
  const tables = document.querySelectorAll('table.table')
  tables.forEach((table) => {
    if (!enhancedTables.has(table)) {
      initTable(table)
      return
    }
    refreshTable(table)
  })
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
