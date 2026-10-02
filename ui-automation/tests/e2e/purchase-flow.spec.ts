import { test, expect } from '../../src/fixtures/test';
import { buildCustomer } from '../../src/test-data/customers';
import { orderDetails } from '../../src/test-data/messages';
import { products } from '../../src/test-data/products';
import { users } from '../../src/test-data/users';

/**
 * The end-to-end scenario required by the assignment. It deliberately logs in through the UI
 * (no saved session) and mirrors each step of the brief with a named test.step.
 */
test.describe('Purchase flow', { tag: ['@e2e', '@smoke'] }, () => {
  test('a standard user can buy a product from login to order confirmation', async ({
    loginPage,
    inventoryPage,
    cartPage,
    checkoutInformationPage,
    checkoutOverviewPage,
    checkoutCompletePage,
  }) => {
    const productName = products.backpack;
    const customer = buildCustomer();

    await test.step('Launch the application', async () => {
      await loginPage.goto();
      await loginPage.expectToBeOpen();
    });

    await test.step('Log in and verify the login succeeded', async () => {
      await loginPage.login(users.standard);
      await inventoryPage.expectToBeOpen();
      await expect(inventoryPage.header.cartBadge, 'cart should start empty').toBeHidden();
    });

    const selectedProduct = await test.step('Select a product and add it to the cart', async () => {
      await expect(inventoryPage.productCard(productName)).toBeVisible();
      const line = await inventoryPage.addProductToCart(productName);
      await expect(inventoryPage.header.cartBadge).toHaveText('1');
      await expect(inventoryPage.removeButton(productName)).toBeVisible();
      return line;
    });

    await test.step('Verify the cart details', async () => {
      await inventoryPage.header.openCart();
      await cartPage.expectToBeOpen();
      await expect(cartPage.cartItems.items).toHaveCount(1);
      expect(await cartPage.cartItems.getLines(), 'cart should contain exactly the selected product').toEqual(
        [selectedProduct],
      );
    });

    await test.step('Proceed to checkout and enter customer details', async () => {
      await cartPage.checkout();
      await checkoutInformationPage.expectToBeOpen();
      await checkoutInformationPage.submitCustomerDetails(customer);
    });

    await test.step('Verify the order overview', async () => {
      await checkoutOverviewPage.expectToBeOpen();
      await expect(checkoutOverviewPage.cartItems.items).toHaveCount(1);
      expect(await checkoutOverviewPage.cartItems.getLines()).toEqual([selectedProduct]);
      await expect(checkoutOverviewPage.paymentInformation).toHaveText(orderDetails.paymentInformation);
      await expect(checkoutOverviewPage.shippingInformation).toHaveText(orderDetails.shippingInformation);

      const summary = await checkoutOverviewPage.getOrderSummary();
      expect(summary.itemTotalCents, 'item total should equal the product price').toBe(
        selectedProduct.priceCents,
      );
      expect(summary.totalCents, 'total should equal item total plus tax').toBe(
        summary.itemTotalCents + summary.taxCents,
      );
    });

    await test.step('Complete the checkout', async () => {
      await checkoutOverviewPage.finish();
    });

    await test.step('Verify the order confirmation', async () => {
      await checkoutCompletePage.expectToBeOpen();
      await expect(checkoutCompletePage.confirmationHeader).toHaveText(orderDetails.confirmationHeader);
      await expect(
        checkoutCompletePage.header.cartBadge,
        'cart should be emptied after ordering',
      ).toBeHidden();

      await checkoutCompletePage.backHome();
      await inventoryPage.expectToBeOpen();
    });
  });
});
