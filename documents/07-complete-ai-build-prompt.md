# Complete AI Build Prompt

Copy the prompt below into an AI coding agent to recreate this application from
scratch.

---

## Prompt

You are a senior Java and Spring Boot engineer. Build a complete URL Shortener
application from scratch in the current workspace. Do not ask for permission
between normal implementation steps; inspect, implement, test, and document the
project end to end.

### Product Goal

Create a single-instance URL shortener that accepts valid HTTP and HTTPS URLs,
generates short shareable URLs, redirects visitors, tracks clicks, and provides
link management and analytics through both REST APIs and a static browser UI.

### Mandatory Technology Choices

- Java 17.
- Spring Boot 4.x compatible APIs.
- Spring Web MVC.
- Spring Data JPA and Hibernate.
- Jakarta Bean Validation.
- H2 only.
- File-backed H2 database at `./data/urlshortener`.
- Apache Maven.
- JUnit 5, Mockito, MockMvc, and Spring integration tests.
- No Docker.
- No Docker Compose.
- No NGINX.
- No PostgreSQL.
- No Redis.
- No cache abstraction.
- No Kafka, RabbitMQ, Kubernetes, or external analytics service.

Do not add Maven Wrapper files. Apache Maven must be installed on `PATH`.

### Repository Structure

Create a conventional Maven project with:

```text
pom.xml
README.md
documents/
src/main/java/com/assignment/urlshortener/
  UrlShortenerApplication.java
  controller/
  dto/
  entity/
  exception/
  repository/
  service/
  util/
  validation/
src/main/resources/
  application.properties
  static/index.html
  static/app.js
  static/styles.css
src/test/java/com/assignment/urlshortener/
src/test/resources/application.properties
```

Use constructor injection. Keep controllers thin. Do not expose JPA entities
directly from APIs.

### Runtime Configuration

Use these production properties:

