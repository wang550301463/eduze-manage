import { expect, test } from '@playwright/test';

test.describe('login', () => {
  test('admin login success', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('账号').fill('admin');
    await page.getByLabel('密码').fill('admin@123');
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page).toHaveURL('/');
    await expect(page.getByText('EduZE Manage')).toBeVisible();
  });

  test('wrong password shows error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('账号').fill('admin');
    await page.getByLabel('密码').fill('wrong-password');
    await page.getByRole('button', { name: '登录' }).click();
    await expect(page).toHaveURL('/login');
    await expect(page.getByText(/账号或密码错误|登录失败/)).toBeVisible();
  });
});
