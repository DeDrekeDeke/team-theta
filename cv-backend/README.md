# CV Backend

Spring Boot backend for the educational CV Manager starter application.

## Run Locally

Start PostgreSQL from the repository root:

```bash
docker compose up -d db
```

Then run the backend:

```bash
cd cv-backend
./gradlew bootRun
```

On Windows PowerShell:

```powershell
cd cv-backend
.\gradlew.bat bootRun
```

Run tests:

```bash
cd cv-backend
./gradlew test
```

On Windows PowerShell:

```powershell
cd cv-backend
.\gradlew.bat test
```

The project targets Java 21 and includes a project-local Gradle wrapper.

The backend runs at:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/api/health
```

## API

Demo login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "user123"
}
```

CV endpoints:

```text
GET  /api/cvs
GET  /api/cvs/{id}
GET  /api/cvs/{id}/html
GET  /api/cvs/search?q=alice
POST /api/cvs/upload
PUT  /api/cvs/{id}
POST /api/cvs/legacy-preview
```

Upload expects `multipart/form-data` fields:

```text
ownerUserId
title
file
```

Admin/settings endpoints:

```text
GET /api/admin/settings
PUT /api/admin/settings/{key}
POST /api/users
GET /api/users
GET /api/users/{id}
```

Admin user creation:

```http
POST /api/users
Authorization: Bearer demo-token-1
Content-Type: application/json

{
  "email": "carol@example.com",
  "displayName": "Carol Candidate",
  "password": "carol123"
}
```

AI placeholder endpoints:

```text
GET  /api/cvs/{cvId}/ai-actions/suggestions
POST /api/cvs/{cvId}/ai-actions/improve-summary
```

Default local database settings:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cvmanager
SPRING_DATASOURCE_USERNAME=cvmanager
SPRING_DATASOURCE_PASSWORD=cvmanager
```

`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` can still be used to override these values. When
those are not set, the application builds the JDBC connection from PostgreSQL
environment variables:

```text
PGHOST=localhost
PGPORT=5432
PGDATABASE=cvmanager
PGUSER=cvmanager
PGPASSWORD=cvmanager
```

## Railway PostgreSQL

If the backend is deployed to Railway, add a PostgreSQL database service and set
these variables on the backend service using Railway variable references:

```text
PGHOST=${{Postgres.PGHOST}}
PGPORT=${{Postgres.PGPORT}}
PGDATABASE=${{Postgres.PGDATABASE}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}
```

If your Railway database service has a different name than `Postgres`, use that
service name in the variable references. Without these variables, the backend
falls back to `localhost:5432`, which will fail inside Railway.

## Password Notes

- Demo users are seeded in `src/main/resources/db/migration/V2__seed_data.sql`.
- `src/main/resources/db/migration/V3__migrate_demo_user_password_hashes.sql` migrates those seeded plain-text passwords to BCrypt hashes and keeps the same demo credentials usable after migration.
- New users created via `POST /api/users` are stored with BCrypt-hashed passwords.
- Login verifies the submitted password against the stored hash.
- User and login responses do not expose plain-text passwords or password hashes.
