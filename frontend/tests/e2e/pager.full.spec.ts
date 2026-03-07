import { expect, Page, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, gotoAndReady, login, logout } from './helpers'

async function assertPagerBasics(page: Page) {
  await expect(page.getByTitle('首页')).toBeVisible()
  await expect(page.getByTitle('上一页')).toBeVisible()
  await expect(page.getByTitle('下一页')).toBeVisible()
  await expect(page.getByTitle('末页')).toBeVisible()
  await expect(page.getByRole('button', { name: '确定' })).toBeVisible()

  const jumpInput = page.locator('.pager-input input')
  await jumpInput.fill('1')
  await page.getByRole('button', { name: '确定' }).click()

  const sizeTrigger = page.locator('.table-pager-size .el-select').first()
  await sizeTrigger.click()
  const sizeOption = page.locator('.el-select-dropdown__item').filter({ hasText: '20 条/页' }).first()
  if (await sizeOption.isVisible().catch(() => false)) {
    await sizeOption.click()
  } else {
    await page.keyboard.press('Escape')
  }
}

test('分页组件在主要列表页交互正常 @full', async ({ page }) => {
  await login(page, accounts.student.accountNo, accounts.student.password, accounts.student.home)

  await gotoAndReady(page, '/student/eval/course')
  await assertNoFatalSignals(page)
  await assertPagerBasics(page)

  await gotoAndReady(page, '/student/feedback/mine')
  await assertNoFatalSignals(page)
  await assertPagerBasics(page)

  await logout(page)
})
