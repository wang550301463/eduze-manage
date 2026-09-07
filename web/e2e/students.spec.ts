import { expect, test } from '@playwright/test';

async function loginAsAdmin(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('admin@123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('/');
}

test.describe('students', () => {
  test('navigate to student list', async ({ page }) => {
    await loginAsAdmin(page);
    await page.getByRole('link', { name: '学员' }).click();
    await expect(page).toHaveURL(/\/students/);
    await expect(page.getByTestId('student-list-page')).toBeVisible();
  });
});
