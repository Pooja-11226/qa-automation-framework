import { defineConfig, devices, type Project } from '@playwright/test';
import { env } from './src/config/env';

const isCI = Boolean(process.env.CI);

const browserDevices = {
  chromium: devices['Desktop Chrome'],
  firefox: devices['Desktop Firefox'],
  webkit: devices['Desktop Safari'],
} as const;

type BrowserName = keyof typeof browserDevices;

/** BROWSERS=chromium,firefox enables cross-browser runs without editing this file. Default: chromium. */
function selectedBrowsers(): BrowserName[] {
  const requested = (process.env.BROWSERS ?? 'chromium')
    .split(',')
    .map((name) => name.trim())
    .filter(Boolean);
  for (const name of requested) {
    if (!(name in browserDevices)) {
      throw new Error(
        `Unknown browser "${name}" in BROWSERS. Use: ${Object.keys(browserDevices).join(', ')}`,
      );
    }
  }
  return requested as BrowserName[];
}

const browserProjects: Project[] = selectedBrowsers().map((name) => ({
  name,
  use: { ...browserDevices[name] },
  dependencies: ['setup'],
}));

export default defineConfig({
  testDir: './tests',
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: isCI,
  // Retry once in CI only, to absorb network blips on a public demo site. Retries are visible in the
  // report as "flaky", so they never hide a real problem. Locally a failure should fail immediately.
  retries: isCI ? 1 : 0,
  workers: isCI ? 2 : undefined,
  timeout: env.timeouts.test,
  expect: { timeout: env.timeouts.expect },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  use: {
    baseURL: env.baseUrl,
    // SauceDemo exposes stable `data-test` attributes; make getByTestId() use them.
    testIdAttribute: 'data-test',
    actionTimeout: env.timeouts.action,
    navigationTimeout: env.timeouts.navigation,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: isCI ? 'on-first-retry' : 'retain-on-failure',
  },
  projects: [
    // Logs in once and saves the session so cart/checkout tests start already authenticated.
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    ...browserProjects,
  ],
});
