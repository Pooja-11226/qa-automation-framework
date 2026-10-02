import { expect, type Page } from '@playwright/test';

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Behaviour shared by every page: knowing its own route, navigating to it,
 * and asserting that the browser is actually on it.
 */
export abstract class BasePage {
  /** Route relative to baseURL, e.g. "/cart.html". */
  protected abstract readonly path: string;

  constructor(protected readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto(this.path);
  }

  /** Page-identity check used after every navigation. Auto-waits; no manual sleeps needed. */
  async expectToBeOpen(): Promise<void> {
    await expect(this.page, `Expected the browser to be on ${this.constructor.name}`).toHaveURL(
      new RegExp(`${escapeRegExp(this.path)}$`),
    );
  }
}
