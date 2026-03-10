import { expect, test } from '@playwright/test'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import * as XLSX from 'xlsx'
import { accounts, assertNoFatalSignals, expectWorkspaceTitle, gotoAndReady, login, logout } from './helpers'

function buildWorkbookFile(headers: string[], rows: string[][]) {
  const tempDir = mkdtempSync(join(tmpdir(), 'student-eval-accounts-'))
  const filePath = join(tempDir, 'accounts.xlsx')
  const worksheet = XLSX.utils.aoa_to_sheet([headers, ...rows])
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, 'accounts')
  const content = XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' })
  writeFileSync(filePath, content)
  return filePath
}

async function chooseMoreAction(row, page, label) {
  await row.getByRole('button', { name: '更多' }).click()
  await page.getByRole('menuitem', { name: label }).click()
}

test('管理员账号管理流程 @full', async ({ page }) => {
  const stamp = `${Date.now()}`
  const studentAccountNo = `2022${stamp.slice(-6)}`
  const counselorAccountNo = `9900${stamp.slice(-6)}`
  const importedCounselorNo = `9800${stamp.slice(-6)}`

  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/accounts')
  await assertNoFatalSignals(page)
  await expectWorkspaceTitle(page, '账号管理')
  await expect(page.getByText('账号由管理员统一创建，默认初始密码为')).toBeVisible()
  await page.locator('.account-filter-row .el-select').nth(0).click()
  await expect(page.getByRole('option', { name: '全部角色' })).toBeVisible()
  await page.keyboard.press('Escape')
  await page.locator('.account-filter-row .el-select').nth(1).click()
  await expect(page.getByRole('option', { name: '全部状态' })).toBeVisible()
  await page.keyboard.press('Escape')

  await page.getByTestId('account-create-open').click()
  await page.getByTestId('account-form-role').selectOption('STUDENT')
  await page.getByTestId('account-form-accountNo').fill(studentAccountNo)
  await page.getByTestId('account-form-realName').fill('测试学生账号')
  await page.getByTestId('account-form-gender').selectOption('男')
  await page.getByTestId('account-form-className').fill('2022级数据科学与大数据技术1班')
  await page.getByTestId('account-form-submit').click()
  await expect(page.locator('table tbody')).toContainText(studentAccountNo)

  await page.getByTestId('account-create-open').click()
  await page.getByTestId('account-form-role').selectOption('COUNSELOR')
  await page.getByTestId('account-form-accountNo').fill(counselorAccountNo)
  await page.getByTestId('account-form-realName').fill('测试辅导员账号')
  await page.getByTestId('account-form-gender').selectOption('女')
  await page.getByTestId('account-form-submit').click()
  await expect(page.locator('table tbody')).toContainText(counselorAccountNo)

  const counselorRow = page.locator('tr', { hasText: counselorAccountNo })
  await counselorRow.getByRole('button', { name: '停用' }).click()
  await page.getByRole('button', { name: '确认停用' }).click()
  await expect(page.getByText('账号已停用')).toBeVisible()

  await logout(page)
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('请输入学号/工号').fill(counselorAccountNo)
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByText('学号/工号或密码错误')).toBeVisible()

  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/accounts')
  const counselorRowAfterBack = page.locator('tr', { hasText: counselorAccountNo })
  await counselorRowAfterBack.getByRole('button', { name: '启用' }).click()
  await page.getByRole('button', { name: '确认启用' }).click()
  await chooseMoreAction(counselorRowAfterBack, page, '重置密码')
  await page.getByRole('button', { name: '确认重置' }).click()
  await expect(page.getByText(`账号 ${counselorAccountNo} 的密码已重置为 123456`)).toBeVisible()

  await logout(page)
  await login(page, counselorAccountNo, '123456', /\/teacher\//)
  await assertNoFatalSignals(page)
  await logout(page)

  await login(page, accounts.admin.accountNo, accounts.admin.password, accounts.admin.home)
  await gotoAndReady(page, '/admin/accounts')

  const importFile = buildWorkbookFile(
    ['工号', '姓名', '性别', '联系电话'],
    [
      [importedCounselorNo, '导入辅导员', '女', '13800009999'],
      ['9000000002', '重复辅导员', '男', '13800000002'],
      ['9800999998', '', '男', '13800009998']
    ]
  )

  await page.getByTestId('account-import-open').click()
  await page.getByTestId('account-import-role').selectOption('COUNSELOR')
  await page.getByTestId('account-import-file').setInputFiles(importFile)
  await page.getByTestId('account-import-preview').click()
  await expect(page.getByText('预校验结果')).toBeVisible()
  await expect(page.locator('.preview-stat.success')).toContainText('可导入')
  await expect(page.locator('.preview-stat.danger')).toContainText('错误')
  await page.getByTestId('account-import-commit').click()
  await expect(page.getByRole('heading', { name: '批量导入账号' })).toHaveCount(0)
  await expect(page.getByText('导入完成：成功 1，失败 0')).toBeVisible()
  await expect(page.locator('table tbody')).toContainText(importedCounselorNo)

  await page.getByPlaceholder('搜索学号/工号/姓名/班级').fill('9000000002')
  await page.getByPlaceholder('搜索学号/工号/姓名/班级').press('Enter')
  const protectedRow = page.locator('tr', { hasText: '9000000002' })
  await expect(protectedRow.getByRole('button', { name: '更多' })).toHaveAttribute('title', /不能删除|不可删除/)

  await page.getByPlaceholder('搜索学号/工号/姓名/班级').fill(importedCounselorNo)
  await page.getByPlaceholder('搜索学号/工号/姓名/班级').press('Enter')
  const importedCounselorRow = page.locator('tr', { hasText: importedCounselorNo })
  await chooseMoreAction(importedCounselorRow, page, '删除账号')
  await page.getByRole('button', { name: '确认删除' }).click()
  await expect(page.locator('table tbody')).not.toContainText(importedCounselorNo)

  await page.getByPlaceholder('搜索学号/工号/姓名/班级').fill(studentAccountNo)
  await page.getByPlaceholder('搜索学号/工号/姓名/班级').press('Enter')
  const studentRow = page.locator('tr', { hasText: studentAccountNo })
  await chooseMoreAction(studentRow, page, '删除账号')
  await page.getByRole('button', { name: '确认删除' }).click()
  await expect(page.locator('table tbody')).not.toContainText(studentAccountNo)

  await page.getByPlaceholder('搜索学号/工号/姓名/班级').fill(counselorAccountNo)
  await page.getByPlaceholder('搜索学号/工号/姓名/班级').press('Enter')
  const finalCounselorRow = page.locator('tr', { hasText: counselorAccountNo })
  await chooseMoreAction(finalCounselorRow, page, '删除账号')
  await page.getByRole('button', { name: '确认删除' }).click()
  await expect(page.locator('table tbody')).not.toContainText(counselorAccountNo)

  await logout(page)
})

