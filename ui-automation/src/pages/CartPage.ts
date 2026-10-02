import type { Locator, Page } from '@playwright/test';
import { CartItemList } from '../components/CartItemList';
import { pageTitles } from '../test-data/messages';
import { SecuredPage } from './SecuredPage';

export class CartPage extends SecuredPage {
  protected readonly path = '/cart.html';
  protected readonly expectedTitle = pageTitles.cart;
  readonly cartItems: CartItemList;
  readonly checkoutButton: Locator;
  readonly continueShoppingButton: Locator;

  constructor(page: Page) {
    super(page);
    this.cartItems = new CartItemList(page);
    this.checkoutButton = page.getByTestId('checkout');
    this.continueShoppingButton = page.getByTestId('continue-shopping');
  }

  removeButton(productName: string): Locator {
    return this.cartItems.item(productName).getByRole('button', { name: 'Remove' });
  }

  async removeProduct(productName: string): Promise<void> {
    await this.removeButton(productName).click();
  }

  async checkout(): Promise<void> {
    await this.checkoutButton.click();
  }

  async continueShopping(): Promise<void> {
    await this.continueShoppingButton.click();
  }
}
