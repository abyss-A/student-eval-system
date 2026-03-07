import { expect, test } from '@playwright/test'
import { accounts, expectWorkspaceTitle, login, logout } from './helpers'

test('接口500时页面给出错误反馈 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)

  await page.route('**/api/v1/notices**', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ code: 50000, message: 'mock server error' })
    })
  })

  await page.goto('/admin/notices', { waitUntil: 'networkidle' })
  const bodyText = await page.locator('body').innerText()
  expect(bodyText).toContain('加载失败')

  await page.unroute('**/api/v1/notices**')
  await logout(page)
})

test('断网恢复后页面可继续访问 @full', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)

  await page.context().setOffline(true)
  await page.goto('/student/notices').catch(() => {})
  await page.context().setOffline(false)

  await page.goto('/student/notices', { waitUntil: 'networkidle' })
  await expectWorkspaceTitle(page, '公告通知')
  await logout(page)
})
