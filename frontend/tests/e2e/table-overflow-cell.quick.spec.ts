import { expect, test } from '@playwright/test'

test('TableOverflowCell 浮层长文本不省略 @quick', async ({ page }) => {
  const longClassName =
    '2022级数据科学与大数据技术1班' +
    '错错错错错错错错错错错错错错错错错错错错' +
    '错错错错错错错错错错错错错错错错错错错错' +
    '错错错错错错错错错错错错错错错错错错错错'

  const rows = [
    {
      id: 10001,
      role: 'STUDENT',
      accountNo: 'TDD_OVERFLOW_0001',
      realName: '浮层长文本测试',
      gender: '男',
      phone: '13800000000',
      className: longClassName,
      enabled: 1,
      createdAt: '2026-03-10 00:00',
      canDelete: true
    }
  ]

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
        body: JSON.stringify({ code: 0, message: 'ok', data: rows })
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

  const row = page.locator('tbody tr', { hasText: 'TDD_OVERFLOW_0001' })
  const classCell = row.locator('td', { hasText: '2022级数据科学与大数据技术1班' }).first()
  await expect(classCell).toContainText('2022级数据科学与大数据技术1班')

  await classCell.locator('.table-overflow-cell').hover()
  await classCell.locator('.table-overflow-cell__toggle').click()

  const popoverText = page
    .locator('.table-overflow-cell-popper .table-overflow-cell__popover-text', {
      hasText: '2022级数据科学与大数据技术1班'
    })
    .first()
  await expect(popoverText).toBeVisible()
  const textOverflow = await popoverText.evaluate((el) => getComputedStyle(el).textOverflow)
  const overflowY = await popoverText.evaluate((el) => getComputedStyle(el).overflowY)

  expect(textOverflow).not.toBe('ellipsis')
  expect(overflowY).toBe('auto')
})
