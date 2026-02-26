import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import StudentView from '../views/StudentView.vue'
import TeacherView from '../views/TeacherView.vue'
import AdminView from '../views/AdminView.vue'
import RankingView from '../views/RankingView.vue'
import { canAccessRoute, getHomeByRole, getRole, getToken, isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: () => (isLoggedIn() ? getHomeByRole() : '/login')
  },
  {
    path: '/login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/student',
    component: StudentView,
    meta: { roles: ['STUDENT'] }
  },
  {
    path: '/teacher',
    component: TeacherView,
    meta: { roles: ['COUNSELOR', 'ADMIN'] }
  },
  {
    path: '/admin',
    component: AdminView,
    meta: { roles: ['ADMIN'] }
  },
  {
    path: '/ranking',
    component: RankingView,
    meta: { roles: ['STUDENT', 'COUNSELOR', 'ADMIN'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = getToken()
  const role = getRole()

  if (to.meta?.public) {
    if (token) {
      return getHomeByRole(role)
    }
    return true
  }

  if (!token) {
    return '/login'
  }

  if (!canAccessRoute(to.meta?.roles, role)) {
    return getHomeByRole(role)
  }

  return true
})

export default router