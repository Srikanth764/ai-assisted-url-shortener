# AI-Assisted Engineering Log

This document records the recent engineer-reviewed changes to the URL
Shortener prototype. AI assistance was used for exploration, implementation,
test updates, and documentation review. The resulting changes were validated
with the Maven test suite.

## H2-Only Runtime

The application was simplified to a single Spring Boot instance using a
file-backed H2 database at `./data/urlshortener`. The former external-service
deployment assets were removed because the target workflow only requires local
filesystem persistence.

Tests use a separate in-memory H2 database configured under
`src/test/resources/application.properties`. This prevents application runs
from locking the persistent local database during JUnit execution.

## Browser Workflows

The static browser interface was aligned with the supplied reference designs:

- `Create Link` uses a compact side-by-side form on desktop.
- `Manage Links` displays a searchable table with status, clicks, dates, and
  actions.
- `Analytics` provides a persisted-link selector and detailed analytics views.
- The analytics dashboard shows total clicks, unique visitors, clicks over
  time, and device, browser, operating-system, and country distributions.
- The layout collapses into a single column on narrow screens.

## API Additions

The Manage Links workflow required these endpoints:

- `GET /api/v1/urls` lists link summaries without exposing JPA entities.
- `PUT /api/v1/urls/{shortCode}` updates the destination and expiration.
- `DELETE /api/v1/urls/{shortCode}/permanent` permanently deletes a link.
- `DELETE /api/v1/urls/{shortCode}` remains the soft-disable operation.

Click events now retain derived device and operating-system values plus a
visitor key based on the request address. This supports unique-visitor counts
without introducing an external analytics service.

## URL Validation

Original destinations are validated with a URI-based custom constraint in both
create and update requests. Only absolute HTTP and HTTPS URLs with a valid host
are accepted. Credentials, fragments, whitespace, control characters, unsafe
delimiters, malformed hosts, and other schemes are rejected before the service
layer runs.

The update request validates HTTP/HTTPS URLs and future expiration timestamps.

## Verification

The final Maven run completed with:

```text
Tests run: 82
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Coverage includes service mapping and mutation behavior, MockMvc endpoint
contracts, validation, integration flows, redirect behavior, analytics, and
browser-facing application startup.
