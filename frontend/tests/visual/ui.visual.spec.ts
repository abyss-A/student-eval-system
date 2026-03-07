import { expect, test } from '@playwright/test'
import { accounts, login, logout } from '../e2e/helpers'

async function expectScreenshotNotBlank(buffer: Buffer) {
  expect(buffer.byteLength).toBeGreaterThan(80_000)
}

test('@visual 管理员工作台截图', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await page.goto('/admin/counselor/scopes', { waitUntil: 'networkidle' })
  const shot = await page.screenshot({ fullPage: true })
  await expectScreenshotNotBlank(shot)
  await logout(page)
})

test('@visual 辅导员审核页截图', async ({ page }) => {
  await login(page, accounts.counselor.accountNo, accounts.counselor.password, accounts.counselor.home)
  await page.goto('/teacher/review/tasks', { waitUntil: 'networkidle' })
  const shot = await page.screenshot({ fullPage: true })
  await expectScreenshotNotBlank(shot)
  await logout(page)
})

test('@visual 学生课程填报截图', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await page.goto('/student/eval/course', { waitUntil: 'networkidle' })
  const shot = await page.screenshot({ fullPage: true })
  await expectScreenshotNotBlank(shot)
  await logout(page)
})
