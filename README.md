# CV Manager Summer School

Educational starter application for a one-week CV manager improvement project.

## Repository Structure

```text
.
  cv-backend/       Spring Boot backend
  cv-frontend/      React + TypeScript frontend
  docker-compose.yml
  .env.example
```

The backend and frontend starter applications are scaffolded.

## Prerequisites

- Java JDK for the Spring Boot backend
- Node.js for the React frontend
- Docker and Docker Compose
- Git

## Local Setup

1. Create a local environment file:

   ```bash
   cp .env.example .env
   ```

   On Windows PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Start PostgreSQL from the repository root:

   ```bash
   docker compose up -d db
   ```

3. Check that the database is running:

   ```bash
   docker compose ps
   ```

PostgreSQL will be available on `localhost:5432` with the default values from
`.env.example`:

```text
database: cvmanager
username: cvmanager
password: cvmanager
```

## Backend

Backend code will live in `cv-backend`.

Backend stack:

- Java
- Spring Boot
- PostgreSQL
- Flyway migrations

Local command:

```bash
cd cv-backend
./gradlew bootRun
```

On Windows PowerShell:

```powershell
cd cv-backend
.\gradlew.bat bootRun
```

The backend is a Gradle project targeting Java 21 and includes a project-local
Gradle wrapper.

The backend runs at:

```text
http://localhost:8080
```

## Frontend

Frontend code will live in `cv-frontend`.

Frontend stack:

- React
- TypeScript
- Vite

Local commands:

```bash
cd cv-frontend
npm install
npm run dev
```

The frontend runs at:

```text
http://localhost:5173
```

The frontend calls the backend at `http://localhost:8080` by default. Override
that with `VITE_API_BASE_URL` if needed.

## Demo Users

```text
admin@example.com / admin123
alice@example.com / user123
bob@example.com / user123
```

## Useful API Calls

```text
POST http://localhost:8080/api/auth/login
GET  http://localhost:8080/api/cvs
GET  http://localhost:8080/api/cvs/search?q=Alice
GET  http://localhost:8080/api/cvs/1
GET  http://localhost:8080/api/cvs/1/html
POST http://localhost:8080/api/cvs/upload
GET  http://localhost:8080/api/admin/settings
GET  http://localhost:8080/api/users
POST http://localhost:8080/api/users
PUT  http://localhost:8080/api/users/1
POST http://localhost:8080/api/cvs/1/ai-actions/improve-summary
```

Integration note for CVM-008:

- User management reuses the `/api/users` endpoints introduced by CVM-001.
- New-user passwords are stored through the pulled BCrypt `PasswordEncoder`
  flow, and role updates use the existing `admin` flag on `user_account`.

## Reset Local Database

To delete local database data and recreate the PostgreSQL container:

```bash
docker compose down -v
docker compose up -d db
```

This removes all local database data.

## Project Notes

- Keep changes small and reviewable.
- Use Flyway migrations for backend database schema changes.
- Do not commit real secrets or local `.env` files.
- Keep the application runnable locally as it grows.
