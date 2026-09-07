import { expect, test } from '@playwright/test';

async function loginAsAdmin(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('admin@123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('/');
}

test.describe('settings navigation', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('branch page', async ({ page }) => {
    await page.getByRole('link', { name: '校区' }).click();
    await expect(page).toHaveURL('/settings/branches');
    await expect(page.getByTestId('branch-list-page')).toBeVisible();
    await expect(page.getByRole('heading', { name: '校区' })).toBeVisible();
  });

  test('user page', async ({ page }) => {
    await page.getByRole('link', { name: '账号' }).click();
    await expect(page).toHaveURL('/settings/users');
    await expect(page.getByTestId('user-list-page')).toBeVisible();
  });

  test('role page', async ({ page }) => {
    await page.getByRole('link', { name: '角色' }).click();
    await expect(page).toHaveURL('/settings/roles');
    await expect(page.getByTestId('role-list-page')).toBeVisible();
  });
});
