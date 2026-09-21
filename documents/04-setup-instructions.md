# Setup Instructions

The application runs as a single Spring Boot process with a file-backed H2
database. No external database, cache, container runtime, or message broker is
required.

## Prerequisites

- Java 17 or newer
- Git

The Maven Wrapper is included, so Maven does not need to be installed
separately.

## Clone and Run

```bash
git clone https://github.com/<your-github-username>/<your-repository>.git
cd <your-repository>
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The application is available at:

```text
http://localhost:8080
```

H2 stores data under `./data/urlshortener`. The directory and database files
are created automatically. Stop the application before moving or deleting the
H2 files.

## Run Tests

macOS/Linux:

```bash
./mvnw clean test
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

## Health Check

```text
http://localhost:8080/actuator/health
```

The expected status is `UP`.

## Reset Local Data

Stop the application, then remove the local H2 files when a clean database is
needed:

macOS/Linux:

```bash
rm -rf data
```

Windows PowerShell:

```powershell
Remove-Item -Recurse -Force .\data
```
