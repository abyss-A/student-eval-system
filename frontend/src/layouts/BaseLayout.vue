<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div
        class="sidebar-brand"
        role="button"
        tabindex="0"
        @click="goHome"
        @keydown.enter.prevent="goHome"
        @keydown.space.prevent="goHome"
      >
        <div class="brand-mark">综测</div>
        <div class="brand-text">
          <div class="brand-name">{{ appName }}</div>
        </div>
      </div>

      <nav class="menu">
        <section v-for="g in menuGroups" :key="g.title" class="menu-group">
          <button class="menu-group-title" type="button" @click="toggleGroup(g.title)">
            <span>{{ g.title }}</span>
            <span class="chev" :class="{ open: isGroupOpen(g.title) }">▼</span>
          </button>
          <div v-show="isGroupOpen(g.title)" class="menu-items">
            <RouterLink v-for="item in g.items" :key="item.to" :to="item.to" class="menu-item">
              {{ item.label }}
            </RouterLink>
          </div>
        </section>
      </nav>
    </aside>

    <div class="main">
      <header class="workspace-tools-bar">
        <div class="workspace-tools-left">
          <el-button
            class="workspace-tool-btn"
            type="default"
            circle
            aria-label="切换侧边栏"
            title="切换侧边栏"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <el-icon><Fold /></el-icon>
          </el-button>
          <el-button
            class="workspace-tool-btn"
            type="default"
            circle
            aria-label="刷新当前页面"
            title="刷新当前页面"
            @click="refreshCurrentView"
          >
            <el-icon><Refresh /></el-icon>
          </el-button>
          <div class="workspace-title">{{ pageTitle }}</div>
        </div>

        <div class="workspace-tools-right">
          <el-button class="user-pill user-pill-btn" type="default" @click="goProfile" aria-label="打开账号中心">
            <span class="user-name">{{ state.realName || '未命名用户' }}</span>
            <span class="user-sep">·</span>
            <span class="user-role">{{ roleText }}</span>
          </el-button>
          <el-button class="btn ghost" type="default" @click="logout">退出登录</el-button>
        </div>
      </header>

      <div class="workspace-tabs-bar">
        <div class="workspace-tab-scroll">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="workspace-tab"
            :class="{ active: tab.key === activeTabKey }"
            role="button"
            tabindex="0"
            @click="activateTab(tab.key)"
            @keydown.enter.prevent="activateTab(tab.key)"
            @keydown.space.prevent="activateTab(tab.key)"
          >
            <span class="workspace-tab-text">{{ tab.title }}</span>
            <button
              v-if="tab.closable"
              class="workspace-tab-close"
              type="button"
              aria-label="关闭标签"
              @click.stop="closeTab(tab.key)"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <main class="content">
        <RouterView :key="contentViewKey" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { isNavigationFailure, useRoute, useRouter } from 'vue-router'
import { Fold, Refresh } from '@element-plus/icons-vue'
import { clearAuth, getHomeByRole, getProfilePathByRole, getRealName, getRole, roleLabel } from '../utils/auth'
import submissionStore from '../stores/submissionStore'

const props = defineProps({
  appName: {
    type: String,
    default: '大学生综合测评管理系统'
  },
  menuGroups: {
    type: Array,
    required: true
  }
})

const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)
const openGroups = reactive({})
const tabs = ref([])
const activeTabKey = ref('')
const viewReloadSeed = ref(0)

const state = reactive({
  role: '',
  realName: ''
})

const syncAuth = () => {
  state.role = getRole()
  state.realName = getRealName()
}

const homePath = computed(() => getHomeByRole(state.role))

const homeTabTitle = computed(() => {
  const resolved = router.resolve(homePath.value || '/')
  const title = resolved.meta?.tabTitle || resolved.meta?.title
  if (typeof title === 'string' && title.trim()) return title.trim()
  return '首页'
})

const roleText = computed(() => roleLabel(state.role))

const pageTitle = computed(() => {
  const title = route.meta?.title
  if (typeof title === 'string' && title.trim()) return title
  return '综合测评'
})

const contentViewKey = computed(() => `${route.path}::${viewReloadSeed.value}`)

const resolveTabTitle = (currentRoute) => {
  const metaTitle = currentRoute.meta?.tabTitle || currentRoute.meta?.title
  const base = typeof metaTitle === 'string' && metaTitle.trim() ? metaTitle.trim() : '页面'
  const id = currentRoute.params?.id
  if (id !== undefined && id !== null && base.includes('详情')) {
    return `${base} #${id}`
  }
  return base
}

