import { STORAGE_STATE_PATH } from '../../src/config/env';
import { test, expect } from '../../src/fixtures/test';
import { buildCustomer, missingCustomerFieldCases } from '../../src/test-data/customers';
import { checkoutErrors } from '../../src/test-data/messages';
import { basketScenarios, products } from '../../src/test-data/products';
import type { CartLine } from '../../src/types/models';
import { sumCents } from '../../src/utils/price';

test.use({ storageState: STORAGE_STATE_PATH });

const byName = (a: CartLine, b: CartLine): number => a.name.localeCompare(b.name);

test.describe('Checkout', { tag: '@regression' }, () => {
  test.describe('customer information', () => {
    test.beforeEach(async ({ inventoryPage, cartPage, checkoutInformationPage }) => {
      await inventoryPage.goto();
      await inventoryPage.addProductToCart(products.backpack);
      await inventoryPage.header.openCart();
      await cartPage.checkout();
      await checkoutInformationPage.expectToBeOpen();
    });

    for (const { field, label, expectedError } of missingCustomerFieldCases) {
      test(
        `checkout is blocked when the ${label} is missing`,
        { tag: '@negative' },
        async ({ checkoutInformationPage }) => {
          await checkoutInformationPage.submitCustomerDetails(buildCustomer({ [field]: '' }));

          await expect(checkoutInformationPage.errorMessage).toHaveText(expectedError);
          await checkoutInformationPage.expectToBeOpen();
        },
      );
    }

    test(
      'submitting an empty form reports the first missing field',
      { tag: '@negative' },
      async ({ checkoutInformationPage }) => {
        await checkoutInformationPage.continue();

        await expect(checkoutInformationPage.errorMessage).toHaveText(checkoutErrors.firstNameRequired);
        await checkoutInformationPage.expectToBeOpen();
      },
    );

    test('cancelling returns to the cart without losing items', async ({
      checkoutInformationPage,
      cartPage,
    }) => {
      await checkoutInformationPage.cancel();

      await cartPage.expectToBeOpen();
      await expect(cartPage.cartItems.item(products.backpack)).toBeVisible();
    });
  });

  test.describe('order totals', () => {
    for (const scenario of basketScenarios) {
      test(`the overview shows correct totals for ${scenario.description}`, async ({
        inventoryPage,
        cartPage,
        checkoutInformationPage,
        checkoutOverviewPage,
      }) => {
        await inventoryPage.goto();
        const expectedLines = await inventoryPage.addProductsToCart(scenario.items);
        await inventoryPage.header.openCart();
        await cartPage.checkout();
        await checkoutInformationPage.submitCustomerDetails(buildCustomer());

        await checkoutOverviewPage.expectToBeOpen();
        await expect(checkoutOverviewPage.cartItems.items).toHaveCount(expectedLines.length);
        const actualLines = await checkoutOverviewPage.cartItems.getLines();
        expect([...actualLines].sort(byName), 'overview lines should match the selected products').toEqual(
          [...expectedLines].sort(byName),
        );

        const summary = await checkoutOverviewPage.getOrderSummary();
        const expectedItemTotal = sumCents(expectedLines.map((line) => line.priceCents * line.quantity));
        expect(summary.itemTotalCents, 'item total should equal the sum of line prices').toBe(
          expectedItemTotal,
        );
        expect(summary.taxCents, 'tax should be charged').toBeGreaterThan(0);
        expect(summary.totalCents, 'total should equal item total plus tax').toBe(
          summary.itemTotalCents + summary.taxCents,
        );
      });
    }
  });

  /**
   * These tests describe the CORRECT behaviour and currently fail because of defects in the demo app.
   * `test.fail()` keeps the suite green while the defect exists and turns the test red the day it is fixed,
   * prompting removal of the annotation. See docs/known-issues.md.
   */
  test.describe('known defects', { tag: '@known-defect' }, () => {
    test('KD-UI-01: checkout should not be possible with an empty cart', async ({ cartPage }) => {
      test.fail(true, 'KD-UI-01: SauceDemo lets a user start checkout with an empty cart');

      await cartPage.goto();
      await expect(cartPage.cartItems.items).toHaveCount(0);

      await cartPage.checkout();

      await cartPage.expectToBeOpen();
    });

    test('KD-UI-02: whitespace-only customer details should be rejected', async ({
      inventoryPage,
      cartPage,
      checkoutInformationPage,
    }) => {
      test.fail(true, 'KD-UI-02: SauceDemo accepts names and postal codes made only of spaces');

      await inventoryPage.goto();
      await inventoryPage.addProductToCart(products.onesie);
      await inventoryPage.header.openCart();
      await cartPage.checkout();

      await checkoutInformationPage.submitCustomerDetails({
        firstName: '   ',
        lastName: '   ',
        postalCode: '   ',
      });

      await expect(checkoutInformationPage.errorMessage).toBeVisible();
      await checkoutInformationPage.expectToBeOpen();
    });
  });
});
