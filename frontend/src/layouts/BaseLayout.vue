<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-brand" @click="goHome" role="button" tabindex="0">
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
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn" type="button" @click="sidebarCollapsed = !sidebarCollapsed" aria-label="切换侧边栏">
            ☰
          </button>
          <div class="topbar-title">{{ pageTitle }}</div>
        </div>
        <div class="topbar-right">
          <div class="user-pill">
            <span class="user-name">{{ state.realName || '未命名用户' }}</span>
            <span class="user-sep">·</span>
            <span class="user-role">{{ roleText }}</span>
          </div>
          <button class="btn ghost" type="button" @click="logout">退出登录</button>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getHomeByRole, getRealName, getRole, roleLabel } from '../utils/auth'
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

const state = reactive({
  role: '',
  realName: ''
})

const syncAuth = () => {
  state.role = getRole()
  state.realName = getRealName()
}

syncAuth()
watch(() => route.fullPath, syncAuth)

watch(
  () => props.menuGroups,
  (groups) => {
    ;(groups || []).forEach((g) => {
      if (g?.title && openGroups[g.title] === undefined) {
        openGroups[g.title] = true
      }
    })
  },
  { immediate: true, deep: true }
)

const roleText = computed(() => roleLabel(state.role))

const pageTitle = computed(() => {
  const title = route.meta?.title
  if (typeof title === 'string' && title.trim()) return title
  return '综合测评'
})

const isGroupOpen = (title) => openGroups[title] !== false
const toggleGroup = (title) => {
  openGroups[title] = !isGroupOpen(title)
}

const goHome = () => {
  router.push(getHomeByRole())
}

const logout = () => {
  submissionStore.reset()
  clearAuth()
  router.replace('/login')
}
</script>

