import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('辅导员审核页与抽屉可打开 @full', async ({ page }) => {
  await login(page, accounts.counselor.accountNo, accounts.counselor.password, accounts.counselor.home)
  await gotoAndReady(page, '/teacher/review/tasks')
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '待审核列表')

  const openBtn = page.getByRole('button', { name: /打开审核|查看/ }).first()
  if (await openBtn.isVisible().catch(() => false)) {
    await openBtn.click()
    await expect(page.getByText('课程审核')).toBeVisible()
    await expect(page.getByText('活动审核')).toBeVisible()
    const closeBtn = page.getByRole('button', { name: '关闭' }).last()
    if (await closeBtn.isVisible().catch(() => false)) {
      await closeBtn.click()
    }
  }

  await logout(page)
})

test('辅导员公告/反馈/排名页面可访问 @full', async ({ page }) => {
  await login(page, accounts.counselor.accountNo, accounts.counselor.password, accounts.counselor.home)

  const routes = [
    ['/teacher/notices', '公告管理'],
    ['/teacher/feedback/mine', '我的反馈'],
    ['/teacher/ranking', '综合排名'],
    ['/teacher/me/profile', '账号中心']
  ] as const

  for (const [route, title] of routes) {
    await gotoAndReady(page, route)
    await assertNoFatalSignals(page)
    await expectWorkspaceTitle(page, title)
  }

  await logout(page)
})
