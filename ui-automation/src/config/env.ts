import path from 'node:path';
import dotenv from 'dotenv';

/**
 * Single source of truth for UI-suite configuration.
 *
 * Resolution order: real environment variables (e.g. CI secrets) win over values in the .env file,
 * because dotenv never overrides variables that are already set.
 * Point ENV_FILE at another file (e.g. `.env.staging`) to switch environments without code changes.
 */
const PROJECT_ROOT = path.resolve(__dirname, '..', '..');

dotenv.config({ path: process.env.ENV_FILE ?? path.join(PROJECT_ROOT, '.env'), quiet: true });

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(
      `Missing required environment variable "${name}". ` +
        'Create ui-automation/.env from .env.example or export the variable in your shell/CI.',
    );
  }
  return value;
}

function optionalPositiveInt(name: string, fallback: number): number {
  const raw = process.env[name]?.trim();
  if (!raw) {
    return fallback;
  }
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`Environment variable "${name}" must be a positive integer, got "${raw}".`);
  }
  return value;
}

export const env = {
  baseUrl: required('BASE_URL'),
  users: {
    standard: required('STANDARD_USER'),
    lockedOut: required('LOCKED_OUT_USER'),
  },
  password: required('USER_PASSWORD'),
  timeouts: {
    test: optionalPositiveInt('TEST_TIMEOUT_MS', 30_000),
    expect: optionalPositiveInt('EXPECT_TIMEOUT_MS', 5_000),
    action: optionalPositiveInt('ACTION_TIMEOUT_MS', 10_000),
    navigation: optionalPositiveInt('NAVIGATION_TIMEOUT_MS', 15_000),
  },
} as const;

/** Where the setup project saves the authenticated browser state (git-ignored). */
export const STORAGE_STATE_PATH = path.join(PROJECT_ROOT, '.auth', 'standard-user.json');
