import type { Locator, Page } from '@playwright/test';
import { pageTitles } from '../test-data/messages';
import type { CustomerDetails } from '../types/models';
import { SecuredPage } from './SecuredPage';

export class CheckoutInformationPage extends SecuredPage {
  protected readonly path = '/checkout-step-one.html';
  protected readonly expectedTitle = pageTitles.checkoutInformation;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly postalCodeInput: Locator;
  readonly continueButton: Locator;
  readonly cancelButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.firstNameInput = page.getByTestId('firstName');
    this.lastNameInput = page.getByTestId('lastName');
    this.postalCodeInput = page.getByTestId('postalCode');
    this.continueButton = page.getByTestId('continue');
    this.cancelButton = page.getByTestId('cancel');
    this.errorMessage = page.getByTestId('error');
  }

  async fillCustomerDetails(customer: CustomerDetails): Promise<void> {
    await this.firstNameInput.fill(customer.firstName);
    await this.lastNameInput.fill(customer.lastName);
    await this.postalCodeInput.fill(customer.postalCode);
  }

  async continue(): Promise<void> {
    await this.continueButton.click();
  }

  async submitCustomerDetails(customer: CustomerDetails): Promise<void> {
    await this.fillCustomerDetails(customer);
    await this.continue();
  }

  async cancel(): Promise<void> {
    await this.cancelButton.click();
  }
}
