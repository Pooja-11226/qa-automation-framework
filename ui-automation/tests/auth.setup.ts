import { STORAGE_STATE_PATH } from '../src/config/env';
import { test as setup } from '../src/fixtures/test';
import { users } from '../src/test-data/users';

/**
 * Runs once before the browser projects. Saves the logged-in session (SauceDemo uses a
 * `session-username` cookie with a 10-minute lifetime) so tests that are not *about* login
 * do not repeat it and do not fail because of an unrelated login problem.
 */
setup('authenticate as the standard user', async ({ page, loginPage, inventoryPage }) => {
  await loginPage.goto();
  await loginPage.login(users.standard);
  await inventoryPage.expectToBeOpen();
  await page.context().storageState({ path: STORAGE_STATE_PATH });
});
