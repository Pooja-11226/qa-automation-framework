import { expect, type Locator, type Page } from '@playwright/test';
import type { UserCredentials } from '../types/models';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  protected readonly path = '/';
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly dismissErrorButton: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.getByTestId('username');
    this.passwordInput = page.getByTestId('password');
    this.loginButton = page.getByTestId('login-button');
    this.errorMessage = page.getByTestId('error');
    this.dismissErrorButton = page.getByTestId('error-button');
  }

  /** Submits the login form. Used for both valid and invalid attempts; the test asserts the outcome. */
  async login(credentials: UserCredentials): Promise<void> {
    await this.usernameInput.fill(credentials.username);
    await this.passwordInput.fill(credentials.password);
    await this.loginButton.click();
  }

  async dismissError(): Promise<void> {
    await this.dismissErrorButton.click();
  }

  override async expectToBeOpen(): Promise<void> {
    await super.expectToBeOpen();
    await expect(this.loginButton, 'login form should be displayed').toBeVisible();
  }
}
