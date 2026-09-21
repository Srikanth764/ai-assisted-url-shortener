# Software Requirements Specification

## 1. Document Control

| Field | Value |
|---|---|
| Product | URL Shortener |
| Version | 1.0 |
| Runtime | Java 17, Spring Boot, H2 |
| Storage | File-backed H2 at `./data/urlshortener` |
| Deployment target | Single local application instance |
| External services | None required |

## 2. Purpose

The system converts long HTTP and HTTPS URLs into short, shareable URLs. Users
can create links, redirect visitors, inspect analytics, edit destinations,
disable links, and permanently delete links through a browser interface and
REST API.

## 3. Scope

### 3.1 In Scope

- HTTP and HTTPS URL shortening.
- Seven-character Base62 short-code generation.
- Optional custom aliases.
- Future expiration dates.
- HTTP 302 redirects.
- Click-count tracking.
- Asynchronous click-event recording.
- Browser, device, operating-system, and country analytics.
- Unique visitor counts based on request address.
- Link management and search.
- Link editing, disabling, and permanent deletion.
- File-backed H2 persistence.
- Responsive static browser UI.
- Unit, controller, validation, and integration tests.

### 3.2 Out of Scope

- User accounts and authentication.
- Multi-user ownership.
- PostgreSQL, Redis, Docker, NGINX, or other external services.
- Multi-instance deployment.
- Rate limiting.
- Custom domains.
- Guaranteed geographic resolution for local IP addresses.
- Changing an existing short code or alias.

## 4. User Roles

The current prototype has one anonymous user role. Anyone who can access the
application can create and manage links. Authentication and authorization are
future scope.

## 5. Functional Requirements

### FR-001 Create a Short URL

The system shall accept a `POST /api/v1/urls` request containing `originalUrl`
and optional `customAlias` and `expiresAt`.

The system shall return HTTP `201 Created` with:

- `shortCode`
- `shortUrl`
- `originalUrl`
- `createdAt`
- `expiresAt`

### FR-002 Generate Short Codes

When no alias is supplied, the system shall generate a seven-character code
using `SecureRandom` and Base62 characters: `0-9`, `a-z`, and `A-Z`.

The system shall check database uniqueness and retry up to five times. If all
attempts collide, it shall return HTTP `503`.

### FR-003 Custom Aliases

The system shall:

- Trim aliases.
- Normalize aliases to lowercase.
- Accept 4 to 30 characters.
- Allow lowercase letters, digits, hyphens, and underscores.
- Reject reserved routes such as `api`, `admin`, `login`, `logout`, `docs`, and
  `swagger`.
- Reject duplicate aliases with HTTP `409`.
- Preserve the alias during redirects and edits.

### FR-004 Strict Original URL Validation

The system shall accept only absolute URLs with an `http` or `https` scheme.

The URL shall have a valid host and be no longer than 2,048 characters.

The system shall reject:

- `ftp`, `file`, `javascript`, `data`, and other schemes.
- Relative URLs and missing hosts.
- User information or embedded credentials.
- Fragments.
- Whitespace and control characters.
- Unsafe delimiters such as quotes, backticks, `<`, and `>`.
- Malformed URI syntax.

Invalid input shall return HTTP `400` with a structured field error.

### FR-005 Expiration

`expiresAt` shall be an optional complete ISO-8601 instant in the future.
Expired links shall return HTTP `410 Gone` and shall not increment clicks.

### FR-006 Redirect

The system shall resolve `GET /{shortCode}` for codes matching
`[a-zA-Z0-9_-]+`.

For an active, non-expired link it shall:

1. Read the mapping from H2.
2. Atomically increment click count and update last-accessed time.
3. Record a detailed click event asynchronously.
4. Return HTTP `302 Found` with the original URL in `Location`.

Missing or inactive links shall return HTTP `404`. Expired links shall return
HTTP `410`.

### FR-007 Manage Links

`GET /api/v1/urls` shall return all link summaries without exposing JPA
entities. Each summary shall contain:

- Short code and short URL.
- Original URL.
- Click count.
- Created time.
- Expiration time.
- Active status.

The UI shall support searching by short code or original URL.

### FR-008 Edit Links

`PUT /api/v1/urls/{shortCode}` shall update the original URL and optional
expiration while preserving the short code.

The same strict URL and future-expiration validation shall apply to edits.

### FR-009 Disable Links

`DELETE /api/v1/urls/{shortCode}` shall soft-disable an active link. Disabled
links shall remain visible in the management table but shall not redirect.

