import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('管理员核心页面可访问 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)

  const routes = [
    ['/admin/finalize/tasks', '待处理测评单'],
    ['/admin/counselor/scopes', '班级权限管理'],
    ['/admin/notices', '公告管理'],
    ['/admin/feedback/handle', '反馈处理'],
    ['/admin/ranking', '综合排名'],
    ['/admin/me/profile', '账号中心']
  ] as const

  for (const [route, title] of routes) {
    await gotoAndReady(page, route)
    await assertNoFatalSignals(page)
    await expectWorkspaceTitle(page, title)
  }

  await logout(page)
})

test('班级权限管理页交互检查 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/counselor/scopes')
  await assertNoFatalSignals(page)

  await expect(page.getByPlaceholder('搜索工号/姓名')).toBeVisible()

  const firstRow = page.locator('table tbody tr').first()
  if (await firstRow.isVisible().catch(() => false)) {
    await firstRow.click()
    await expect(page.getByPlaceholder('搜索班级')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText('班级权限配置')).toBeVisible()
    await expect(page.getByRole('button', { name: '保存' })).toBeVisible()
  }

  await logout(page)
})
