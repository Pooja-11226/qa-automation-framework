# Known Issues

These are **application defects in the demo systems**, not automation failures. The automation asserts the correct behaviour and does not work around them. See [test-strategy.md](test-strategy.md) §5 for how they are executed.

**How each defect was found:**
- **SauceDemo:** by reviewing the application's public source code (`saucelabs/sample-app-web`).
- **Restful-Booker:** by reviewing the API's public source code (`mwinteringham/restful-booker`; routes, validator and parser).

Every entry is exercised by an automated test. Run those tests to see the live behaviour:
- UI: `npm run test:known-defects`
- API: `mvn test -Pknown-defects`

If a public deployment behaves differently from the source code, update the "Actual" column after the first run.

**Severity scale:**
- **High:** data integrity or business-rule violation.
- **Medium:** incorrect contract or status code that misleads clients.
- **Low:** convention deviation.

## SauceDemo (UI)

| ID | Title | Steps | Expected | Actual | Severity | Test |
|---|---|---|---|---|---|---|
| KD-UI-01 | Checkout can be started and completed with an empty cart | Log in, open the cart without adding anything, click Checkout | Checkout is blocked, or the button is disabled, while the cart is empty | Checkout information page opens. The order can be completed for nothing. The cart page has no empty-cart guard. | High | `checkout.spec.ts` › KD-UI-01 |
| KD-UI-02 | Whitespace-only customer details are accepted | Add a product, check out, enter only spaces in First Name, Last Name and Postal Code, then Continue | Validation error for each field | Proceeds to the overview. Validation uses a truthiness check, so `"   "` passes. | Medium | `checkout.spec.ts` › KD-UI-02 |

**How the evidence appears in reports:**
- Both tests use `test.fail()`, so the HTML report shows them as **expected failures**.
- The failure message shows the actual behaviour, for example "Expected URL `/cart.html`, received `/checkout-step-one.html`".
- If SauceDemo fixes a defect, the test is reported as an unexpected pass and the build turns red. That is the signal to remove `test.fail()`.

## Restful-Booker (API)

| ID | Title | Steps to reproduce | Expected | Actual | Severity | Test (`BookingKnownDefectsTest`) |
|---|---|---|---|---|---|---|
| KD-API-01 | Missing mandatory field returns a server error | `POST /booking` without e.g. `firstname` | 400 Bad Request | 500 Internal Server Error. PUT correctly returns 400 for the same payload, so the two operations are inconsistent. | Medium | `createWithoutMandatoryFieldShouldReturnBadRequest` (7 fields) |
| KD-API-02 | Field types are not validated | `POST /booking` with `totalprice: "one hundred"`, `depositpaid: "yes"`, `checkin: "not-a-date"`, or `firstname: 12345` | 400 Bad Request | String values are accepted and coerced: price stored as `null`, deposit as `true`, date as an invalid value. A numeric name crashes name trimming and returns 500. | High | `wronglyTypedFieldShouldReturnBadRequest` (4 cases) |
| KD-API-03 | Unsupported Content-Type returns a server error | `POST /booking` with `Content-Type: text/plain` | 415 Unsupported Media Type | 500 | Medium | `unsupportedContentTypeShouldReturn415` |
| KD-API-04 | Write to a non-existent booking returns 405 | `PUT` or `DELETE /booking/999999999` with valid auth | 404 Not Found | 405 Method Not Allowed | Medium | `updateUnknownBookingShouldReturnNotFound`, `deleteUnknownBookingShouldReturnNotFound` |
| KD-API-05 | Check-out before check-in is accepted | `POST /booking` with checkout 3 days before checkin | 400 Bad Request | 200; booking stored | High | `checkoutBeforeCheckinShouldBeRejected` |
| KD-API-06 | A rejected create still stores the booking | `POST /booking` with `Accept: text/plain` | 418 and nothing stored | 418, but the booking is saved before the response format is checked. The record can be found with `GET /booking?firstname=…`. | High | `rejectedAcceptHeaderShouldNotCreateBooking` |
| KD-API-07 | Failed login returns 200 | `POST /auth` with a wrong password | 401 Unauthorized | 200 with `{"reason":"Bad credentials"}` | Medium | `invalidCredentialsShouldReturnUnauthorized` |
| KD-API-08 | PUT does not replace optional fields | Create with `additionalneeds`, then `PUT` a full booking without it | `additionalneeds` removed (full replacement) | Old value retained. PUT merges into the stored record. | Medium | `putShouldReplaceTheWholeBooking` |
| KD-API-09 | Decimal prices are truncated | `POST /booking` with `totalprice: 150.75` | 150.75 stored, or 400 if only integers are allowed | 150 stored silently | High | `decimalTotalPriceShouldBePreserved` |

**How the main suite handles these:** where the API rejects bad input but with the wrong status class (KD-API-01, 03 and 04), the blocking tests in `BookingValidationTest` assert the dependable contract: the request is refused and no booking id is returned. The precise status code is asserted only in the known-defect tests.

## Observations (convention deviations, not tracked as defects)

| Observation | Detail | How the suite treats it |
|---|---|---|
| `POST /booking` returns 200 instead of 201 Created | REST convention for resource creation is 201 | Asserted as documented (200) |
| `DELETE /booking/{id}` returns 201 instead of 200/204 | Documented by the API owner | Asserted as documented (201) |
| `GET /ping` returns 201 | Documented health-check response | Asserted as documented (201) |
| Unsupported `Accept` on GET returns 418 "I'm a Teapot" | Documented; 406 Not Acceptable is conventional | Asserted as documented (418) |
| The public Restful-Booker instance is shared by everyone | Other users can create or modify data at any time | Tests use unique generated names, never depend on pre-existing ids, and clean up after themselves |
| SauceDemo session cookie lasts 10 minutes | The saved login (`.auth/standard-user.json`) expires after 10 minutes | The `setup` project re-creates it at the start of every run |