Repeated disable requests shall return an appropriate conflict response.

### FR-010 Permanently Delete Links

`DELETE /api/v1/urls/{shortCode}/permanent` shall permanently remove the link.
Unknown links shall return HTTP `404`.

### FR-011 Detailed Analytics

`GET /api/v1/urls/{shortCode}/click-analytics` shall return:

- Short code.
- Total click events.
- Unique visitors.
- Browser distribution.
- Device distribution.
- Operating-system distribution.
- Country distribution.

Click events shall be persisted asynchronously. The analytics view may be
briefly eventually consistent after a redirect.

The country value shall use the `CF-IPCountry` request header when present and
`Unknown` otherwise. Local browser requests normally produce `Unknown` because
local traffic has no geographic provider header.

### FR-012 Browser UI

The UI shall provide three tabs:

- **Create Link:** side-by-side URL, alias, expiry, and submit controls.
- **Manage Links:** searchable table with status, dates, clicks, edit, disable,
  delete, and refresh actions.
- **Analytics:** link selector, total-click and unique-visitor metrics, clicks-
  over-time visualization, and device/browser/OS/country distributions.

The UI shall be responsive and collapse to a single-column layout on narrow
screens.

## 6. REST API Contract

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/v1/urls` | Create link | `201` |
| GET | `/api/v1/urls` | List links | `200` |
| GET | `/api/v1/urls/{shortCode}/click-analytics` | Detailed analytics | `200` |
| PUT | `/api/v1/urls/{shortCode}` | Edit destination/expiry | `204` |
| DELETE | `/api/v1/urls/{shortCode}` | Disable link | `204` |
| DELETE | `/api/v1/urls/{shortCode}/permanent` | Delete link | `204` |
| GET | `/{shortCode}` | Redirect | `302` |

## 7. Data Requirements

### ShortUrl

- `id`: generated Long primary key.
- `originalUrl`: required, maximum 2,048 characters.
- `shortCode`: required, unique.
- `createdAt`: required and immutable.
- `clickCount`: non-negative long.
- `lastAccessedAt`: nullable instant.
- `active`: boolean, default true.
- `expiresAt`: nullable instant.

### ClickEvent

- `id`: generated Long primary key.
- `shortCode`: required.
- `clickedAt`: required instant.
- `country`: required string, commonly `Unknown` locally.
- `browser`: required derived value.
- `device`: nullable for legacy rows.
- `operatingSystem`: nullable for legacy rows.
- `visitorKey`: nullable for legacy rows.

The application shall tolerate existing H2 rows created before the newer
analytics fields were introduced.

## 8. Non-Functional Requirements

### NFR-001 Maintainability

Use controller, service, repository, entity, DTO, exception, utility, and
validation layers. Controllers shall not contain business logic.

### NFR-002 Security

Validate all external input, avoid exposing entities, reject unsafe URL schemes,
return structured errors, and avoid leaking internal exception details.

### NFR-003 Persistence

Application data shall survive application restarts through file-backed H2.
Tests shall use isolated in-memory H2 and shall never modify production data.

### NFR-004 Usability

The browser interface shall use clear labels, responsive layout, accessible
controls, useful empty states, and actionable error messages.

### NFR-005 Testability

The project shall include JUnit 5 tests for utilities, services, controllers,
validation, redirects, analytics, and integration flows.

### NFR-006 Portability

The application shall run on Java 17 or newer with Apache Maven installed.
No container runtime or external infrastructure shall be required.

## 9. Error Handling

The global exception handler shall return structured responses for validation,
not-found, conflict, expiration, short-code generation, and unexpected errors.
Unexpected errors shall not expose stack traces, SQL, filesystem paths, or
internal class names.

## 10. Acceptance Criteria

The implementation is acceptable when:

1. A valid HTTP or HTTPS URL creates a short link.
2. Invalid schemes and unsafe URL forms return HTTP `400`.
3. Generated codes are unique and seven characters long.
4. Custom aliases normalize and enforce uniqueness.
5. Redirects return HTTP `302` and increment click counts.
6. Expired and disabled links do not redirect.
7. File-backed H2 data survives application restart.
8. Manage Links lists previously persisted links.
9. Edit, disable, and permanent-delete actions work.
10. Analytics returns all documented dimensions.
11. The UI matches the three-tab workflow and is responsive.
12. `mvn clean test` completes without failures or errors.
