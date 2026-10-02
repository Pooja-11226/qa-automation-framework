import { expect, type Locator, type Page } from '@playwright/test';
import { pageTitles } from '../test-data/messages';
import type { CartLine, SortOption } from '../types/models';
import { parsePriceToCents } from '../utils/price';
import { SecuredPage } from './SecuredPage';

export class InventoryPage extends SecuredPage {
  protected readonly path = '/inventory.html';
  protected readonly expectedTitle = pageTitles.inventory;
  readonly productCards: Locator;
  readonly productNames: Locator;
  readonly productPrices: Locator;
  readonly sortDropdown: Locator;

  constructor(page: Page) {
    super(page);
    this.productCards = page.getByTestId('inventory-item');
    this.productNames = page.getByTestId('inventory-item-name');
    this.productPrices = page.getByTestId('inventory-item-price');
    this.sortDropdown = page.getByTestId('product-sort-container');
  }

  /** Locates a product card by its visible name rather than by generated ids. */
  productCard(productName: string): Locator {
    return this.productCards.filter({ has: this.page.getByText(productName, { exact: true }) });
  }

  addToCartButton(productName: string): Locator {
    return this.productCard(productName).getByRole('button', { name: 'Add to cart' });
  }

  removeButton(productName: string): Locator {
    return this.productCard(productName).getByRole('button', { name: 'Remove' });
  }

  /** Reads a product's details as displayed, so expected values are captured rather than hard-coded. */
  async getProduct(productName: string): Promise<CartLine> {
    const priceText = await this.productCard(productName).getByTestId('inventory-item-price').innerText();
    return { name: productName, priceCents: parsePriceToCents(priceText), quantity: 1 };
  }

  /** Adds a product and returns the line the cart is now expected to contain. */
  async addProductToCart(productName: string): Promise<CartLine> {
    const expectedLine = await this.getProduct(productName);
    await this.addToCartButton(productName).click();
    return expectedLine;
  }

  async addProductsToCart(productNames: readonly string[]): Promise<CartLine[]> {
    const lines: CartLine[] = [];
    for (const productName of productNames) {
      lines.push(await this.addProductToCart(productName));
    }
    return lines;
  }

  async removeProductFromCart(productName: string): Promise<void> {
    await this.removeButton(productName).click();
  }

  async sortBy(option: SortOption): Promise<void> {
    await this.sortDropdown.selectOption(option);
  }

  async getDisplayedNames(): Promise<string[]> {
    await expect(this.productNames.first()).toBeVisible();
    return (await this.productNames.allInnerTexts()).map((name) => name.trim());
  }

  async getDisplayedPricesCents(): Promise<number[]> {
    await expect(this.productPrices.first()).toBeVisible();
    return (await this.productPrices.allInnerTexts()).map(parsePriceToCents);
  }
}
