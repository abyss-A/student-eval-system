import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('工作台折叠、刷新、标签切换正常 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)

  const sidebar = page.locator('.sidebar')
  await expect(sidebar).toBeVisible()

  await page.getByRole('button', { name: '切换侧边栏' }).click()
  await expect(sidebar).toHaveClass(/collapsed/)
  await page.getByRole('button', { name: '切换侧边栏' }).click()
  await expect(sidebar).not.toHaveClass(/collapsed/)

  await gotoAndReady(page, '/admin/submissions')
  await expectWorkspaceTitle(page, '测评单查看')
  await gotoAndReady(page, '/admin/notices')
  await expectWorkspaceTitle(page, '公告管理')
  await gotoAndReady(page, '/admin/ranking')
  await expectWorkspaceTitle(page, '综合排名')
  await assertNoFatalSignals(page)

  const tabCount = await page.locator('.workspace-tab').count()
  expect(tabCount).toBeGreaterThanOrEqual(2)

  await page.getByRole('button', { name: '刷新当前页面' }).click()
  await page.waitForURL(/\/admin\/ranking/)
  await assertNoFatalSignals(page)

  const closableButtons = page.locator('.workspace-tab-close')
  const closableCount = await closableButtons.count()
  if (closableCount > 0) {
    await closableButtons.first().click()
    await expect(page.locator('.workspace-tab.active')).toBeVisible()
  }

  await logout(page)
})
