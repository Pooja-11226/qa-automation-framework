# Coverage Matrix

**Source** shows where each requirement comes from:
- **PDF:** stated in the assignment.
- **PDF-Rec:** listed under the PDF's "Recommended Additional Coverage".
- **Added:** extra risk-based coverage chosen by the author.

**Test ID** is the real identifier in the code, so every entry can be found with a text search:
- UI tests use the Playwright title. Run one with `npx playwright test -g "<title>"`.
- API tests use `Class#method`. Run one with `mvn test -Dtest=Class#method`.
- `KD-…` identifiers are defect IDs that appear in the test titles and descriptions; see [known-issues.md](known-issues.md).
- "×n" means a data-driven test with n cases.

## Part 1: UI (SauceDemo, Playwright)

### Mandatory end-to-end scenario

One test, `e2e/purchase-flow.spec.ts` › *a standard user can buy a product from login to order confirmation*, covers every step. Each PDF step is a named `test.step` in that test.

| Requirement (PDF wording) | Source | Test ID (`test.step` name) | Expected result |
|---|---|---|---|
| Launch the application | PDF | Launch the application | Login page is displayed |
| Login, verify successful login | PDF | Log in and verify the login succeeded | Inventory page with title "Products"; cart empty |
| Search/select a product | PDF | Select a product and add it to the cart | The named product card is visible and selected by name |
| Add the product to the cart | PDF | Select a product and add it to the cart | Badge shows 1; button changes to "Remove" |
| Verify the cart details | PDF | Verify the cart details | Exactly one line; name, price and quantity equal the product as listed |
| Proceed to checkout, enter customer/shipping details | PDF | Proceed to checkout and enter customer details | Information page accepts generated customer data |
| Complete the checkout | PDF | Verify the order overview; Complete the checkout | Overview lines, payment and shipping info correct; total = item total + tax |
| Verify the order/success confirmation | PDF | Verify the order confirmation | "Thank you for your order!"; cart badge cleared; Back Home returns to inventory |

### Supporting scenarios

| Requirement | Source | Test ID (Playwright title) | Automation | Expected result |
|---|---|---|---|---|
| Positive login | PDF | a valid user can log in and lands on the inventory page | `auth/login.spec.ts` | Inventory page with products |
| Negative login | PDF | login is rejected for a wrong password / an unknown username / a username in the wrong case / a locked-out account / an empty username / an empty password | `auth/login.spec.ts`, data-driven ×6 | Exact error shown; still on login; inventory remains unreachable |
| Error message dismissal | Added | the login error message can be dismissed | `auth/login.spec.ts` | Error disappears |
| Protected page access control | Added | an anonymous user is redirected from a protected page to login | `auth/login.spec.ts` | Redirect to login with the access-denied message |
| Logout | Added | logging out ends the session | `auth/login.spec.ts` | Login page; inventory unreachable afterwards |
| Cart shows product details | PDF | adding a product updates the badge and the cart shows its details | `cart/cart.spec.ts` | Badge 1; cart line equals listed product |
| Removing products | Added | removing products keeps the badge and cart contents accurate | `cart/cart.spec.ts` | Badge decrements, then disappears when empty |
| Cart persistence | Added | cart contents survive a page reload | `cart/cart.spec.ts` | Same lines after reload |
| Continue shopping | Added | continue shopping returns to the inventory with the cart intact | `cart/cart.spec.ts` | Inventory shown; badge and button state kept |
| Product selection (sorting) | Added | products can be sorted by price, low to high / price, high to low / name, A to Z / name, Z to A | `cart/product-selection.spec.ts`, data-driven ×4 | Displayed order equals the sorted order |
| Mandatory checkout fields | PDF | checkout is blocked when the first name / last name / postal code is missing | `checkout/checkout.spec.ts`, data-driven ×3 | Exact field error; stays on the information page |
| Empty checkout form | Added | submitting an empty form reports the first missing field | `checkout/checkout.spec.ts` | First-name error shown |
| Cancel checkout | Added | cancelling returns to the cart without losing items | `checkout/checkout.spec.ts` | Cart shown with item present |
| Order totals | Added | the overview shows correct totals for a single low-priced item / two items / three items including the most expensive product | `checkout/checkout.spec.ts`, data-driven ×3 | Lines match; item total = sum of prices; tax > 0; total = item total + tax (cent-exact) |
| Empty cart cannot be checked out | Added | KD-UI-01: checkout should not be possible with an empty cart | `checkout/checkout.spec.ts`, `test.fail` | Expected: stays on cart. Actual: checkout opens (defect) |
| Whitespace-only details rejected | Added | KD-UI-02: whitespace-only customer details should be rejected | `checkout/checkout.spec.ts`, `test.fail` | Expected: validation error. Actual: accepted (defect) |

