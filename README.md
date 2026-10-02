# QA Automation Framework: UI and API Test Automation

A CI-ready automation framework covering UI and API test automation.

| Part | System under test | Stack | Report |
|---|---|---|---|
| UI | [SauceDemo](https://www.saucedemo.com), the full purchase flow | Playwright, TypeScript, Node 22.13+ | Playwright HTML |
| API | [Restful-Booker](https://restful-booker.herokuapp.com), booking CRUD and auth | Java 17, REST Assured, TestNG, Maven | Allure and Surefire |

**At a glance**
- **UI:** 29 test cases, with a separate login setup project for session reuse.
- **API:** 55 tests in the blocking suite plus 19 known-defect tests. TestNG counts each data-provider row as a test.
- **Defects:** 11 application defects found and documented in [docs/known-issues.md](docs/known-issues.md). The tests expose them; they do not work around them.
- **Coverage:** every PDF requirement is mapped to a test in [docs/coverage-matrix.md](docs/coverage-matrix.md).

---

## Contents

1. [Repository layout](#repository-layout)
2. [Prerequisites](#prerequisites)
3. [Installation and configuration](#installation-and-configuration)
4. [Running the tests](#running-the-tests)
5. [Reports, screenshots and traces](#reports-screenshots-and-traces)
6. [Architecture](#architecture)
7. [Test coverage](#test-coverage)
8. [CI/CD](#cicd)
9. [Design decisions](#design-decisions)
10. [Assumptions](#assumptions)
11. [Known issues](#known-issues)
12. [Limitations and next steps](#limitations-and-next-steps)

---

## Repository layout

```
qa-automation-framework/
├── README.md
├── .gitignore
├── .github/workflows/ci.yml        # UI job, API job, non-blocking known-defect job
├── docs/
│   ├── test-strategy.md            # scope, risks, techniques, defect handling
│   ├── coverage-matrix.md          # requirement → test ID → automation → expected result
│   ├── known-issues.md             # application defects with evidence
│   └── sample-outputs/             # console logs and report screenshots from a real run
├── ui-automation/
│   ├── playwright.config.ts
│   ├── .env.example
│   ├── src/
│   │   ├── config/env.ts           # validated configuration
│   │   ├── pages/                  # page objects
│   │   ├── components/             # header, cart item list (shared UI parts)
│   │   ├── fixtures/test.ts        # injects page objects; logs each test
│   │   ├── test-data/              # users, products, messages, customers
│   │   ├── types/models.ts         # shared domain types
│   │   └── utils/                  # logger, price maths
│   └── tests/
│       ├── auth.setup.ts           # logs in once, saves the session
│       ├── auth/login.spec.ts
│       ├── cart/cart.spec.ts
│       ├── cart/product-selection.spec.ts
│       ├── checkout/checkout.spec.ts
│       └── e2e/purchase-flow.spec.ts
└── api-automation/
    ├── pom.xml
    ├── .env.example
    └── src/
        ├── main/java/com/sdet/api/
        │   ├── config/             # ConfigKey, ConfigManager
        │   ├── spec/               # request and response specifications
        │   ├── client/             # API clients, Auth, TokenManager, HTTP constants
        │   ├── model/              # request/response POJOs (records)
        │   ├── data/               # BookingDataFactory, BookingField
        │   └── listener/           # SLF4J HTTP logging filter, TestNG logging listener
        └── test/
            ├── java/com/sdet/api/tests/   # test classes, plus support/ (groups, schemas, assertions)
            └── resources/                 # config/qa.properties, schemas/, logback-test.xml
```

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Node.js | 22.13+ (LTS, used in CI). 20.19+ and 24+ also work; these minimums come from ESLint 10 and Faker 10. | `node -v` |
| npm | 10+ | `npm -v` |
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Git | any | `git --version` |

Internet access to `saucedemo.com`, `restful-booker.herokuapp.com`, the npm registry, Maven Central and the Playwright browser CDN is also required.

## Installation and configuration

Start from the repository root, whether you cloned it from GitHub or unzipped the submission archive:

```bash
cd qa-automation-framework

# UI
cd ui-automation
npm ci
npx playwright install chromium          # add --with-deps on a fresh Linux machine
cp .env.example .env

# API
cd ../api-automation
cp .env.example .env

mvn -q -DskipTests dependency:resolve    # optional: pre-download dependencies
```

On Windows PowerShell, use `Copy-Item .env.example .env` instead of `cp`.

### Configuration

Nothing environment-specific is hard-coded. The UI `.env` files are git-ignored. The committed `ui-automation/.env.example` file contain only the SauceDemo demo credentials published by SauceDemo and Restful-Booker. API configuration uses the environment/property precedence described below.

**UI** (`ui-automation/src/config/env.ts`). Real environment variables override `.env`. Missing or invalid values fail fast with a clear message.

| Variable | Purpose | Default |
|---|---|---|
| `BASE_URL` | Application URL | required |
| `STANDARD_USER`, `LOCKED_OUT_USER`, `USER_PASSWORD` | Test accounts | required |
| `ENV_FILE` | Load another file, e.g. `.env.staging` | `.env` |
| `BROWSERS` | `chromium`, `firefox` and/or `webkit` (comma-separated) | `chromium` |
| `TEST_TIMEOUT_MS`, `EXPECT_TIMEOUT_MS`, `ACTION_TIMEOUT_MS`, `NAVIGATION_TIMEOUT_MS` | Timeouts | 30000 / 5000 / 10000 / 15000 |
| `LOG_LEVEL` | `debug`, `info`, `warn` or `error` | `info` |
| `FAKER_SEED` | Reproducible customer data | random |

**API** (`ConfigManager`). Precedence, highest first:
1. JVM system property (`-Dapi.base.uri=…`).
2. Environment variable (`API_BASE_URI`).
3. `.env`.
4. `src/test/resources/config/<env>.properties`.

Select the environment with `-Denv=<name>` or `API_ENV` (default `qa`). To add a staging environment, create `config/staging.properties` and run `mvn test -Denv=staging`.

| Property / variable | Purpose |
|---|---|
| `api.base.uri` / `API_BASE_URI` | API base URL |
| `api.username`, `api.password` / `API_USERNAME`, `API_PASSWORD` | Admin credentials (never in properties files) |
| `api.connect.timeout.ms`, `api.read.timeout.ms` | HTTP timeouts |
| `api.healthcheck.enabled` | Fail-fast `/ping` before the suite |
| `-Dthread.count` | Parallel threads (default 4) |
| `-Ddata.seed` | Reproducible generated data |

## Running the tests

### Run everything

```bash
(cd ui-automation && npx playwright test) && (cd api-automation && mvn clean test)
```

### UI (run from `ui-automation/`)

| Goal | Command |
|---|---|
| All tests, headless | `npm test` |
| Headed (visible browser) | `npm run test:headed` |
| Interactive UI mode (watch, time-travel) | `npm run test:ui` |
| Step-through debugger | `npm run test:debug` |
| Smoke / regression / negative / E2E / known defects | `npm run test:smoke`, `test:regression`, `test:negative`, `test:e2e`, `test:known-defects` |
| Exclude known defects | `npx playwright test --grep-invert @known-defect` |
| One file | `npx playwright test tests/checkout/checkout.spec.ts` |
| One test by name | `npx playwright test -g "login is rejected for a locked-out account"` |
| Cross-browser | `BROWSERS=chromium,firefox,webkit npx playwright test` (install those browsers first) |
| Control parallelism | `npx playwright test --workers=4` (fully parallel by default) |
| Lint, type-check, format | `npm run lint`, `npm run typecheck`, `npm run format:check` |

### API (run from `api-automation/`)

| Goal | Command |
|---|---|
| All blocking tests | `mvn clean test` |
| By group | `mvn test -Dgroups=smoke` (also `regression` or `negative`) |
| Known-defect tests only (expected to fail) | `mvn test -Pknown-defects` |
| One class | `mvn test -Dtest=BookingCrudTest` |
| One method | `mvn test -Dtest=BookingCrudTest#deleteBookingRemovesIt` |
| Sequential run (debugging) | `mvn test -Dthread.count=1` |
| Verbose HTTP logs on the console | `mvn test -DCONSOLE_LOG_LEVEL=DEBUG` |
| Another environment | `mvn test -Denv=staging` |

## Reports, screenshots and traces

### UI

| Artifact | Location | How to open |
|---|---|---|
| HTML report | `ui-automation/playwright-report/` | `npm run report` |
| Screenshot, video and trace of failed tests | `ui-automation/test-results/<test>/` | Linked from the HTML report |
| JUnit XML (for CI dashboards) | `ui-automation/test-results/junit.xml` | Any JUnit viewer |

Screenshots are captured on failure; video and trace are kept on failure locally. In CI, the trace is recorded on the first retry. Open a trace with `npx playwright show-trace test-results/<test>/trace.zip` to see every action, DOM snapshot, network call and console message. The E2E test is split into named `test.step()` blocks that mirror the assignment's steps, so the report shows exactly which business step failed.

### API

| Artifact | Location | How to open |
|---|---|---|
| Allure results | `api-automation/target/allure-results/` | `mvn allure:serve` (opens a browser) |
| Static Allure report | `api-automation/target/site/allure-maven-plugin/` | `mvn allure:report`, then open `index.html` |
| Surefire/TestNG reports | `api-automation/target/surefire-reports/` | `emailable-report.html` |
| Full HTTP log | `api-automation/target/logs/api-tests.log` | Text editor |

Every API test in Allure carries the full request and response as attachments. The log file contains a DEBUG record of every exchange, and each line is tagged with the test that issued it, so parallel runs stay readable.

Credentials never reach reports or logs:
- `Authorization` and `Cookie` headers (Basic auth and the token cookie) are masked in Allure attachments and in all logs.
- `POST /auth` exchanges are deliberately not attached to Allure, because their bodies carry the password and the issued token. They are still logged, with `password` and `token` values redacted.

### Diagnosing a failure

1. **UI:** open the HTML report and go to the failed step. Its custom assertion message says what was expected, for example "item total should equal the sum of line prices". Then check the screenshot, and open the trace for the DOM and network at the moment of failure. Re-run locally with `--headed` or `--debug`.
2. **API:** the AssertJ message includes the response body. The Allure attachment shows the exact request. `target/logs/api-tests.log` shows the timeline for that test. Re-run one method with `-Dtest=Class#method -Dthread.count=1`.
3. **Triage:** is it an application defect, a test defect or an environment issue? Environment outages surface as a health-check failure (API) or navigation timeouts (UI). A real defect gets added to `docs/known-issues.md`.

## Architecture

### UI framework

```
spec file ──uses──▶ fixtures/test.ts ──creates──▶ Page objects ──compose──▶ Components
    │                     │                          │
    └── test-data ◀───────┘                          └── utils/price (parse "$29.99" → 2999 cents)
          (users from config/env.ts, products, messages, Faker customers)
```

- **Page Object Model.** `BasePage` provides navigation and an "am I on this page?" check. `SecuredPage` adds the shared header and page title. Each concrete page exposes locators as readonly fields and actions as small methods.
- **Components.** The cart line list appears on both the cart and overview pages, so it is modelled once as `CartItemList`. The header (cart link and badge, logout) is `HeaderComponent`.
- **Fixtures.** Tests receive page objects as parameters (`async ({ cartPage }) => …`) instead of constructing them. An auto fixture logs the start and end of every test.
- **Assertions live in tests.** Page objects only assert their own identity (`expectToBeOpen`). Business expectations stay in the specs where a reader expects them.
- **Session reuse.** A `setup` project logs in once and saves the browser storage state. Cart and checkout specs start authenticated. Login and E2E specs deliberately log in through the UI.

### API framework

```
Test class ──extends──▶ BaseApiTest (health check, auth helpers, cleanup)
    │
    ├── BookingDataFactory ──▶ model records (Booking, BookingDates, …)
    │
    └── BookingClient / AuthClient / HealthClient
            └── RequestSpecFactory (base URI, media types, timeouts,
                                    AllureRestAssured + Slf4jLoggingFilter)
    then: ResponseSpecFactory (status, content type, JSON schema)
          + AssertJ (recursive POJO comparison, field checks)
```

- **Clients never assert.** They return the raw `Response`, so the same `update(id, payload, auth)` serves the happy path, the 403 matrix and the validation tests.
- **Specifications.** `RequestSpecFactory` holds everything every request needs. `ResponseSpecFactory` holds reusable expectations, including JSON Schema contract checks.
- **Auth as a value.** `Auth.token(…)`, `Auth.basic(…)` and `Auth.none()` are passed into client calls. This makes negative authentication tests a data-provider row, not a new method. `toString()` is masked because these values appear in reports.
- **Secrets stay out of reports.** The token travels as a `Cookie` header, which is masked like `Authorization`. Requests that carry credentials in the body use a separate spec without Allure attachments.
- **State isolation.** `createBooking()` registers the id. `@AfterMethod(alwaysRun = true)` deletes it even when the test fails. A `ThreadLocal` keeps parallel tests apart.

## Test coverage

Full mapping: [docs/coverage-matrix.md](docs/coverage-matrix.md). Strategy and rationale: [docs/test-strategy.md](docs/test-strategy.md).

| Area | Positive | Negative | Edge / boundary |
|---|---|---|---|
| UI login and session | valid login, logout | 6 rejected logins, anonymous access | case-sensitive username, error dismissal |
| UI catalogue and cart | add, view, 4 sort orders | — | last item removed (badge hidden), reload persistence |
| UI checkout | full purchase, totals for 3 baskets | 3 missing fields, empty form | cancel keeps cart, whitespace input (KD), empty cart (KD) |
| API auth | token issued | 5 invalid-credential cases, 9 forbidden-write cases | case-mismatched username, null fields |
| API CRUD | POST, GET, PUT (2 auth methods), PATCH, DELETE, list, filter | unknown/invalid ids, 7 missing fields (POST and PUT), empty/malformed body, bad media types | price 0, same-day stay, accented names, name trimming, no optional field |

## CI/CD

`.github/workflows/ci.yml` runs on every push to `main`, on pull requests, and on demand. It has three jobs.

| Job | Steps | Blocking |
|---|---|---|
| `ui-tests` | checkout, set up Node 22 (npm cache), `npm ci`, lint, type-check, install Chromium with OS dependencies, run tests, upload HTML report and failure evidence | Yes |
| `api-tests` | checkout, set up Java 17 (Maven cache), `mvn clean test`, generate Allure report, upload Allure, Surefire reports and logs | Yes |
| `api-known-defects` | runs `mvn test -Pknown-defects` and uploads its reports | No (`continue-on-error`) |

Artifacts are uploaded even when tests fail (`if: !cancelled()`), so failure evidence is always available. In CI, Playwright forbids a stray `test.only`, uses 2 workers and retries a failed test once (reported as flaky).

## Design decisions

| Decision | Reason |
|---|---|
| **SauceDemo** for UI | Stable, fast, has `data-test` attributes, and covers the full mandatory flow. It has no search box, so "search/select a product" is implemented as selecting by name (plus sorting tests). |
| **Restful-Booker** for API | Real persistence for PUT/PATCH/DELETE, real token and Basic auth, and an open-source implementation, which allowed defects to be confirmed in code. |
| **TestNG** | Groups, data providers and method-level parallelism are built in; it is common in Java QA stacks. |
| **Allure** for API reports | Shows each request and response next to the test. Playwright's own HTML report already covers the UI side, so no extra reporter is added there. |
| **`data-test` locators** | Purpose-built for testing; stable across styling changes. ARIA roles are used for buttons, which also reflects accessibility. |
| **No hard waits** | Playwright auto-waiting and web-first assertions remove the main cause of flaky UI tests. |
| **Prices compared in cents** | Avoids floating-point errors (for example `0.1 + 0.2 ≠ 0.3`) when checking totals. |
| **Expected prices read from the UI** | The test checks consistency between catalogue, cart and overview without duplicating the price list. |
| **Session saved once** (`storageState`) | Tests not about login are faster and are not broken by an unrelated login failure. |
| **Framework code in `src/main/java`** | Separates the reusable client/spec/model layer from tests. For this reason the test libraries are compile scope. |
| **Literal `application/json` Accept** | REST Assured's `ContentType.JSON` sends four comma-separated types; Restful-Booker matches Accept literally and would return 418. |
| **No custom retry of API assertions** | The PDF recommends "retry handling where appropriate". Retrying assertions can hide real defects, so it is not used. Availability is handled by a fail-fast `/ping` health check. The UI uses Playwright's built-in CI retry, and those retries are visible as flaky. |
| **Defects asserted, not worked around** | Tests describe correct behaviour. Defects are kept visible through `test.fail()` (UI) and a separate group (API), so the main build stays a trustworthy signal. |
| **Single source of truth for Maven execution** | No `testng.xml`: groups, parallelism and exclusions live in the Surefire configuration, so `-Dgroups` and `-Dtest` work without editing files. |

## Assumptions

- The public demo sites are the systems under test, so their documented quirks are asserted as documented. Examples are DELETE returning 201 and `/ping` returning 201; see [docs/known-issues.md](docs/known-issues.md) › Observations.
- Restful-Booker is shared and may be reset by its host. Tests therefore create and clean up their own data and never rely on existing ids.
- SauceDemo's intentionally faulty accounts (`problem_user`, `error_user`, and so on) are out of scope. See test-strategy.md.
- The SauceDemo demo credentials in ui-automation/.env.example are public. Real credentials would be supplied through CI secrets, which override .env without code changes.

## Known issues

Eleven application defects were found: 2 in SauceDemo and 9 in Restful-Booker. Examples include checkout with an empty cart, field types not validated, decimal prices truncated, and a rejected request still storing data. All are listed with steps, expected and actual behaviour, severity and the exposing test in [docs/known-issues.md](docs/known-issues.md).

## Limitations and next steps

- **Live data.** The public environments are outside our control, so occasional outages cause failures that are not product defects. The API health check makes these obvious.
- **Possible extensions:**
  - API contract checks for XML responses.
  - Visual comparison and accessibility scans (axe) in the UI suite.
  - Test data seeding against a private environment.
  - Publishing reports to GitHub Pages.
  - Running the UI matrix across browsers in parallel CI jobs.
