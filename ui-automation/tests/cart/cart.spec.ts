import { STORAGE_STATE_PATH } from '../../src/config/env';
import { test, expect } from '../../src/fixtures/test';
import { products } from '../../src/test-data/products';

// Every test gets a fresh browser context from the saved session, so each one starts with an empty cart.
test.use({ storageState: STORAGE_STATE_PATH });

test.describe('Shopping cart', { tag: '@regression' }, () => {
  test.beforeEach(async ({ inventoryPage }) => {
    await inventoryPage.goto();
    await inventoryPage.expectToBeOpen();
  });

  test('adding a product updates the badge and the cart shows its details', async ({
    inventoryPage,
    cartPage,
  }) => {
    const expectedLine = await inventoryPage.addProductToCart(products.backpack);

    await expect(inventoryPage.header.cartBadge).toHaveText('1');
    await expect(inventoryPage.removeButton(products.backpack)).toBeVisible();

    await inventoryPage.header.openCart();
    await cartPage.expectToBeOpen();
    await expect(cartPage.cartItems.items).toHaveCount(1);
    expect(await cartPage.cartItems.getLines(), 'cart line should match the product as listed').toEqual([
      expectedLine,
    ]);
  });

  test('removing products keeps the badge and cart contents accurate', async ({
    inventoryPage,
    cartPage,
  }) => {
    await inventoryPage.addProductsToCart([products.backpack, products.bikeLight]);
    await expect(inventoryPage.header.cartBadge).toHaveText('2');

    await inventoryPage.removeProductFromCart(products.bikeLight);
    await expect(inventoryPage.header.cartBadge).toHaveText('1');
    await expect(inventoryPage.addToCartButton(products.bikeLight)).toBeVisible();

    await inventoryPage.header.openCart();
    await expect(cartPage.cartItems.items).toHaveCount(1);
    await expect(cartPage.cartItems.item(products.backpack)).toBeVisible();

    await cartPage.removeProduct(products.backpack);
    await expect(cartPage.cartItems.items).toHaveCount(0);
    await expect(cartPage.header.cartBadge, 'badge should disappear when the cart is empty').toBeHidden();
  });

  test('cart contents survive a page reload', async ({ page, inventoryPage, cartPage }) => {
    const expectedLines = await inventoryPage.addProductsToCart([products.onesie, products.fleeceJacket]);

    await page.reload();

    await expect(inventoryPage.header.cartBadge).toHaveText(String(expectedLines.length));
    await inventoryPage.header.openCart();
    await expect(cartPage.cartItems.items).toHaveCount(expectedLines.length);
    expect(await cartPage.cartItems.getLines()).toEqual(expectedLines);
  });

  test('continue shopping returns to the inventory with the cart intact', async ({
    inventoryPage,
    cartPage,
  }) => {
    await inventoryPage.addProductToCart(products.boltTShirt);
    await inventoryPage.header.openCart();

    await cartPage.continueShopping();

    await inventoryPage.expectToBeOpen();
    await expect(inventoryPage.header.cartBadge).toHaveText('1');
    await expect(inventoryPage.removeButton(products.boltTShirt)).toBeVisible();
  });
});