```properties
spring.application.name=url-shortener
app.base-url=http://localhost:8080
spring.datasource.url=jdbc:h2:file:./data/urlshortener
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Use a test-only `src/test/resources/application.properties` with an isolated
in-memory H2 database:

```properties
spring.datasource.url=jdbc:h2:mem:urlshortener-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
app.base-url=http://localhost:8080
```

### Data Model

Implement `ShortUrl` as a JPA entity with:

- Generated Long ID.
- Original URL, maximum 2,048 characters.
- Unique short code.
- Immutable creation timestamp.
- Click count.
- Nullable last-accessed timestamp.
- Active boolean defaulting to true.
- Nullable expiration timestamp.

Implement `ClickEvent` with:

- Generated Long ID.
- Short code.
- Click timestamp.
- Country.
- Browser.
- Device.
- Operating system.
- Visitor key.

New analytics fields must remain nullable at the database level so an existing
file-backed H2 database can migrate without failing on legacy rows. Treat null
analytics dimensions as `Unknown` in responses.

### URL Validation

Create a reusable custom Bean Validation annotation and validator for strict
original URL validation. Apply it to create and update request DTOs.

Accept only absolute URLs with:

- Scheme exactly `http` or `https`, case-insensitive.
- A valid non-empty host.
- Maximum length 2,048 characters.

Reject:

- Relative URLs.
- Missing hosts.
- `ftp`, `file`, `javascript`, `data`, and all other schemes.
- User information or credentials.
- Fragments.
- Whitespace and control characters.
- Quotes, backticks, `<`, and `>`.
- Malformed URI syntax.

Trim input before validation where appropriate, but do not silently repair
unsafe internal whitespace.

### Short-Code Rules

When no custom alias is provided:

- Generate exactly seven Base62 characters using `SecureRandom`.
- Check uniqueness through the repository.
- Retry at most five times.
- Throw a dedicated exception if all attempts collide.

For custom aliases:

- Trim and lowercase.
- Length 4 through 30.
- Regex: `^[a-z0-9_-]+$`.
- Reject reserved values: `api`, `admin`, `login`, `logout`, `docs`, `swagger`.
- Reject duplicates with a conflict response.

### Required REST APIs

Implement exactly these application APIs:

```text
POST   /api/v1/urls
GET    /api/v1/urls
GET    /api/v1/urls/{shortCode}/click-analytics
PUT    /api/v1/urls/{shortCode}
DELETE /api/v1/urls/{shortCode}
DELETE /api/v1/urls/{shortCode}/permanent
GET    /{shortCode}
```

Do not implement a separate aggregate analytics endpoint. The management list
and detailed click analytics endpoint replace it.

#### Create

Request:

```json
{
  "originalUrl": "https://example.com/article",
  "expiresAt": "2027-01-01T00:00:00Z",
  "customAlias": "article"
}
```

Return `201 Created` with short code, short URL, original URL, creation time,
and expiration.

#### Redirect

For an active non-expired link:

- Increment click count atomically.
- Update last-accessed timestamp.
- Record a click event asynchronously.
- Return `302 Found` with a `Location` header.

Return `404` for missing/inactive links and `410` for expired links.

#### Management

`GET /api/v1/urls` returns DTO summaries containing short code, short URL,
original URL, clicks, created time, expiration, and active status.

`PUT /api/v1/urls/{shortCode}` accepts a validated original URL and optional
future expiration. Preserve the short code.

`DELETE /api/v1/urls/{shortCode}` performs a soft disable.

`DELETE /api/v1/urls/{shortCode}/permanent` permanently removes the mapping.

#### Detailed Analytics

Return a DTO containing:

```json
{
  "shortCode": "abc1234",
  "totalEvents": 12,
  "uniqueVisitors": 7,
  "byBrowser": {"Chrome": 8, "Firefox": 4},
  "byDevice": {"Desktop": 9, "Mobile": 3},
  "byOperatingSystem": {"Windows": 7, "macOS": 5},
  "byCountry": {"Unknown": 12}
}
```

Use `CF-IPCountry` when supplied. For local requests without that header, store
`Unknown`. Use the request remote address as the visitor key. Explain in the
README that local country values are normally `Unknown` without a geolocation
proxy.

### Exception Handling

Create a global exception handler with structured error responses for:

- Validation errors: `400`.
- Missing links: `404`.
- Alias conflicts: `409`.
- Expired links: `410`.
- Short-code generation exhaustion: `503`.
- Unexpected errors: `500` with no stack trace, SQL, or internal path leakage.

### Browser UI

Create a static UI served by Spring Boot with three top-level tabs:

1. **Create Link**
   - Blue header and concise subtitle.
   - Original URL, custom alias, and expiration fields arranged side by side on
     desktop.
   - Clear validation errors.
   - Created-link result with copy and open actions.

2. **Manage Links**
   - Fetch `GET /api/v1/urls` on load and refresh.
   - Search by short code or original URL.
   - Table columns: short URL, destination, status, clicks, created, expires,
     actions.
   - Edit action calls the PUT endpoint.
   - Disable action calls the soft-delete endpoint.
   - Delete action confirms and calls the permanent-delete endpoint.
   - Show empty and error states.

3. **Analytics**
   - Populate a link selector from the management list.
   - On selection, call `/click-analytics`.
   - Show Total Clicks and Unique Visitors KPI cards.
   - Show clicks-over-time visualization.
   - Show Device, Browser, Operating System, and Country distribution cards.
   - Use CSS/DOM visualization or a small dependency-free chart; do not add a
     backend chart service.

Use responsive CSS so analytics cards and forms collapse to one column on small
screens. Preserve accessible labels, keyboard operation, and readable error
messages.

### Testing Requirements

Write tests for:

- Base62 length, alphabet, and collision retries.
- Strict HTTP/HTTPS URL validation.
- Rejection of unsafe schemes, credentials, fragments, whitespace, malformed
  hosts, and overlong URLs.
- Alias normalization, format, reserved values, duplicates, and race conflicts.
- Create, redirect, expiration, disabled-link behavior, and click counting.
- Management list mapping.
- Edit and permanent delete behavior.
- Detailed analytics mappings and null/legacy analytics fields.
- Browser, device, and operating-system detection.
- Controller status codes and structured errors with MockMvc.
- Integration flows using test-only in-memory H2.
- Application context startup.

Tests must never use or modify the production `./data/urlshortener` database.

### Documentation Requirements

Create:

- `README.md` with product overview, architecture, API reference, validation,
  UI workflows, setup, test command, and limitations.
- `documents/04-setup-instructions.md` with Java/Maven setup, run, test, and H2
  reset commands.
- `documents/05-ai-assisted-engineering-log.md` describing engineering choices.
- `documents/06-software-requirements-specification.md` containing the complete
  SRS.
- This prompt as `documents/07-complete-ai-build-prompt.md`.

Do not document Docker, PostgreSQL, Redis, NGINX, caching, Maven Wrapper,
Actuator, or multi-instance deployment because they are not part of this
application.

### Required Verification Workflow

After implementation:

1. Inspect the complete source tree.
2. Search for forbidden references: Docker, Redis, PostgreSQL, NGINX, cache,
   Maven Wrapper, and aggregate analytics.
3. Run `mvn clean test`.
4. Run `git diff --check`.
5. Check that no credentials or generated H2 files are tracked.
6. Report changed files, API endpoints, test count, and any remaining risks.

Do not commit automatically. Keep changes focused and do not introduce
unrequested infrastructure.

---

## End Prompt