### UI framework expectations

| PDF expectation | Where it is implemented |
|---|---|
| Proper locator strategy | `getByTestId` (configured to use `data-test`), ARIA roles, exact text; see `src/pages/*` |
| Reusable page/component methods | `src/pages/*`; `src/components/HeaderComponent.ts`; `CartItemList.ts` (shared by the cart and overview pages) |
| Meaningful assertions | Web-first assertions with messages; captured-vs-displayed data comparison; cent-exact totals |
| Positive and negative validation | See the two tables above |
| Test data management | `src/test-data/*`: typed constants, data-driven baskets, Faker customers (optional `FAKER_SEED`) |
| Configuration management / environment support | `src/config/env.ts`: validated `.env` and environment variables; `ENV_FILE` to switch environments |
| Synchronization/wait strategy | Auto-waiting only; no `waitForTimeout` anywhere |
| Clear test structure | One spec per feature area; Arrange-Act-Assert; named steps in the E2E test |
| Logging | `src/utils/logger.ts`; an auto fixture in `src/fixtures/test.ts` logs the start and end of every test |
| Reporting (HTML) | Playwright HTML and JUnit XML reporters (`playwright.config.ts`) |
| Screenshot capture on failure | `screenshot: 'only-on-failure'`; video and trace also kept on failure |
| Independent and suite execution | Fresh browser context per test; saved login from the `setup` project; any file or test can run alone |
| Page Object Model, reusable utilities | `BasePage` → `SecuredPage` → concrete pages; `src/utils/price.ts`, `logger.ts` |
| Test grouping/tagging | `@smoke`, `@regression`, `@negative`, `@e2e`, `@known-defect` |
| Parallel execution | `fullyParallel: true`; cross-browser via `BROWSERS` |

## Part 2: API (Restful-Booker, REST Assured)

### Positive scenarios

| Requirement | Source | Test ID | Automation | Expected result |
|---|---|---|---|---|
| Service availability | Added | `HealthCheckTest#pingReportsServiceIsUp` | Single test (plus a fail-fast check in `BaseApiTest`) | 201 |
| Authentication | PDF | `AuthTest#validCredentialsReturnToken` | Schema `auth-token.json` | 200; token not blank |
| Successful POST | PDF | `BookingCrudTest#createBookingReturnsTheCreatedBooking` | Schema `create-booking-response.json` | 200; id > 0; echoed booking equals payload |
| Data-driven POST | PDF-Rec | `BookingCrudTest#bookingVariantsArePersistedAsSent` | ×5: no deposit, no optional field, price 0, same-day stay, accented names | Stored booking equals payload; schema `booking.json` |
| Successful GET | PDF | `BookingCrudTest#getBookingByIdReturnsTheStoredBooking` | Schema `booking.json` | 200; body equals payload |
| Name trimming | Added | `BookingCrudTest#namesAreTrimmedOnCreate` | Edge case | Stored names have no surrounding spaces |
| Successful PUT | PDF | `BookingCrudTest#updateBookingReplacesAllFields` | ×2: token, Basic auth | 200; response and re-fetched booking equal the replacement |
| Successful PATCH | PDF | `BookingCrudTest#partialUpdateChangesOnlyProvidedFields` | Verified by follow-up GET | Changed fields updated; all others unchanged |
| Successful DELETE | PDF | `BookingCrudTest#deleteBookingRemovesIt` | Verified by follow-up GET | 201 (API convention), then GET 404 |
| List bookings | Added | `BookingQueryTest#listingBookingsIncludesNewBooking` | Schema `booking-ids.json` | Contains the new id |
| Filter by name | Added | `BookingQueryTest#filteringByNameReturnsOnlyTheMatchingBooking` | Unique generated name | Exactly the created id |
| Filter with no match | Added | `BookingQueryTest#filteringByUnknownNameReturnsEmptyList` | | 200; empty array |

### Negative scenarios

