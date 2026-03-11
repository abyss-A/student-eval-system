import { expect, test } from '@playwright/test'

function withCorsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization,Content-Type'
  }
}

test('学生首页可访问并展示核心区域 @quick', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('role', 'STUDENT')
    localStorage.setItem('realName', '测试学生')
    localStorage.setItem('userId', '1')
  })

  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const req = route.request()
    const headers = withCorsHeaders()

    if (req.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers })
      return
    }

    const url = req.url()
    if (url.endsWith('/api/v1/submissions') && req.method() === 'POST') {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: { id: 11, status: 'DRAFT' } })
      })
      return
    }

    if (url.includes('/api/v1/submissions/11') && req.method() === 'GET') {
      if (url.endsWith('/score')) {
        await route.fulfill({
          status: 200,
          headers,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 0,
            message: 'ok',
            data: {
              status: 'DRAFT',
              reviewPhase: 'NOT_REVIEWED',
              reviewTotalCount: 0,
              reviewDoneCount: 0,
              previewTotalScore: 0,
              canStudentResubmit: false
            }
          })
        })
        return
      }

      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            submission: { id: 11, status: 'DRAFT' },
            student: { id: 1, realName: '测试学生' },
            semester: { id: 1, name: '2026年春季学期' },
            courses: [],
            activities: []
          }
        })
      })
      return
    }

    if (url.includes('/api/v1/notices')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            { id: 1, title: '通知一', published_at: '2026-03-01 00:00' },
            { id: 2, title: '通知二', published_at: '2026-02-28 00:00' }
          ]
        })
      })
      return
    }

    if (url.includes('/api/v1/feedbacks/my')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            { id: 1, status: 'NEW' },
            { id: 2, status: 'CLOSED' }
          ]
        })
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

  await page.goto('/student/home', { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.workspace-title')).toHaveText('首页')
  await expect(page.getByRole('heading', { name: '分数概览' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '待办提醒' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '公告通知' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '快捷入口' })).toBeVisible()
  await expect(page.getByText('2026年春季学期')).toBeVisible()
})

test('辅导员首页可访问并展示核心区域 @quick', async ({ page }) => {
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
    if (url.includes('/api/v1/reviews/tasks')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            { id: 1, status: 'SUBMITTED', review_phase: 'NOT_REVIEWED' },
            { id: 2, status: 'SUBMITTED', review_phase: 'IN_PROGRESS' },
            { id: 3, status: 'SUBMITTED', review_phase: 'REVIEWED' },
            { id: 4, status: 'SUBMITTED', review_phase: 'READY_TO_SUBMIT' },
            { id: 5, status: 'COUNSELOR_REVIEWED', review_phase: 'READY_TO_SUBMIT' }
          ]
        })
      })
      return
    }

    if (url.includes('/api/v1/notices')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [{ id: 1, title: '通知一', updated_at: '2026-03-01 00:00' }]
        })
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
  await expect(page.locator('.workspace-title')).toHaveText('首页')
  await expect(page.getByRole('heading', { name: '待办列表预览' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '可提交管理员' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '公告通知' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '快捷入口' })).toBeVisible()
})

test('管理员首页可访问并展示核心区域 @quick', async ({ page }) => {
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
    if (url.includes('/api/v1/admin/semesters')) {
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

    if (url.includes('/api/v1/feedbacks')) {
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
              { id: 1, status: 'NEW', title: '反馈一', creator_real_name: '张三', class_name: '2022级1班', created_at: '2026-03-01 00:00' },
              { id: 2, status: 'NEW', title: '反馈二', creator_real_name: '李四', class_name: '2022级2班', created_at: '2026-03-01 00:00' },
              { id: 3, status: 'NEW', title: '反馈三', creator_real_name: '王五', class_name: '2022级3班', created_at: '2026-03-01 00:00' }
            ]
          })
        })
        return
      }
    }

    if (url.includes('/api/v1/admin/tasks')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [
            { id: 11, account_no: '2022000001', real_name: '测试学生', class_name: '2022级1班', total_score: 88.5, passTime: '2026-03-01 00:00', status: 'COUNSELOR_REVIEWED' }
          ]
        })
      })
      return
    }

    if (url.includes('/api/v1/notices')) {
      await route.fulfill({
        status: 200,
        headers,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [{ id: 1, title: '通知一', updated_at: '2026-03-01 00:00' }]
        })
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
  await expect(page.locator('.workspace-title')).toHaveText('首页')
  await expect(page.getByRole('heading', { name: 'NEW 反馈预览' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '测评单动态预览' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '公告通知' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '快捷入口' })).toBeVisible()
  await expect(page.getByText('2026年春季学期')).toBeVisible()
})
