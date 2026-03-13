import { expect, test } from '@playwright/test'

test('账号管理支持批量停用与批量重置密码 @quick', async ({ page }) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization,Content-Type'
  }

  const accounts = [
    {
      id: 101,
      role: 'STUDENT',
      accountNo: '2022000101',
      realName: '学生一',
      gender: '男',
      phone: '13800000101',
      className: '2022级数据科学与大数据技术1班',
      enabled: 1,
      createdAt: '2026-03-01 00:00',
      canDelete: true,
      deleteBlockReason: ''
    },
    {
      id: 102,
      role: 'COUNSELOR',
      accountNo: '9000000102',
      realName: '辅导员二',
      gender: '女',
      phone: '13800000102',
      className: '',
      enabled: 0,
      createdAt: '2026-03-01 00:00',
      canDelete: true,
      deleteBlockReason: ''
    }
  ]

  let enabledCalls = 0
  let resetCalls = 0

  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('realName', '测试管理员')
    localStorage.setItem('userId', '1')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const url = req.url()

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    if (url.includes('/api/v1/admin/accounts') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: accounts })
      })
      return
    }

    const enabledMatch = url.match(/\/api\/v1\/admin\/accounts\/(\d+)\/enabled$/)
    if (enabledMatch && req.method() === 'PUT') {
      enabledCalls += 1
      const id = Number(enabledMatch[1] || '0')
      let body: any = {}
      try {
        body = req.postDataJSON()
      } catch {
        body = {}
      }
      const next = body?.enabled ? 1 : 0
      const hit = accounts.find((a) => a.id === id)
      if (hit) hit.enabled = next
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: hit || null })
      })
      return
    }

    const resetMatch = url.match(/\/api\/v1\/admin\/accounts\/(\d+)\/reset-password$/)
    if (resetMatch && req.method() === 'POST') {
      resetCalls += 1
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: { defaultPassword: '123456' } })
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

  const table = page.locator('table.account-table')
  const firstRowCheckbox = table.locator('tbody tr').first().locator('input[type="checkbox"]').first()
  await expect(firstRowCheckbox).toBeEnabled()
  await firstRowCheckbox.check({ force: true })

  await expect(page.locator('.account-toolbar-actions')).toContainText('已选 1 项')

  await page.getByRole('button', { name: '批量操作' }).click()
  await page.locator('.el-dropdown-menu__item').filter({ hasText: '批量停用' }).first().click()
  await page.getByRole('button', { name: '确认停用' }).click()

  await expect(page.locator('.account-notice')).toContainText('批量停用完成')
  expect(enabledCalls).toBeGreaterThanOrEqual(1)

  const firstRowCheckboxAgain = table.locator('tbody tr').first().locator('input[type="checkbox"]').first()
  await expect(firstRowCheckboxAgain).toBeEnabled()
  await firstRowCheckboxAgain.check({ force: true })
  await page.getByRole('button', { name: '批量操作' }).click()
  await page.locator('.el-dropdown-menu__item').filter({ hasText: '批量重置密码' }).first().click()
  await page.getByRole('button', { name: '确认重置' }).click()

  await expect(page.locator('.account-notice')).toContainText('批量重置密码完成')
  expect(resetCalls).toBeGreaterThanOrEqual(1)
})
