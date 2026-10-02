# Sample Outputs

The assignment asks for sample outputs. They must come from a **real run** against the live demo systems, so capture them on your machine with the steps below and commit the files to this folder. The figures under "Expected results" describe what a healthy run should produce. They are reference values, not captured output.

## How to capture

```bash
# UI
cd ui-automation
npm ci && npx playwright install chromium && cp .env.example .env
npx playwright test 2>&1 | tee ../docs/sample-outputs/ui-console.txt
npx playwright show-report      # take screenshots of the overview and of one test with its steps

# API
cd ../api-automation
cp .env.example .env
mvn clean test 2>&1 | tee ../docs/sample-outputs/api-console.txt
mvn allure:serve                # take screenshots of the overview and of one test with request/response attachments

# Known-defect evidence
mvn test -Pknown-defects 2>&1 | tee ../docs/sample-outputs/api-known-defects-console.txt
```

Suggested files to commit:

| File | Content |
|---|---|
| `ui-console.txt` | Playwright console output |
| `ui-html-report.png` | Playwright HTML report overview |
| `ui-e2e-steps.png` | The E2E test expanded, showing its named steps |
| `ui-known-defect.png` | KD-UI-01 showing "expected to fail" and the actual URL |
| `api-console.txt` | Maven/TestNG console output |
| `api-allure-overview.png` | Allure overview |
| `api-allure-request.png` | One test with its request/response attachments |
| `api-known-defects-console.txt` | Known-defect run showing the actual status codes |

## Expected results

### UI: `npx playwright test` (chromium)

- 30 entries: the `setup` project's login step plus 29 test cases in the chromium project.
- **30 passed.** This count includes the 2 `@known-defect` tests, which Playwright reports as passed because they failed as expected.
- `npm run test:smoke` runs the setup, the valid-login test and the E2E purchase test.

### API: `mvn clean test`

`Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`. Data-provider rows each count as a test:

| Class | Tests |
|---|---|
| HealthCheckTest | 1 |
| AuthTest | 6 |
| BookingCrudTest | 12 |
| BookingQueryTest | 3 |
| BookingAuthorizationTest | 9 |
| BookingValidationTest | 24 |

`mvn test -Dgroups=smoke` runs 9 tests.

### API: `mvn test -Pknown-defects`

19 tests are expected to **fail**, each failure documenting a confirmed defect. Each failure message shows the actual status code and response body, which is the defect evidence.

## If results differ

| Symptom | Likely cause | Action |
|---|---|---|
| API run stops with "API health check failed" | Restful-Booker is down or asleep (free hosting) | Retry after a minute; check `https://restful-booker.herokuapp.com/ping` |
| A known-defect test passes | The demo app fixed the defect | Remove `test.fail()` (UI) or move the test into the main suite (API), and update `known-issues.md` |
| UI tests fail at login with a timeout | Site unreachable, or a corporate proxy | Open `https://www.saucedemo.com` in a browser; re-run with `--headed` |
