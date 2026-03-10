import { expect, test } from '@playwright/test'
import { accounts, login, logout } from '../e2e/helpers'

test.describe('@visual 移动端可用性截图', () => {
  test.use({
    viewport: { width: 390, height: 844 }
  })

  test('学生课程页移动端截图', async ({ page }) => {
    await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
    await page.goto('/student/eval/course', { waitUntil: 'networkidle' })
    const shot = await page.screenshot({ fullPage: true })
    expect(shot.byteLength).toBeGreaterThan(55_000)
    await logout(page)
  })

  test('管理员班级权限页移动端截图', async ({ page }) => {
    await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
    await page.goto('/admin/counselor/scopes', { waitUntil: 'networkidle' })
    const shot = await page.screenshot({ fullPage: true })
    expect(shot.byteLength).toBeGreaterThan(55_000)
    await logout(page)
  })
})
