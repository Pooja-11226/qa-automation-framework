import { faker } from '@faker-js/faker';
import type { CustomerDetails } from '../types/models';
import { checkoutErrors } from './messages';

// A fixed seed makes generated data reproducible when re-running a failure locally.
const seed = process.env.FAKER_SEED;
if (seed) {
  faker.seed(Number(seed));
}

/** Builds a valid, unique customer. Override individual fields to create invalid variants. */
export function buildCustomer(overrides: Partial<CustomerDetails> = {}): CustomerDetails {
  return {
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName(),
    postalCode: faker.location.zipCode('#####'),
    ...overrides,
  };
}

export interface MissingFieldCase {
  readonly field: keyof CustomerDetails;
  readonly label: string;
  readonly expectedError: string;
}

/** Each mandatory checkout field and the validation error expected when it is left empty. */
export const missingCustomerFieldCases: readonly MissingFieldCase[] = [
  { field: 'firstName', label: 'first name', expectedError: checkoutErrors.firstNameRequired },
  { field: 'lastName', label: 'last name', expectedError: checkoutErrors.lastNameRequired },
  { field: 'postalCode', label: 'postal code', expectedError: checkoutErrors.postalCodeRequired },
];
