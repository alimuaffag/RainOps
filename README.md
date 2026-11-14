# RainOps

Rainwater incident management application with a responsive Arabic interface.

## Features

- Dashboard totals by status and priority
- Paginated incident listing with title/district search and combined filters
- Incident creation and editing with validated coordinates
- Status transitions, resolution timestamps, and chronological history
- Mobile layouts, loading/empty/error states, and retry actions

## Stack

Java 21, Spring Boot, Spring Data JPA, PostgreSQL, Flyway, React, TypeScript, Vite.

## Layout

- backend/ — REST API, database migrations, integration tests
- frontend/ — React application and browser workflow tests
- scripts/ — local database lifecycle helper

## Local development

Prerequisites: Java 21, Node.js 24, PostgreSQL 17.

For an existing initialized local database on this workstation:

```powershell
.\scripts\database.ps1 start
cd backend
.\mvnw.cmd spring-boot:run
```

For a fresh environment, provision a PostgreSQL database and set DB_URL (JDBC URL), DB_USERNAME, and DB_PASSWORD before starting the backend. Flyway applies the schema migrations automatically. Local credentials may be kept in the git-ignored .local/database.properties.

Start the frontend in another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open http://127.0.0.1:5173. The API runs on 127.0.0.1:8080. The Vite development proxy forwards /api requests to the API.

## Verification

Backend: run ./mvnw verify from backend/ with a dedicated test database configured.

Frontend: run npm run lint, npm run build, and npm test from frontend/. Install the browser once with npx playwright install chromium.

## Demo deployment

The root Dockerfile builds React and packages it inside Spring Boot for same-origin deployment. render.yaml defines a free Render web service and isolated PostgreSQL database. Render generates DEMO_PASSWORD; the reviewer username is reviewer.

The demo profile enables session-based login, CSRF protection, secure cookies, SPA routes, and optional sample incidents. Local development remains unauthenticated. This is a shared reviewer account, not a full user/role-management system.

For local Docker verification, copy .env.example to .env, set distinct random passwords (reviewer password at least 8 characters), and run docker compose up --build. Open http://127.0.0.1:8082. Local demo cookies are not Secure because this URL uses HTTP; Render enables Secure cookies over HTTPS.

Free Render databases expire after 30 days and web services may sleep after inactivity. Confirm current platform limits before sharing a review link. Keep credentials out of Git. No production-data migration is included.