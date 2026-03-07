import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, gotoAndReady, login, logout } from './helpers'

test('反馈创建页未保存离开保护生效 @full', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)
  await gotoAndReady(page, '/student/feedback/create')
  await assertNoFatalSignals(page)

  await page.getByPlaceholder('例如：系统无法上传图片').fill('离开拦截测试')

  const navLink = page.locator('.menu-item', { hasText: '我的反馈' }).first()
  let nextDialogAction: 'dismiss' | 'accept' = 'dismiss'
  page.on('dialog', async (dialog) => {
    if (nextDialogAction === 'dismiss') {
      await dialog.dismiss()
      return
    }
    await dialog.accept()
  })

  await navLink.click({ force: true })
  await expect(page).toHaveURL(/\/student\/feedback\/create/)

  nextDialogAction = 'accept'
  await navLink.click({ force: true })
  await page.waitForURL(/\/student\/feedback\/mine/)
  await assertNoFatalSignals(page)

  await logout(page)
})
