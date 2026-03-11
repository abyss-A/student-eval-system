import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RankingView from '../views/RankingView.vue'
import StudentLayout from '../layouts/StudentLayout.vue'
import TeacherLayout from '../layouts/TeacherLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

import EvalCourse from '../views/student/EvalCourse.vue'
import EvalModule from '../views/student/EvalModule.vue'
import EvalSubmit from '../views/student/EvalSubmit.vue'

import Notices from '../views/common/Notices.vue'
import NoticeDetail from '../views/common/NoticeDetail.vue'

import FeedbackCreate from '../views/common/FeedbackCreate.vue'
import FeedbackMyList from '../views/common/FeedbackMyList.vue'
import FeedbackDetail from '../views/common/FeedbackDetail.vue'
import FeedbackHandle from '../views/common/FeedbackHandle.vue'
import AccountProfile from '../views/common/AccountProfile.vue'

import TeacherReviewTasks from '../views/TeacherView.vue'
import AdminView from '../views/AdminView.vue'
import CounselorScopeView from '../views/admin/CounselorScopeView.vue'
import AccountManagementView from '../views/admin/AccountManagementView.vue'
import SemesterManagementView from '../views/admin/SemesterManagementView.vue'

import { canAccessRoute, getHomeByRole, getRole, getToken, isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: () => (isLoggedIn() ? getHomeByRole() : '/login')
  },
  {
    path: '/login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    redirect: { path: '/login', query: { reason: 'contact-admin' } }
  },
  {
    path: '/student',
    component: StudentLayout,
    meta: { roles: ['STUDENT'] },
    children: [
      { path: '', redirect: '/student/eval/course' },
      { path: 'eval/course', component: EvalCourse, meta: { title: '课程成绩' } },
      { path: 'eval/moral', component: EvalModule, props: { moduleType: 'MORAL', label: '德育' }, meta: { title: '德育填报' } },
      { path: 'eval/intel', component: EvalModule, props: { moduleType: 'INTEL_PRO_INNOV', label: '智育' }, meta: { title: '智育填报' } },
      { path: 'eval/sport', component: EvalModule, props: { moduleType: 'SPORT_ACTIVITY', label: '体育' }, meta: { title: '体育填报' } },
      { path: 'eval/art', component: EvalModule, props: { moduleType: 'ART', label: '美育' }, meta: { title: '美育填报' } },
      { path: 'eval/labor', component: EvalModule, props: { moduleType: 'LABOR', label: '劳育' }, meta: { title: '劳育填报' } },
      { path: 'eval/submit', component: EvalSubmit, meta: { title: '综合成绩与提交' } },
      { path: 'notices', component: Notices, meta: { title: '公告通知' } },
      { path: 'notices/:id', component: NoticeDetail, meta: { title: '公告详情' } },
      { path: 'feedback/create', component: FeedbackCreate, meta: { title: '我要反馈', unsavedGuard: 'feedbackCreate' } },
      { path: 'feedback/mine', component: FeedbackMyList, meta: { title: '我的反馈' } },
      { path: 'feedback/:id', component: FeedbackDetail, meta: { title: '反馈详情' } },
      { path: 'ranking', component: RankingView, meta: { title: '综合排名' } },
      { path: 'me/profile', component: AccountProfile, meta: { title: '账号中心' } }
    ]
  },
  {
    path: '/teacher',
    component: TeacherLayout,
    meta: { roles: ['COUNSELOR'] },
    children: [
      { path: '', redirect: '/teacher/review/tasks' },
      { path: 'review/tasks', component: TeacherReviewTasks, meta: { title: '待审核列表' } },
      { path: 'notices', component: Notices, meta: { title: '公告管理' } },
      { path: 'notices/:id', component: NoticeDetail, meta: { title: '公告详情' } },
      { path: 'feedback/create', component: FeedbackCreate, meta: { title: '我要反馈', unsavedGuard: 'feedbackCreate' } },
      { path: 'feedback/mine', component: FeedbackMyList, meta: { title: '我的反馈' } },
      { path: 'feedback/:id', component: FeedbackDetail, meta: { title: '反馈详情' } },
      { path: 'ranking', component: RankingView, meta: { title: '综合排名' } },
      { path: 'me/profile', component: AccountProfile, meta: { title: '账号中心' } }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/submissions' },
      { path: 'submissions', component: AdminView, meta: { title: '测评单查看' } },
      { path: 'finalize/tasks', redirect: '/admin/submissions' },
      { path: 'counselor/scopes', component: CounselorScopeView, meta: { title: '班级权限管理' } },
      { path: 'accounts', component: AccountManagementView, meta: { title: '账号管理' } },
      { path: 'semesters', component: SemesterManagementView, meta: { title: '学期管理' } },
      { path: 'notices', component: Notices, meta: { title: '公告管理' } },
      { path: 'notices/:id', component: NoticeDetail, meta: { title: '公告详情' } },
      { path: 'feedback/handle', component: FeedbackHandle, meta: { title: '反馈处理' } },
      { path: 'feedback/:id', component: FeedbackDetail, meta: { title: '反馈详情' } },
      { path: 'ranking', component: RankingView, meta: { title: '综合排名' } },
      { path: 'me/profile', component: AccountProfile, meta: { title: '账号中心' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = getToken()
  const role = getRole()
  const isPublic = to.matched.some((record) => record.meta?.public)

  if (isPublic) {
    if (token) return getHomeByRole(role)
    return true
  }

  if (!token) {
    return '/login'
  }

  const requiredRoles = to.matched.flatMap((record) => record.meta?.roles || [])
  if (!canAccessRoute(requiredRoles, role)) {
    return getHomeByRole(role)
  }
  return true
})

export default router
