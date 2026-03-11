import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, login, logout } from './helpers'

test('@quick 登录页视觉质感已升级', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })

  const style = await page.locator('.fb-login-card').evaluate((element) => {
    const cardStyle = window.getComputedStyle(element)
    const pageStyle = window.getComputedStyle(document.querySelector('.fb-login-page'))
    return {
      radius: Number.parseFloat(cardStyle.borderRadius || '0'),
      backdropFilter: cardStyle.backdropFilter || cardStyle.webkitBackdropFilter || 'none',
      pageBackgroundImage: pageStyle.backgroundImage || 'none'
    }
  })

  expect(style.radius).toBeGreaterThanOrEqual(22)
  expect(style.backdropFilter).not.toBe('none')
  expect(style.pageBackgroundImage).not.toBe('none')
})

test('@quick 登录页已切换到更克制的学院版', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })

  await expect(page.locator('.fb-brand-badge')).toHaveCount(0)
  await expect(page.locator('.fb-password-toggle svg')).toBeVisible()

  const toggleText = await page.locator('.fb-password-toggle').textContent()
  expect(String(toggleText || '').trim()).toBe('')

  const layout = await page.evaluate(() => {
    const loginCard = document.querySelector('.fb-login-card')?.getBoundingClientRect()
    const devTools = document.querySelector('.fb-dev-tools')?.getBoundingClientRect()
    const titleStyle = window.getComputedStyle(document.querySelector('.fb-brand-title'))
    return {
      loginCardWidth: loginCard?.width || 0,
      devToolsWidth: devTools?.width || 0,
      titleWeight: Number.parseInt(titleStyle.fontWeight || '0', 10)
    }
  })

  expect(layout.loginCardWidth).toBeGreaterThan(0)
  expect(layout.devToolsWidth).toBeGreaterThan(0)
  expect(layout.devToolsWidth).toBeGreaterThanOrEqual(layout.loginCardWidth - 12)
  expect(layout.titleWeight).toBeLessThanOrEqual(850)
})

test('@quick 登录页已统一为主容器布局', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })

  await expect(page.locator('.fb-login-rail')).toBeVisible()

  const layout = await page.evaluate(() => {
    const brand = document.querySelector('.fb-brand-panel')?.getBoundingClientRect()
    const rail = document.querySelector('.fb-login-rail')?.getBoundingClientRect()
    return {
      visualGap: brand && rail ? rail.left - brand.right : 999,
      railWidth: rail?.width || 0
    }
  })

  expect(layout.visualGap).toBeLessThan(90)
  expect(layout.railWidth).toBeGreaterThan(380)
})

test('@quick 登录页不再提供自助注册入口', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await expect(page.getByRole('button', { name: '创建新账户' })).toHaveCount(0)

  await page.goto('/register', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByText('账号由管理员统一创建，请联系管理员开通账号。')).toBeVisible()
})

test('@quick 登录与路由守卫', async ({ page }) => {
  await page.goto('/student/eval/course')
  await expect(page).toHaveURL(/\/login/)

  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '首页')
  await logout(page)
})

test('@quick 角色越权会回到首页', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await page.goto('/admin/submissions', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/student\//)
  await logout(page)
})

test('@quick 管理员旧测评单路径会跳转到新路径', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await page.goto('/admin/finalize/tasks', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/admin\/submissions/)
  await expectWorkspaceTitle(page, '测评单查看')
  await logout(page)
})
