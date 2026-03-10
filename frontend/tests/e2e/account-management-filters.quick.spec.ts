import { expect, test } from '@playwright/test'

test('账号管理筛选下拉默认文案 @quick', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('realName', '测试管理员')
    localStorage.setItem('userId', '1')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const headers = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
      'Access-Control-Allow-Headers': 'Authorization,Content-Type'
    }

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    if (req.url().includes('/api/v1/admin/accounts')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: [] })
      })
      return
    }

    await route.fulfill({
      status: 200,
      headers,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: null })
    })
  })

  await page.goto('/admin/accounts', { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.workspace-title')).toHaveText('账号管理')

  const selects = page.locator('.account-filter-row .el-select')
  await expect(selects).toHaveCount(2)

  await expect(selects.nth(0)).toContainText('全部角色')
  await expect(selects.nth(1)).toContainText('全部状态')
})
