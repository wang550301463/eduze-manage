import { defineConfig } from '@playwright/test';

/** Local release-gate E2E: uses system Chrome to avoid Playwright browser download. */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:5173',
    channel: 'chrome',
    headless: true,
    screenshot: 'only-on-failure',
  },
});
