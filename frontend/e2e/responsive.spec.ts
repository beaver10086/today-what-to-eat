import { expect, test } from '@playwright/test'

test('菜品列表可用、布局不横向溢出并保存设备截图', async ({ page }, testInfo) => {
  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    let data: unknown = { records: [], total: 0, page: 1, size: 12 }
    if (url.pathname.endsWith('/canteens') || url.pathname.endsWith('/shops') || url.pathname.endsWith('/tags'))
      data = { records: [], total: 0, page: 1, size: 100 }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'success', data }) })
  })

  await page.goto('/')
  await expect(page.getByRole('heading', { name: '找一份合口味的' })).toBeVisible()
  await expect(page.getByText('没有找到匹配的菜品')).toBeVisible()
  await expect.poll(async () => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: testInfo.outputPath(`device-${testInfo.project.name}.png`), fullPage: true })
})

test('未登录访问收藏页会被引导登录', async ({ page }) => {
  await page.goto('/favorites')
  await expect(page).toHaveURL(/\/login\?redirect=%2Ffavorites/)
})
