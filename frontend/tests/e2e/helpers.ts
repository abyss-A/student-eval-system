import { expect, Page } from '@playwright/test'

export const accounts = {
  student: { accountNo: '2022000001', password: '123456', home: /\/student\// },
  counselor: { accountNo: '9000000002', password: '123456', home: /\/teacher\// },
  admin: { accountNo: '9000000001', password: '123456', home: /\/admin\// }
} as const

export async function login(page: Page, accountNo: string, password: string, homeUrl: RegExp) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('请输入学号/工号').fill(accountNo)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL(homeUrl)
}

export async function gotoAndReady(page: Page, path: string) {
  await page.goto(path, { waitUntil: 'networkidle' })
  await page.waitForLoadState('domcontentloaded')
}

export async function logout(page: Page) {
  const btn = page.getByRole('button', { name: '退出登录' }).first()
  if (await btn.isVisible().catch(() => false)) {
    await btn.click()
    await page.waitForURL(/\/login/)
  }
}

export async function assertNoFatalSignals(page: Page) {
  const bodyText = await page.locator('body').innerText()
  expect(bodyText).not.toContain('加载失败')
  expect(bodyText).not.toContain('无法连接后端')
  expect(bodyText).not.toContain('服务器异常')
  expect(bodyText).not.toContain('Network Error')
  expect(bodyText).not.toContain('Unhandled')
}

export async function expectWorkspaceTitle(page: Page, text: string) {
  await expect(page.locator('.workspace-title')).toHaveText(text)
}