| Requirement (PDF wording) | Source | Test ID | Automation | Expected result |
|---|---|---|---|---|
| Invalid authentication | PDF | `AuthTest#invalidCredentialsDoNotIssueToken` | ×5: wrong password, unknown user, wrong case, empty, missing | No token; reason "Bad credentials" (API uses 200; see KD-API-07) |
| Invalid authentication on writes | PDF | `BookingAuthorizationTest#writeWithoutValidCredentialsIsForbidden` | ×9: PUT/PATCH/DELETE × none/forged token/wrong password | 403; booking unchanged |
| Invalid resource ID | PDF | `BookingValidationTest#getUnknownBookingReturnsNotFound` | ×4: 999999999, 0, -1, "not-a-number" | 404 |
| Invalid resource ID on write | PDF | `BookingValidationTest#updateUnknownBookingIsRejected`, `#deleteUnknownBookingIsRejected` | | 4xx (API returns 405; see KD-API-04) |
| Missing mandatory fields (create) | PDF | `BookingValidationTest#createWithoutMandatoryFieldIsRejected` | ×7 fields | Rejected; no booking id (API returns 500; see KD-API-01) |
| Missing mandatory fields (update) | PDF | `BookingValidationTest#updateWithoutMandatoryFieldReturnsBadRequest` | ×7 fields | 400; booking unchanged |
| Invalid/missing input | PDF | `BookingValidationTest#createWithEmptyBodyIsRejected` | | Rejected; no booking id |
| Unsupported/invalid request data | PDF | `BookingValidationTest#createWithMalformedJsonReturnsBadRequest` | | 400 |
| Unsupported/invalid request data | PDF | `BookingValidationTest#createWithUnsupportedContentTypeIsRejected` | | Rejected (API returns 500; see KD-API-03) |
| Unsupported/invalid request data | PDF | `BookingValidationTest#getWithUnsupportedAcceptHeaderIsRejected` | | 418 (documented API behaviour) |
| Wrongly typed values | PDF | `BookingKnownDefectsTest#wronglyTypedFieldShouldReturnBadRequest` (KD-API-02) | ×4, known-defect group | Expected 400 (defect: accepted or 500) |

All nine known-defect tests (KD-API-01 to KD-API-09) are listed with steps, expected and actual results in [known-issues.md](known-issues.md).

### API framework requirements

| PDF requirement | Where it is implemented |
|---|---|
| REST Assured usage; reusable request methods | `client/BookingClient`, `AuthClient`, `HealthClient`: one method per operation, returning the raw `Response` |
| Request/response specification | `spec/RequestSpecFactory` (base URI, literal JSON media types, timeouts, filters); `spec/ResponseSpecFactory` (status, content type, schema) |
| Request payload handling | `model/*` records serialised by Jackson; `data/BookingDataFactory` builds valid and deliberately invalid payloads |
| Response validation; status code and JSON field validation | Response specs, AssertJ recursive comparison of deserialised POJOs, targeted field assertions |
| Authentication handling | `client/Auth` (token, Basic, none); `client/TokenManager` (thread-safe cached token) |
| Logging | `listener/Slf4jLoggingFilter` (INFO summary, DEBUG exchange, credentials masked); `listener/TestLoggingListener` (test name on every log line); `logback-test.xml` |
| Reporting | Allure (request/response attachments) and Surefire reports |
| Environment/configuration management | `config/ConfigManager`: `-D` > environment variable > `.env` > `config/<env>.properties` |
| Test data management | `data/BookingDataFactory`, `data/BookingField`; unique names; optional `-Ddata.seed`; automatic cleanup in `BaseApiTest` |
| JSON Schema validation (PDF-Rec) | `src/test/resources/schemas/*.json` |
| Data-driven and parameterised tests (PDF-Rec) | TestNG `@DataProvider` in `AuthTest`, `BookingCrudTest`, `BookingAuthorizationTest`, `BookingValidationTest`, `BookingKnownDefectsTest` |
| Test tagging (PDF-Rec) | TestNG groups `smoke`, `regression`, `negative`, `known-defect` |
| Parallel execution (PDF-Rec) | Surefire `parallel=methods`, `threadCount=4` (override with `-Dthread.count`) |
| Request/response POJOs (PDF-Rec) | `model/Booking`, `BookingDates`, `CreateBookingResponse`, `AuthRequest`, `AuthResponse` |
| Retry handling where appropriate (PDF-Rec) | Assertions are deliberately not retried. A fail-fast `/ping` health check handles an unavailable environment. The UI uses Playwright's built-in CI retry, and retried tests are reported as flaky. See README › Design decisions. |
