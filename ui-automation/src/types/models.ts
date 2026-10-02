/** Domain types shared by page objects, test data and tests. */

export interface UserCredentials {
  readonly username: string;
  readonly password: string;
}

export interface CustomerDetails {
  readonly firstName: string;
  readonly lastName: string;
  readonly postalCode: string;
}

/** One product line as shown on the inventory, cart and overview pages. Prices are kept in cents. */
export interface CartLine {
  readonly name: string;
  readonly priceCents: number;
  readonly quantity: number;
}

/** Totals shown on the checkout overview page, in cents. */
export interface OrderSummary {
  readonly itemTotalCents: number;
  readonly taxCents: number;
  readonly totalCents: number;
}

/** Values of the inventory sort dropdown. */
export type SortOption = 'az' | 'za' | 'lohi' | 'hilo';
