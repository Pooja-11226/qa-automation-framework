/**
 * Minimal levelled logger. Output goes to stdout/stderr, which Playwright captures per test
 * and shows in the HTML report, so log lines sit next to the failing step.
 * Never log credentials or other secrets.
 */
type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LEVEL_ORDER: Record<LogLevel, number> = { debug: 10, info: 20, warn: 30, error: 40 };

function resolveThreshold(): LogLevel {
  const configured = process.env.LOG_LEVEL?.toLowerCase();
  return configured && configured in LEVEL_ORDER ? (configured as LogLevel) : 'info';
}

const threshold = resolveThreshold();

function write(level: LogLevel, message: string, context?: Record<string, unknown>): void {
  if (LEVEL_ORDER[level] < LEVEL_ORDER[threshold]) {
    return;
  }
  const suffix = context ? ` ${JSON.stringify(context)}` : '';
  const line = `${new Date().toISOString()} [${level.toUpperCase()}] ${message}${suffix}`;
  if (level === 'error' || level === 'warn') {
    console.error(line);
  } else {
    console.log(line);
  }
}

export const logger = {
  debug: (message: string, context?: Record<string, unknown>): void => write('debug', message, context),
  info: (message: string, context?: Record<string, unknown>): void => write('info', message, context),
  warn: (message: string, context?: Record<string, unknown>): void => write('warn', message, context),
  error: (message: string, context?: Record<string, unknown>): void => write('error', message, context),
};
