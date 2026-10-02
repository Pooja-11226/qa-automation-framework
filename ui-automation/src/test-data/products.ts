/** Product names as displayed in the SauceDemo catalogue. Prices are read from the UI, never hard-coded. */
export const products = {
  backpack: 'Sauce Labs Backpack',
  bikeLight: 'Sauce Labs Bike Light',
  boltTShirt: 'Sauce Labs Bolt T-Shirt',
  fleeceJacket: 'Sauce Labs Fleece Jacket',
  onesie: 'Sauce Labs Onesie',
  redTShirt: 'Test.allTheThings() T-Shirt (Red)',
} as const;

export interface BasketScenario {
  readonly description: string;
  readonly items: readonly string[];
}

/** Data-driven baskets for order-total checks: low value, multiple items, and the full high-value range. */
export const basketScenarios: readonly BasketScenario[] = [
  { description: 'a single low-priced item', items: [products.onesie] },
  { description: 'two items', items: [products.backpack, products.bikeLight] },
  {
    description: 'three items including the most expensive product',
    items: [products.fleeceJacket, products.boltTShirt, products.redTShirt],
  },
];
