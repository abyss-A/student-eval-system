import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('学生端全链路页面巡检 @full', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)

  const routes = [
    ['/student/eval/course', '课程成绩'],
    ['/student/eval/moral', '德育填报'],
    ['/student/eval/intel', '智育填报'],
    ['/student/eval/sport', '体育填报'],
    ['/student/eval/art', '美育填报'],
    ['/student/eval/labor', '劳育填报'],
    ['/student/eval/submit', '综合成绩与提交'],
    ['/student/notices', '公告通知'],
    ['/student/feedback/create', '我要反馈'],
    ['/student/feedback/mine', '我的反馈'],
    ['/student/ranking', '综合排名'],
    ['/student/me/profile', '账号中心']
  ] as const

  for (const [route, title] of routes) {
    await gotoAndReady(page, route)
    await assertNoFatalSignals(page)
    await expectWorkspaceTitle(page, title)
  }

  await logout(page)
})

test('学生端提交页按钮状态与提示可见 @full', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await gotoAndReady(page, '/student/eval/submit')
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '综合成绩与提交')

  const hasPrimaryAction = await page.getByRole('button', { name: /提交审核|再次提交/ }).first().isVisible().catch(() => false)
  expect(hasPrimaryAction).toBeTruthy()

  await logout(page)
})
