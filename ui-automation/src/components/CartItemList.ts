import type { Locator, Page } from '@playwright/test';
import type { CartLine } from '../types/models';
import { parsePriceToCents } from '../utils/price';

/**
 * The list of line items rendered on both the cart page and the checkout overview page.
 * Modelled once as a component to avoid duplicating the same locators in two page objects.
 */
export class CartItemList {
  readonly items: Locator;

  constructor(private readonly page: Page) {
    this.items = page.getByTestId('inventory-item');
  }

  item(productName: string): Locator {
    return this.items.filter({ has: this.page.getByText(productName, { exact: true }) });
  }

  /**
   * Reads every rendered line. Callers should first assert the expected count with
   * `expect(list.items).toHaveCount(n)` so the list is fully rendered before it is read.
   */
  async getLines(): Promise<CartLine[]> {
    const lines: CartLine[] = [];
    for (const row of await this.items.all()) {
      const [name, price, quantity] = await Promise.all([
        row.getByTestId('inventory-item-name').innerText(),
        row.getByTestId('inventory-item-price').innerText(),
        row.getByTestId('item-quantity').innerText(),
      ]);
      lines.push({ name: name.trim(), priceCents: parsePriceToCents(price), quantity: Number(quantity) });
    }
    return lines;
  }
}
