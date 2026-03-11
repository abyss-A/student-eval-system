import { expect, test } from '@playwright/test'

test('管理员学期管理支持新建学期 @quick', async ({ page }) => {
  let nextId = 100
  let semesters = [
    { id: 1, name: '2026年春季学期', year: 2026, term: 1, isActive: 1, createdAt: '2026-03-01 00:00' }
  ]
  let activeSemester = semesters[0]
  let submittedPendingCount = 0

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

    if (url.endsWith('/api/v1/admin/semesters') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: { activeSemester, submittedPendingCount, semesters }
        })
      })
      return
    }

    if (url.endsWith('/api/v1/admin/semesters') && req.method() === 'POST') {
      let body = {}
      try {
        body = req.postDataJSON() || {}
      } catch {
        body = {}
      }
      const created = {
        id: nextId++,
        name: String(body?.name || '').trim() || '未命名学期',
        year: Number(body?.year) || 2099,
        term: String(body?.season || '').toUpperCase() === 'AUTUMN' ? 2 : 1,
        isActive: 0,
        createdAt: '2026-03-11 00:00'
      }
      semesters = [created, ...semesters]
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: created })
      })
      return
    }

    const activateMatch = url.match(/\/api\/v1\/admin\/semesters\/(\d+)\/active$/)
    if (activateMatch && req.method() === 'PUT') {
      const targetId = Number(activateMatch[1])
      semesters = semesters.map((s) => ({ ...s, isActive: s.id === targetId ? 1 : 0 }))
      activeSemester = semesters.find((s) => s.id === targetId) || activeSemester
      submittedPendingCount = 0
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: activeSemester })
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

  await page.goto('/admin/semesters', { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.workspace-title')).toHaveText('学期管理')
  await expect(page.locator('.semester-summary')).toContainText('2026年春季学期')
  await expect(page.locator('.semester-summary')).toContainText('0')

  await page.getByRole('button', { name: '新建学期' }).click()
  await expect(page.locator('.semester-create-dialog')).toBeVisible()

  const yearInput = page.locator('.semester-create-dialog .el-input-number input').first()
  await yearInput.fill('2099')
  await page.locator('.semester-create-dialog').getByText('秋季').click()

  await page.getByRole('button', { name: '创建' }).click()

  await expect(page.locator('table')).toContainText('2099年秋季学期')
})
