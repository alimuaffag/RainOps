import { defineConfig, devices } from '@playwright/test'
export default defineConfig({
  testDir: './tests',
  testMatch: 'demo.spec.ts',
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env.RAINOPS_DEMO_URL || 'http://127.0.0.1:8082',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
})
