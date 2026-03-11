import { expect, test } from '@playwright/test'

function withCorsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization,Content-Type'
  }
}

test('辅导员首页支持一键直达并自动打开审核抽屉 @quick', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'COUNSELOR')
    localStorage.setItem('realName', '测试辅导员')
    localStorage.setItem('userId', '2')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const headers = withCorsHeaders()

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    const url = req.url()

    if (url.includes('/api/v1/reviews/tasks') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            {
              id: 100,
              status: 'SUBMITTED',
              review_phase: 'NOT_REVIEWED',
              account_no: '2022000001',
              real_name: '学生甲',
              class_name: '2022级1班',
              review_done_count: 0,
              review_total_count: 3,
              submitted_at: '2026-03-01 00:00'
            }
          ]
        })
      })
      return
    }

    const subMatch = url.match(/\/api\/v1\/submissions\/(\d+)$/)
    if (subMatch && req.method() === 'GET') {
      const id = Number(subMatch[1] || '0')
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            submission: { id, status: 'SUBMITTED' },
            student: { id: 1, accountNo: '2022000001', realName: '学生甲' },
            semester: { id: 1, name: '2026年春季学期' },
            courses: [],
            activities: []
          }
        })
      })
      return
    }

    if (url.includes('/api/v1/notices') && req.method() === 'GET') {
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

  await page.goto('/teacher/home', { waitUntil: 'domcontentloaded' })
  await page.getByRole('button', { name: '继续审核' }).first().click()

  await page.waitForURL(/\/teacher\/review\/tasks/)
  await expect(page.getByText('课程审核')).toBeVisible()
})

test('管理员首页 NEW 反馈一键直达并自动打开处理抽屉 @quick', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('realName', '测试管理员')
    localStorage.setItem('userId', '3')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const headers = withCorsHeaders()

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    const url = req.url()

    if (url.includes('/api/v1/admin/semesters') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: { activeSemester: { id: 1, name: '2026年春季学期' }, submittedPendingCount: 12, semesters: [] }
        })
      })
      return
    }

    if (url.includes('/api/v1/admin/tasks') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: [] })
      })
      return
    }

    if (url.includes('/api/v1/feedbacks') && req.method() === 'GET') {
      const parsed = new URL(url)
      if (parsed.searchParams.get('status') === 'NEW') {
        await route.fulfill({
          status: 200,
          headers,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 0,
            message: 'ok',
            data: [
              {
                id: 501,
                status: 'NEW',
                title: '需要协助处理导出问题',
                creator_real_name: '张三',
                class_name: '2022级1班',
                created_at: '2026-03-01 00:00'
              }
            ]
          })
        })
        return
      }
    }

    if (url.includes('/api/v1/feedbacks/501') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            id: 501,
            status: 'NEW',
            title: '需要协助处理导出问题',
            content: '导出 Word 失败，提示模板异常。',
            screenshot_file_ids: '',
            creator_real_name: '张三',
            class_name: '2022级1班',
            created_at: '2026-03-01 00:00'
          }
        })
      })
      return
    }

    if (url.includes('/api/v1/notices') && req.method() === 'GET') {
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

  await page.goto('/admin/home', { waitUntil: 'domcontentloaded' })
  await page.getByRole('button', { name: '立即处理' }).first().click()

  await page.waitForURL(/\/admin\/feedback\/handle/)
  await expect(page.locator('.drawer-panel').getByText('反馈处理')).toBeVisible()
})

test('管理员首页测评单一键直达并自动打开查看抽屉 @quick', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('realName', '测试管理员')
    localStorage.setItem('userId', '3')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const headers = withCorsHeaders()

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    const url = req.url()

    if (url.includes('/api/v1/admin/semesters') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: { activeSemester: { id: 1, name: '2026年春季学期' }, submittedPendingCount: 12, semesters: [] }
        })
      })
      return
    }

    if (url.includes('/api/v1/feedbacks') && req.method() === 'GET') {
      const parsed = new URL(url)
      if (parsed.searchParams.get('status') === 'NEW') {
        await route.fulfill({
          status: 200,
          headers,
          contentType: 'application/json',
          body: JSON.stringify({ code: 0, message: 'ok', data: [] })
        })
        return
      }
    }

    if (url.includes('/api/v1/admin/tasks') && req.method() === 'GET') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            {
              id: 601,
              account_no: '2022000001',
              real_name: '学生甲',
              class_name: '2022级1班',
              total_score: 88.5,
              passTime: '2026-03-01 00:00',
              status: 'COUNSELOR_REVIEWED'
            }
          ]
        })
      })
      return
    }

    const subMatch = url.match(/\/api\/v1\/submissions\/(\d+)$/)
    if (subMatch && req.method() === 'GET') {
      const id = Number(subMatch[1] || '0')
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            submission: { id, status: 'COUNSELOR_REVIEWED' },
            student: { id: 1, accountNo: '2022000001', realName: '学生甲' },
            semester: { id: 1, name: '2026年春季学期' },
            courses: [],
            activities: []
          }
        })
      })
      return
    }

    if (url.includes('/api/v1/notices') && req.method() === 'GET') {
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

  await page.goto('/admin/home', { waitUntil: 'domcontentloaded' })

  await Promise.all([
    page.waitForURL(/\/admin\/submissions/),
    page.getByRole('button', { name: '查看', exact: true }).first().click()
  ])
  await expect(page.locator('.drawer-panel').getByText('查看测评单 #601')).toBeVisible()
})
