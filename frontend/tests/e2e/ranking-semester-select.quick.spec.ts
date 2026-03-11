import { expect, test } from '@playwright/test'

test('综合排名支持按学期名称下拉切换 @quick', async ({ page }) => {
  const semesters = [
    { id: 1, name: '2026年春季学期', year: 2026, term: 1, isActive: 1, createdAt: '2026-03-01 00:00' },
    { id: 2, name: '2025年秋季学期', year: 2025, term: 2, isActive: 0, createdAt: '2025-09-01 00:00' }
  ]

  const rankingsBySemesterId = {
    1: [
      { id: 1, account_no: '2022000001', real_name: '学生一', class_name: '2022级数据科学与大数据技术1班', total_score: 88, rankClass: 1 }
    ],
    2: [
      { id: 2, account_no: '2022000002', real_name: '学生二', class_name: '2022级数据科学与大数据技术2班', total_score: 91, rankClass: 1 }
    ]
  }

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

    const url = req.url()
    if (url.includes('/api/v1/semesters')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: semesters })
      })
      return
    }

    if (url.includes('/api/v1/rankings')) {
      const parsed = new URL(url)
      const semesterId = Number(parsed.searchParams.get('semesterId') || '0')
      const rows = rankingsBySemesterId[semesterId] || []
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

  await page.goto('/admin/ranking', { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.workspace-title')).toHaveText('综合排名')

  const semesterSelect = page.locator('.ranking-semester-select')
  await expect(semesterSelect).toBeVisible()
  await expect(semesterSelect).toContainText('2026年春季学期')

  await expect(page.locator('table')).toContainText('2022000001')

  await semesterSelect.click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: '2025年秋季学期' }).first().click()

  await expect(page.locator('table')).toContainText('2022000002')
})

