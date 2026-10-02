import { expect, type Locator, type Page } from '@playwright/test';
import { HeaderComponent } from '../components/HeaderComponent';
import { BasePage } from './BasePage';

/** A page that is only reachable after login: it has the shared header and a page title. */
export abstract class SecuredPage extends BasePage {
  protected abstract readonly expectedTitle: string;
  readonly header: HeaderComponent;
  readonly title: Locator;

  constructor(page: Page) {
    super(page);
    this.header = new HeaderComponent(page);
    this.title = page.getByTestId('title');
  }

  override async expectToBeOpen(): Promise<void> {
    await super.expectToBeOpen();
    await expect(this.title, `${this.constructor.name} should show its title`).toHaveText(this.expectedTitle);
  }
}
