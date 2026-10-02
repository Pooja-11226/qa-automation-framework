import { env } from '../config/env';
import type { UserCredentials } from '../types/models';

/** Valid accounts come from configuration so no credential is hard-coded in tests. */
export const users = {
  standard: { username: env.users.standard, password: env.password },
  lockedOut: { username: env.users.lockedOut, password: env.password },
} as const satisfies Record<string, UserCredentials>;

/** Deliberately invalid values used by negative login tests (not secrets). */
export const invalidCredentialValues = {
  wrongPassword: 'not-the-right-password',
  unknownUsername: 'unknown_user',
} as const;
