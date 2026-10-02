# Test Strategy

## 1. Purpose

This document explains what is tested, why, and how, for the two systems under test:

| Part | System under test | Tooling |
|---|---|---|
| UI | SauceDemo (`https://www.saucedemo.com`), a public e-commerce demo | Playwright + TypeScript |
| API | Restful-Booker (`https://restful-booker.herokuapp.com`), a public hotel-booking REST API | Java 17, REST Assured, TestNG, Maven |

Restful-Booker was chosen over the APIs listed as examples in the brief because it supports real create, read, update and delete operations with persistence and has genuine authentication. ReqRes now requires an API key with rate limits, and JSONPlaceholder/FakeStore fake their writes, which would make PUT/DELETE and authentication tests meaningless. The brief allows "any other suitable public API".

## 2. Scope

**In scope (UI)**
- The mandatory purchase flow: launch, login, verify login, select product, add to cart, verify cart, checkout, enter customer details, complete, verify confirmation.
- Authentication: valid login, rejected logins, locked-out user, access control on protected pages, logout.
- Product selection: locating products by name; sorting by name and price.
- Cart behaviour: add, remove, badge count, persistence across reload, navigation back to the catalogue.
- Checkout: mandatory-field validation, cancel, price/tax/total arithmetic for several baskets.

**In scope (API)**
- Booking create, read (by id and filtered list), full update (PUT), partial update (PATCH), delete.
- Token and Basic authentication; authorisation of write operations.
- Input validation: missing fields, empty and malformed bodies, unsupported content/accept types, unknown ids.
- Response contracts via JSON Schema.

**Out of scope**
- SauceDemo special accounts designed to misbehave (`problem_user`, `error_user`, `performance_glitch_user`, `visual_user`). They test intentional chaos rather than the business flow; they could be added as a separate suite.
- Visual regression, accessibility audits, and performance/load testing.
- XML and form-encoded variants of the Restful-Booker API. JSON is the primary contract.
- Security testing beyond authentication and authorisation checks, such as injection or fuzzing.

## 3. Risk-based prioritisation

Tests were prioritised by business impact multiplied by likelihood of failure.

| Risk | Impact | Coverage |
|---|---|---|
| Customer cannot complete a purchase | Critical (lost revenue) | E2E purchase test tagged `@smoke` |
| Wrong amount charged (item total, tax, total) | Critical (financial, legal) | Data-driven totals across three baskets, cent-exact arithmetic |
| Unauthorised access to protected pages or data | High (security) | Anonymous redirect, logout session check, rejected-login session check, API 403 matrix |
| Cart loses or misreports items | High | Add/remove/badge/reload tests |
| Booking data corrupted on write | High | PUT/PATCH verify the stored state with a follow-up GET, not just the response |
| Bad input accepted and stored | Medium-High | Validation tests and known-defect tests |
| API contract drift breaking consumers | Medium | JSON Schema validation with `additionalProperties: false` |

## 4. Test design approach

**Positive testing** confirms that each business capability works with valid data. For example, the PUT test verifies both the response and the persisted record, and it runs with both supported authentication methods.

**Negative testing** confirms the system refuses invalid actions safely:
- **UI:** six rejected-login cases, three missing checkout fields, an empty form, and anonymous access.
- **API:** invalid credentials (5 cases) and forbidden writes (3 operations × 3 credential types). It also covers unknown ids (4 cases), missing mandatory fields on create and update (7 fields each), empty and malformed bodies, and unsupported media types.
- After a rejected write, the API tests also assert that the stored booking is unchanged. A correct status code alone does not prove nothing changed.

**Boundary and edge cases**
- **UI:** case-sensitive usernames, empty forms, removing the last cart item (badge disappears), reload persistence, the highest-priced basket, and whitespace-only input (known defect).
- **API:**
  - A zero price and same-day check-in/out.
  - Accented and apostrophe names, and an omitted optional field.
  - Name trimming, check-out before check-in, decimal prices, and non-numeric, zero and negative ids.

**Techniques used:** equivalence partitioning (valid and invalid credential classes), boundary values (price 0, same-day stay), decision tables (operation × credential matrix), and state verification (re-reading data after writes).

## 5. Handling application defects

The suites assert the correct, expected behaviour. They are never adjusted to match a bug. When a check fails because of a confirmed application defect, the defect is recorded in [known-issues.md](known-issues.md) and handled as follows.

- **UI:** the test is marked with Playwright's `test.fail()` and tagged `@known-defect`. It runs in every build and is reported as an expected failure. If the defect is fixed, the test "unexpectedly passes", which turns the build red so the marker can be removed.
- **API:** the precise-contract test lives in `BookingKnownDefectsTest` (TestNG group `known-defect`). It is excluded from the blocking run and executed by a separate non-blocking CI job with `mvn test -Pknown-defects`. Where the API does reject bad input but with the wrong status code, the main suite still asserts the reliable part: the request is refused and nothing is created. The known-defect test asserts the exact status code.

Separating defects this way keeps the main pipeline a trustworthy signal. A red build means something changed, not "the demo app is still buggy".

## 6. Automation strategy

- **Independence:** every test creates its own state. UI tests use a fresh browser context, so each starts with an empty cart. API tests create their own bookings and delete them in `@AfterMethod(alwaysRun = true)`. There is no ordering dependency and no reliance on pre-seeded data.
- **Parallelism:** Playwright runs fully parallel. TestNG runs methods in parallel (4 threads by default), which is safe because state is per test and shared helpers use `ThreadLocal`. Unique, generated booking names prevent collisions in the shared public environment.
- **Synchronisation:** only Playwright's auto-waiting and web-first assertions (`toHaveURL`, `toHaveText`, `toHaveCount`) are used. There are no fixed sleeps.
- **Locators:** the order of preference is `data-test` attributes, then ARIA role with accessible name, then exact visible text. No XPath and no CSS tied to styling.
- **Retries:**
  - API assertions are never retried, so a retry can never hide a functional failure.
  - Environment availability is handled up front by a fail-fast `/ping` health check in `@BeforeSuite`.
  - The UI suite uses Playwright's built-in `retries: 1` in CI only, to absorb network blips on a public site. Retried tests are flagged "flaky" in the report rather than hidden.
- **Execution levels:**
  - **Smoke:** `@smoke` and `smoke`, critical path only.
  - **Regression:** `@regression` and `regression`.
  - **Negative-only:** `@negative` and `negative`.
  - **Known defects:** `@known-defect` and `known-defect`.

## 7. Environments and test data

- Configuration is externalised: `.env` files (git-ignored) plus environment variables for both projects. The API also has `-D` system properties and per-environment `config/<env>.properties`. Switching environments needs no code change.
- Credentials never appear in source. The `.env.example` files contain only the publicly published demo credentials. A real system would inject secrets from CI. Authentication headers, the token and passwords are masked in Allure reports and logs.
- **UI data:** typed constants for products and messages, and Faker-generated customers. `FAKER_SEED` makes runs reproducible. Expected prices are read from the UI rather than hard-coded.
- **API data:** `BookingDataFactory` generates valid bookings with unique names (`-Ddata.seed` for reproducibility) and builds invalid variants by removing or replacing individual fields.

## 8. Entry and exit criteria

- **Entry:** the target environment is reachable. The API health check enforces this, and Playwright navigation fails fast.
- **Exit:**
  - All blocking tests pass.
  - Known-defect tests behave as recorded.
  - Every new failure is triaged as an application defect, a test defect or an environment issue before release.
