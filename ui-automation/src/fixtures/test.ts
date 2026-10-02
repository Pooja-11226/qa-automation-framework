import { test as base, expect } from '@playwright/test';
import { CartPage } from '../pages/CartPage';
import { CheckoutCompletePage } from '../pages/CheckoutCompletePage';
import { CheckoutInformationPage } from '../pages/CheckoutInformationPage';
import { CheckoutOverviewPage } from '../pages/CheckoutOverviewPage';
import { InventoryPage } from '../pages/InventoryPage';
import { LoginPage } from '../pages/LoginPage';
import { logger } from '../utils/logger';

interface PageObjects {
  loginPage: LoginPage;
  inventoryPage: InventoryPage;
  cartPage: CartPage;
  checkoutInformationPage: CheckoutInformationPage;
  checkoutOverviewPage: CheckoutOverviewPage;
  checkoutCompletePage: CheckoutCompletePage;
}

interface AutoFixtures {
  testLifecycleLogger: void;
}

/**
 * Project-wide `test`: injects page objects so specs never call `new XxxPage(page)`,
 * and logs the start/end of every test for easier correlation in CI logs.
 */
export const test = base.extend<PageObjects & AutoFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  inventoryPage: async ({ page }, use) => {
    await use(new InventoryPage(page));
  },
  cartPage: async ({ page }, use) => {
    await use(new CartPage(page));
  },
  checkoutInformationPage: async ({ page }, use) => {
    await use(new CheckoutInformationPage(page));
  },
  checkoutOverviewPage: async ({ page }, use) => {
    await use(new CheckoutOverviewPage(page));
  },
  checkoutCompletePage: async ({ page }, use) => {
    await use(new CheckoutCompletePage(page));
  },
  testLifecycleLogger: [
    // eslint-disable-next-line no-empty-pattern
    async ({}, use, testInfo) => {
      const startedAt = Date.now();
      logger.info(`START ${testInfo.titlePath.join(' > ')}`, { project: testInfo.project.name });
      await use();
      const outcome = testInfo.status === testInfo.expectedStatus ? 'as expected' : 'UNEXPECTED';
      logger.info(`END   ${testInfo.title}: ${testInfo.status} (${outcome}) in ${Date.now() - startedAt} ms`);
    },
    { auto: true },
  ],
});

export { expect };
