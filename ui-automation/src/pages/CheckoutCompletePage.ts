import type { Locator, Page } from '@playwright/test';
import { pageTitles } from '../test-data/messages';
import { SecuredPage } from './SecuredPage';

export class CheckoutCompletePage extends SecuredPage {
  protected readonly path = '/checkout-complete.html';
  protected readonly expectedTitle = pageTitles.checkoutComplete;
  readonly confirmationHeader: Locator;
  readonly confirmationText: Locator;
  readonly backHomeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.confirmationHeader = page.getByTestId('complete-header');
    this.confirmationText = page.getByTestId('complete-text');
    this.backHomeButton = page.getByTestId('back-to-products');
  }

  async backHome(): Promise<void> {
    await this.backHomeButton.click();
  }
}