const ensureHomeTab = () => {
  const key = homePath.value
  if (!key || key === '/login') return
  const idx = tabs.value.findIndex((tab) => tab.key === key)
  const homeTab = {
    key,
    to: key,
    title: homeTabTitle.value,
    closable: false,
    createdAt: 0
  }

  if (idx >= 0) {
    tabs.value[idx] = homeTab
    return
  }
  tabs.value.unshift(homeTab)
}

const pruneTabs = (max = 8) => {
  while (tabs.value.length > max) {
    let removeIdx = -1
    let oldest = Number.POSITIVE_INFINITY
    tabs.value.forEach((tab, idx) => {
      if (!tab.closable) return
      if (tab.createdAt < oldest) {
        oldest = tab.createdAt
        removeIdx = idx
      }
    })
    if (removeIdx < 0) break
    const removed = tabs.value.splice(removeIdx, 1)[0]
    if (removed?.key === activeTabKey.value) {
      activeTabKey.value = homePath.value
    }
  }
}

const syncTabByRoute = (currentRoute) => {
  ensureHomeTab()

  const key = currentRoute.path
  const idx = tabs.value.findIndex((tab) => tab.key === key)
  if (idx >= 0) {
    tabs.value[idx] = {
      ...tabs.value[idx],
      to: key,
      title: resolveTabTitle(currentRoute)
    }
  } else {
    tabs.value.push({
      key,
      to: key,
      title: resolveTabTitle(currentRoute),
      closable: key !== homePath.value,
      createdAt: Date.now()
    })
    pruneTabs(8)
  }

  activeTabKey.value = key
}

const isGroupOpen = (title) => openGroups[title] !== false
const toggleGroup = (title) => {
  openGroups[title] = !isGroupOpen(title)
}

const activateTab = async (key) => {
  if (!key || key === activeTabKey.value) return
  const tab = tabs.value.find((item) => item.key === key)
  if (!tab) return
  await router.push(tab.to)
}

const closeTab = async (key) => {
  const idx = tabs.value.findIndex((tab) => tab.key === key)
  if (idx < 0) return
  const tab = tabs.value[idx]
  if (!tab.closable) return

  if (key !== activeTabKey.value) {
    tabs.value.splice(idx, 1)
    return
  }

  const right = tabs.value[idx + 1]
  const left = tabs.value[idx - 1]
  const fallback = tabs.value.find((item) => item.key === homePath.value)
  const target = right || left || fallback
  if (!target) return

  const result = await router.push(target.to)
  if (isNavigationFailure(result)) {
    return
  }

  tabs.value = tabs.value.filter((item) => item.key !== key)
  activeTabKey.value = target.key
}

const refreshCurrentView = () => {
  viewReloadSeed.value += 1
}

const goHome = () => {
  router.push(getHomeByRole())
}

const goProfile = () => {
  router.push(getProfilePathByRole(state.role))
}

const logout = () => {
  submissionStore.reset()
  clearAuth()
  router.replace('/login')
}

syncAuth()

const resolveMenuItemPath = (to) => {
  if (!to) return ''
  if (typeof to === 'string') return to
  if (typeof to === 'object' && typeof to.path === 'string') return to.path
  return ''
}

const matchGroupTitleByPath = (path, groups = props.menuGroups) => {
  const current = String(path || '').trim()
  if (!current) return ''

  let bestTitle = ''
  let bestLength = -1

  for (const group of groups || []) {
    const title = String(group?.title || '').trim()
    if (!title) continue
    const items = Array.isArray(group?.items) ? group.items : []

    for (const item of items) {
      const to = resolveMenuItemPath(item?.to)
      if (!to || to === '/') continue

      const candidates = [to]
      const splitIdx = to.lastIndexOf('/')
      if (splitIdx > 0) {
        const base = to.slice(0, splitIdx)
        if (base && base !== '/') candidates.push(base)
      }

      for (const candidate of candidates) {
        if (!candidate) continue
        const matched = current === candidate || current.startsWith(`${candidate}/`)
        if (!matched) continue
        if (candidate.length > bestLength) {
          bestLength = candidate.length
          bestTitle = title
        }
      }
    }
  }

  return bestTitle
}

watch(
  () => props.menuGroups,
  (groups) => {
    ;(groups || []).forEach((g) => {
      if (g?.title && openGroups[g.title] === undefined) {
        if (typeof g.defaultOpen === 'boolean') {
          openGroups[g.title] = g.defaultOpen
        } else {
          openGroups[g.title] = true
        }
      }
    })
  },
  { immediate: true, deep: true }
)

watch(
  () => route.fullPath,
  () => {
    syncAuth()
    syncTabByRoute(route)
    const groupTitle = matchGroupTitleByPath(route.path)
    if (groupTitle) {
      openGroups[groupTitle] = true
    }
  },
  { immediate: true }
)
</script>
