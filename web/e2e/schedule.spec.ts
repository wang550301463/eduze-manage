import { expect, test } from '@playwright/test';

async function loginAsAdmin(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('admin@123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('/');
}

test.describe('schedule', () => {
  test('open schedule page with lesson query', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/schedule?openLessonId=1&date=2026-05-11');
    await expect(page.getByTestId('weekly-schedule-page')).toBeVisible();
    await expect(page.getByRole('heading', { name: '周课表' })).toBeVisible();
    await expect(page).toHaveURL(/openLessonId=1/);
  });
});
