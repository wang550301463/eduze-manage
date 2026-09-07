import { expect, test } from '@playwright/test';

async function loginAsAdmin(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('admin@123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('/');
}

test.describe('attendance', () => {
  test('attendance nav placeholder', async ({ page }) => {
    await loginAsAdmin(page);
    await page.getByRole('link', { name: '签到', exact: true }).click();
    await expect(page).toHaveURL(/\/attendance/);
  });
});
