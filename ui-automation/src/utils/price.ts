/**
 * Money helpers. Prices are converted to integer cents so that totals can be compared exactly,
 * avoiding floating-point rounding surprises such as 0.1 + 0.2 !== 0.3.
 */
const PRICE_PATTERN = /\$\s*(\d+(?:\.\d{1,2})?)/;

/** Extracts the first dollar amount from text such as "$29.99" or "Item total: $29.99". */
export function parsePriceToCents(text: string): number {
  const match = PRICE_PATTERN.exec(text);
  if (!match?.[1]) {
    throw new Error(`Could not find a dollar amount in "${text}"`);
  }
  return Math.round(Number(match[1]) * 100);
}

export function sumCents(values: readonly number[]): number {
  return values.reduce((total, value) => total + value, 0);
}
