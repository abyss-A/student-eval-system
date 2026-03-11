import { expect, test } from '@playwright/test'

test('管理员学期管理支持新建学期与评分配置 @quick', async ({ page }) => {
  let nextId = 100
  let semesters = [
    { id: 1, name: '2026年春季学期', year: 2026, term: 1, isActive: 1, createdAt: '2026-03-01 00:00' }
  ]
  let activeSemester = semesters[0]
  let submittedPendingCount = 0
  const scoringConfigBySemesterId = {
    1: {
      semesterId: 1,
      wMoral: 0.15,
      wIntel: 0.6,
      intelCourseRatio: 0.85,
      intelInnovationRatio: 0.15,
      wSport: 0.1,
      sportUniversityPeRatio: 0.85,
      sportActivityRatio: 0.15,
      wArt: 0.075,
      wLabor: 0.075,
      capMoral: 100,
      capIntel: 100,
      capSport: 100,
      capArt: 100,
      capLabor: 100
    }
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

    const scoringMatch = url.match(/\/api\/v1\/admin\/semesters\/(\d+)\/scoring-config$/)
    if (scoringMatch && req.method() === 'GET') {
      const semesterId = Number(scoringMatch[1])
      const cfg = scoringConfigBySemesterId[semesterId] || {
        semesterId,
        wMoral: 0.15,
        wIntel: 0.6,
        intelCourseRatio: 0.85,
        intelInnovationRatio: 0.15,
        wSport: 0.1,
        sportUniversityPeRatio: 0.85,
        sportActivityRatio: 0.15,
        wArt: 0.075,
        wLabor: 0.075,
        capMoral: 100,
        capIntel: 100,
        capSport: 100,
        capArt: 100,
        capLabor: 100
      }
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: cfg })
      })
      return
    }

    if (scoringMatch && req.method() === 'PUT') {
      const semesterId = Number(scoringMatch[1])
      let body = {}
      try {
        body = req.postDataJSON() || {}
      } catch {
        body = {}
      }
      scoringConfigBySemesterId[semesterId] = { ...(scoringConfigBySemesterId[semesterId] || {}), semesterId, ...body }
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: scoringConfigBySemesterId[semesterId] })
      })
      return
    }

    const recalcMatch = url.match(/\/api\/v1\/admin\/semesters\/(\d+)\/recalculate$/)
    if (recalcMatch && req.method() === 'POST') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: { total: 1, updated: 1 } })
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

    const renameMatch = url.match(/\/api\/v1\/admin\/semesters\/(\d+)$/)
    if (renameMatch && req.method() === 'PUT') {
      const semesterId = Number(renameMatch[1])
      let body = {}
      try {
        body = req.postDataJSON() || {}
      } catch {
        body = {}
      }
      semesters = semesters.map((s) => (s.id === semesterId ? { ...s, name: String(body?.name || s.name) } : s))
      activeSemester = semesters.find((s) => s.id === activeSemester.id) || activeSemester
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: semesters.find((s) => s.id === semesterId) })
      })
      return
    }

    if (renameMatch && req.method() === 'DELETE') {
      const semesterId = Number(renameMatch[1])
      semesters = semesters.filter((s) => s.id !== semesterId)
      if (activeSemester?.id === semesterId) {
        activeSemester = semesters[0] || null
      }
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: null })
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

  await page.getByRole('button', { name: '评分配置' }).first().click()
  await expect(page.locator('.semester-config-dialog')).toBeVisible()

  const weightSection = page.locator('.semester-config-dialog .semester-config-section').filter({ hasText: '权重' }).first()
  const moralWeight = weightSection.locator('.semester-form-row').filter({ hasText: '德育' }).locator('input')
  const intelWeight = weightSection.locator('.semester-form-row').filter({ hasText: '智育' }).locator('input')
  expect(await moralWeight.evaluate((el) => el.disabled)).toBeFalsy()
  expect(await intelWeight.evaluate((el) => el.disabled)).toBeFalsy()

  // Element Plus 的 el-input-number 会设置 aria-disabled，Playwright 会将其判定为不可输入，这里使用 force 绕过可访问性标记。
  await moralWeight.fill('0.2', { force: true })
  await intelWeight.fill('0.55', { force: true })

  const intelDetailSection = page.locator('.semester-config-dialog .semester-config-section').filter({ hasText: '智育细则' }).first()
  const courseRatio = intelDetailSection.locator('.semester-form-row').filter({ hasText: '课程平均占比' }).locator('input')
  const innovationRatio = intelDetailSection.locator('.semester-form-row').filter({ hasText: '创新活动占比' }).locator('input')
  await courseRatio.fill('0.9', { force: true })
  await innovationRatio.fill('0.1', { force: true })

  await page.getByRole('button', { name: '保存配置' }).click()
  await expect(page.locator('.semester-notice')).toContainText('评分配置已保存')

  await page.getByRole('button', { name: '重算本学期成绩' }).click()
  await page.getByRole('button', { name: '开始重算' }).click()
  await expect(page.locator('.semester-notice')).toContainText('重算完成')

  const configDialog = page.locator('.semester-config-dialog')
  const closeConfigBtn = configDialog.getByRole('button', { name: '关闭', exact: true })
  await expect(closeConfigBtn).toBeEnabled()
  await closeConfigBtn.click()
  await expect(configDialog).toBeHidden()

  await page.getByRole('button', { name: '新建学期' }).click()
  await expect(page.locator('.semester-create-dialog')).toBeVisible()

  const yearInput = page.locator('.semester-create-dialog .el-input-number input').first()
  await yearInput.fill('2099')
  await page.locator('.semester-create-dialog').getByText('秋季').click()

  await page.getByRole('button', { name: '创建' }).click()

  await expect(page.locator('table')).toContainText('2099年秋季学期')
})
