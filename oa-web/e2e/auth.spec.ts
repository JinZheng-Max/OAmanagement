import { expect, test } from '@playwright/test'

test('employee can login through the guarded route', async ({ page }) => {
  await page.route('**/api/auth/login', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
    code: 200, message: '登录成功', traceId: 'test-trace', data: { token: 'signed-token', tokenType: 'Bearer', expiresIn: 1800,
      userInfo: { id: 2, username: 'alice', role: 'EMPLOYEE', employeeId: 8 } }
  }) }))
  await page.goto('/login')
  await page.getByRole('textbox', { name: '用户名' }).fill('alice')
  await page.getByRole('textbox', { name: '密码' }).fill('StrongPass1')
  await page.getByTestId('login').click()
  await expect(page).toHaveURL('http://127.0.0.1:5173/')
  await expect(page.getByText('欢迎，alice')).toBeVisible()
})
