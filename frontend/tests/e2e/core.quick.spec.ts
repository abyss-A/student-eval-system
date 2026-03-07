import { test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('@quick 学生核心链路页面可访问', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)

  const routes = [
    ['/student/eval/course', '课程成绩'],
    ['/student/eval/sport', '体育填报'],
    ['/student/eval/submit', '综合成绩与提交'],
    ['/student/notices', '公告通知'],
    ['/student/feedback/mine', '我的反馈'],
    ['/student/ranking', '综合排名']
  ] as const

  for (const [route, title] of routes) {
    await gotoAndReady(page, route)
    await assertNoFatalSignals(page)
    await expectWorkspaceTitle(page, title)
  }

  await logout(page)
})

test('@quick 辅导员与管理员主页可访问', async ({ page }) => {
  await login(page, accounts.counselor.accountNo, accounts.counselor.password, accounts.counselor.home)
  await gotoAndReady(page, '/teacher/review/tasks')
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '待审核列表')
  await logout(page)

  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/counselor/scopes')
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '班级权限管理')
  await logout(page)
})
