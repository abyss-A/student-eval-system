import { expect, test } from '@playwright/test'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

test('管理员核心页面可访问 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)

  const routes = [
    ['/admin/submissions', '测评单查看'],
    ['/admin/counselor/scopes', '班级权限管理'],
    ['/admin/accounts', '账号管理'],
    ['/admin/semesters', '学期管理'],
    ['/admin/notices', '公告管理'],
    ['/admin/feedback/handle', '反馈处理'],
    ['/admin/ranking', '综合排名'],
    ['/admin/me/profile', '账号中心']
  ] as const

  for (const [route, title] of routes) {
    await gotoAndReady(page, route)
    await assertNoFatalSignals(page)
    await expectWorkspaceTitle(page, title)
  }

  await logout(page)
})

test('班级权限管理页交互检查 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/counselor/scopes')
  await assertNoFatalSignals(page)

  await page.getByPlaceholder('搜索工号/姓名').waitFor({ state: 'visible' })

  const firstRow = page.locator('table tbody tr').first()
  if (await firstRow.isVisible().catch(() => false)) {
    await firstRow.click()
    await page.getByPlaceholder('搜索班级').waitFor({ state: 'visible', timeout: 10_000 })
    await page.getByText('班级权限配置').waitFor({ state: 'visible' })
    await page.getByRole('button', { name: '保存' }).waitFor({ state: 'visible' })
    await page.keyboard.press('Escape')
  }

  await logout(page)
})

test('长文本浮层不会超出边界 @full', async ({ page }) => {
  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/submissions')
  await assertNoFatalSignals(page)

  const overflowCell = page.locator('.table-overflow-cell').first()
  await overflowCell.waitFor({ state: 'visible' })
  await overflowCell.hover()

  const toggle = overflowCell.locator('.table-overflow-cell__toggle')
  await toggle.waitFor({ state: 'visible' })
  await toggle.click()

  const bounds = await page.evaluate(() => {
    const pickVisible = (selector) =>
      Array.from(document.querySelectorAll(selector))
        .filter((element) => {
          const rect = element.getBoundingClientRect()
          return rect.width > 0 && rect.height > 0
        })
        .sort((left, right) => {
          const leftRect = left.getBoundingClientRect()
          const rightRect = right.getBoundingClientRect()
          return rightRect.width * rightRect.height - leftRect.width * leftRect.height
        })[0]

    const popperElement = pickVisible('.table-overflow-cell-popper')
    const popoverElement = pickVisible('.table-overflow-cell__popover')
    const textElement = pickVisible('.table-overflow-cell__popover-text')
    const popper = popperElement?.getBoundingClientRect()
    const popover = popoverElement?.getBoundingClientRect()
    const text = textElement?.getBoundingClientRect()
    const textStyle = textElement ? window.getComputedStyle(textElement) : null

    let naturalSingleLineWidth = 0
    if (textElement && textStyle) {
      const probe = document.createElement('span')
      probe.textContent = textElement.textContent || ''
      probe.style.position = 'fixed'
      probe.style.left = '-9999px'
      probe.style.top = '0'
      probe.style.visibility = 'hidden'
      probe.style.whiteSpace = 'nowrap'
      probe.style.font = textStyle.font
      probe.style.fontFamily = textStyle.fontFamily
      probe.style.fontSize = textStyle.fontSize
      probe.style.fontWeight = textStyle.fontWeight
      probe.style.letterSpacing = textStyle.letterSpacing
      probe.style.lineHeight = textStyle.lineHeight
      document.body.appendChild(probe)
      naturalSingleLineWidth = probe.getBoundingClientRect().width
      probe.remove()
    }

    const lineHeight = textStyle ? Number.parseFloat(textStyle.lineHeight || '0') : 0
    const lineCount = text && lineHeight ? text.height / lineHeight : 0
    const availableSingleLineWidth = Math.min(window.innerWidth * 0.88 - 92, 800)
    const clientWidth = textElement instanceof HTMLElement ? textElement.clientWidth : 0
    const scrollWidth = textElement instanceof HTMLElement ? textElement.scrollWidth : 0

    return {
      popperRight: popper?.right || 0,
      popperBottom: popper?.bottom || 0,
      popoverRight: popover?.right || 0,
      textRight: text?.right || 0,
      popoverBottom: popover?.bottom || 0,
      textBottom: text?.bottom || 0,
      naturalSingleLineWidth,
      availableSingleLineWidth,
      lineCount,
      clientWidth,
      scrollWidth
    }
  })

  expect(bounds.popperRight).toBeGreaterThan(0)
  expect(bounds.popoverRight).toBeLessThanOrEqual(bounds.popperRight + 1)
  expect(bounds.popoverBottom).toBeLessThanOrEqual(bounds.popperBottom + 1)
  expect(bounds.popoverRight).toBeGreaterThan(0)
  expect(bounds.textRight).toBeLessThanOrEqual(bounds.popoverRight + 1)
  expect(bounds.textBottom).toBeLessThanOrEqual(bounds.popoverBottom + 1)
  if (bounds.naturalSingleLineWidth <= bounds.availableSingleLineWidth) {
    expect(bounds.lineCount).toBeLessThanOrEqual(1.2)
    expect(bounds.scrollWidth).toBeLessThanOrEqual(bounds.clientWidth + 1)
  } else {
    expect(bounds.lineCount).toBeLessThanOrEqual(2.2)
  }

  await logout(page)
})
