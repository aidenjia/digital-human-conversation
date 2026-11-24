# Verification

Date: 2026-10-19

- Maven full verify: PASS, 321 unit/MVC tests and 9 integration tests, including 15 new motion tests.
- Frontend production build: PASS.
- ESLint on all three changed/new TS/TSX files: PASS.
- Existing avatar tests: PASS, 15 tests.
- git diff --check: PASS.
- Full frontend ESLint: blocked by two existing preserve-caught-error violations in src/apis/auth.ts (lines 67 and 83); that file was not changed.
- OpenSpec strict CLI validation: NOT COMPLETED. Automatic approval review rejected the command because it attempted an unverified external network destination with uncertain disclosure risk. No workaround or retry was attempted.

Maven in this restricted Windows environment required the existing ByteBuddy startup agent and a workspace-owned temporary directory. The default sandbox temporary directory denied rename operations; tests passed when JVM java.io.tmpdir pointed at target/test-tmp. No project-wide test configuration was changed.

Command used (from repository root, after creating target/test-tmp):

~~~powershell
mvn -o spotless:apply '-DargLine=-javaagent:<local-path> -Djava.io.tmpdir=<local-path> verify
~~~

No live Tencent generation, billed API requests, VRM retargeting, or browser visual acceptance test was performed. Provider calls were simulated in tests. The feature remains opt-in and requires account setup before real use.

## TokenHub migration verification — 2026-10-20

- Full offline Maven verify: PASS, 326 unit/MVC tests plus 9 integration tests (335 total).
- Motion-specific coverage: 20 tests, including actual HTTP request construction via OkHttp interceptors, Bearer headers, snake_case payloads, all provider statuses, malformed replies, sanitized auth/billing/rate errors, timeout without retries, and legacy JSON/cache compatibility.
- Spotless formatting/check: PASS.
- git diff --check: PASS.
- Frontend was not changed by this migration; existing REST response and local status names remain unchanged.
- No real API Key was requested, read or stored. No billable provider request was made.
- OpenSpec CLI was not rerun because of its previously recorded automatic approval rejection for unverified network access.

Configure TOKENHUB_API_KEY in the server process environment, optionally TOKENHUB_BASE_URL for the matching region, then restart Spring Boot before the first live test.
