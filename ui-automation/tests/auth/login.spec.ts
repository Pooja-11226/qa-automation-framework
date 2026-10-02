import { test, expect } from '../../src/fixtures/test';
import { loginErrors } from '../../src/test-data/messages';
import { invalidCredentialValues, users } from '../../src/test-data/users';
import type { UserCredentials } from '../../src/types/models';

interface RejectedLoginCase {
  readonly scenario: string;
  readonly credentials: UserCredentials;
  readonly expectedError: string;
}

const rejectedLoginCases: readonly RejectedLoginCase[] = [
  {
    scenario: 'a wrong password',
    credentials: { username: users.standard.username, password: invalidCredentialValues.wrongPassword },
    expectedError: loginErrors.invalidCredentials,
  },
  {
    scenario: 'an unknown username',
    credentials: { username: invalidCredentialValues.unknownUsername, password: users.standard.password },
    expectedError: loginErrors.invalidCredentials,
  },
  {
    scenario: 'a username in the wrong case',
    credentials: { username: users.standard.username.toUpperCase(), password: users.standard.password },
    expectedError: loginErrors.invalidCredentials,
  },
  {
    scenario: 'a locked-out account',
    credentials: users.lockedOut,
    expectedError: loginErrors.lockedOut,
  },
  {
    scenario: 'an empty username',
    credentials: { username: '', password: users.standard.password },
    expectedError: loginErrors.usernameRequired,
  },
  {
    scenario: 'an empty password',
    credentials: { username: users.standard.username, password: '' },
    expectedError: loginErrors.passwordRequired,
  },
];

test.describe('Login', { tag: '@regression' }, () => {
  test.beforeEach(async ({ loginPage }) => {
    await loginPage.goto();
  });

  test(
    'a valid user can log in and lands on the inventory page',
    { tag: '@smoke' },
    async ({ loginPage, inventoryPage }) => {
      await loginPage.login(users.standard);

      await inventoryPage.expectToBeOpen();
      await expect(inventoryPage.productCards, 'the catalogue should list products').not.toHaveCount(0);
      await expect(inventoryPage.header.cartLink).toBeVisible();
    },
  );

  for (const { scenario, credentials, expectedError } of rejectedLoginCases) {
    test(`login is rejected for ${scenario}`, { tag: '@negative' }, async ({ loginPage, inventoryPage }) => {
      await loginPage.login(credentials);

      await expect(loginPage.errorMessage).toHaveText(expectedError);
      await loginPage.expectToBeOpen();

      // No session must have been created: the protected page must still be unreachable.
      await inventoryPage.goto();
      await loginPage.expectToBeOpen();
    });
  }

  test('the login error message can be dismissed', async ({ loginPage }) => {
    await loginPage.login({ username: '', password: '' });
    await expect(loginPage.errorMessage).toBeVisible();

    await loginPage.dismissError();

    await expect(loginPage.errorMessage).toBeHidden();
  });

  test(
    'an anonymous user is redirected from a protected page to login',
    { tag: '@negative' },
    async ({ loginPage, inventoryPage }) => {
      await inventoryPage.goto();

      await loginPage.expectToBeOpen();
      await expect(loginPage.errorMessage).toHaveText(loginErrors.protectedPage('/inventory.html'));
    },
  );

  test('logging out ends the session', async ({ loginPage, inventoryPage }) => {
    await loginPage.login(users.standard);
    await inventoryPage.expectToBeOpen();

    await inventoryPage.header.logout();
    await loginPage.expectToBeOpen();

    await inventoryPage.goto();
    await loginPage.expectToBeOpen();
    await expect(loginPage.errorMessage).toHaveText(loginErrors.protectedPage('/inventory.html'));
  });
});
