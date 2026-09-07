import { expect, test } from '@playwright/test';

async function loginAsAdmin(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('admin@123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('/');
}

test.describe('student create', () => {
  test('branch select populated in create dialog', async ({ page }) => {
    await loginAsAdmin(page);
    await page.getByRole('link', { name: '学员' }).click();
    await expect(page.getByTestId('student-list-page')).toBeVisible();

    await page.getByRole('button', { name: '新建学员' }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    const branchSelect = page.getByRole('combobox', { name: '校区' });
    await expect(branchSelect).toBeVisible();
    await branchSelect.click();
    await expect(page.getByRole('option').first()).toBeVisible();
  });
});
