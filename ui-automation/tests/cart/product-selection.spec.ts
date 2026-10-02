import { STORAGE_STATE_PATH } from '../../src/config/env';
import { test, expect } from '../../src/fixtures/test';
import type { SortOption } from '../../src/types/models';

test.use({ storageState: STORAGE_STATE_PATH });

const priceSortCases: ReadonlyArray<{ option: SortOption; label: string; direction: 1 | -1 }> = [
  { option: 'lohi', label: 'price, low to high', direction: 1 },
  { option: 'hilo', label: 'price, high to low', direction: -1 },
];

const nameSortCases: ReadonlyArray<{ option: SortOption; label: string; direction: 1 | -1 }> = [
  { option: 'az', label: 'name, A to Z', direction: 1 },
  { option: 'za', label: 'name, Z to A', direction: -1 },
];

test.describe('Product selection', { tag: '@regression' }, () => {
  test.beforeEach(async ({ inventoryPage }) => {
    await inventoryPage.goto();
    await inventoryPage.expectToBeOpen();
  });

  for (const { option, label, direction } of priceSortCases) {
    test(`products can be sorted by ${label}`, async ({ inventoryPage }) => {
      await inventoryPage.sortBy(option);

      const prices = await inventoryPage.getDisplayedPricesCents();
      const expectedOrder = [...prices].sort((a, b) => (a - b) * direction);
      expect(prices, `prices should be ordered by ${label}`).toEqual(expectedOrder);
    });
  }

  for (const { option, label, direction } of nameSortCases) {
    test(`products can be sorted by ${label}`, async ({ inventoryPage }) => {
      await inventoryPage.sortBy(option);

      const names = await inventoryPage.getDisplayedNames();
      const expectedOrder = [...names].sort((a, b) => a.localeCompare(b) * direction);
      expect(names, `names should be ordered by ${label}`).toEqual(expectedOrder);
    });
  }
});
