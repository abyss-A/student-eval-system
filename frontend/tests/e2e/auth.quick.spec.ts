import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, login, logout } from './helpers'

test('@quick 登录与路由守卫', async ({ page }) => {
  await page.goto('/student/eval/course')
  await expect(page).toHaveURL(/\/login/)

  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '课程成绩')
  await logout(page)
})

test('@quick 角色越权会回到首页', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await page.goto('/admin/finalize/tasks', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/student\//)
  await logout(page)
})
