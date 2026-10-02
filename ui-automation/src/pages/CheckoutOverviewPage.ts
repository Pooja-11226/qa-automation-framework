import type { Locator, Page } from '@playwright/test';
import { CartItemList } from '../components/CartItemList';
import { pageTitles } from '../test-data/messages';
import type { OrderSummary } from '../types/models';
import { parsePriceToCents } from '../utils/price';
import { SecuredPage } from './SecuredPage';

export class CheckoutOverviewPage extends SecuredPage {
  protected readonly path = '/checkout-step-two.html';
  protected readonly expectedTitle = pageTitles.checkoutOverview;
  readonly cartItems: CartItemList;
  readonly paymentInformation: Locator;
  readonly shippingInformation: Locator;
  readonly itemTotalLabel: Locator;
  readonly taxLabel: Locator;
  readonly totalLabel: Locator;
  readonly finishButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.cartItems = new CartItemList(page);
    this.paymentInformation = page.getByTestId('payment-info-value');
    this.shippingInformation = page.getByTestId('shipping-info-value');
    this.itemTotalLabel = page.getByTestId('subtotal-label');
    this.taxLabel = page.getByTestId('tax-label');
    this.totalLabel = page.getByTestId('total-label');
    this.finishButton = page.getByTestId('finish');
    this.cancelButton = page.getByTestId('cancel');
  }

  async getOrderSummary(): Promise<OrderSummary> {
    const [itemTotal, tax, total] = await Promise.all([
      this.itemTotalLabel.innerText(),
      this.taxLabel.innerText(),
      this.totalLabel.innerText(),
    ]);
    return {
      itemTotalCents: parsePriceToCents(itemTotal),
      taxCents: parsePriceToCents(tax),
      totalCents: parsePriceToCents(total),
    };
  }

  async finish(): Promise<void> {
    await this.finishButton.click();
  }
}
