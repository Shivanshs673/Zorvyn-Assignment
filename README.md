<<<<<<< HEAD
# Zorvyn-Assignment
=======
# Finance Data Processing and Access Control Backend

Small Kotlin + Spring Boot backend for the placement assignment.

The project uses PostgreSQL (Supabase-compatible) with Spring Data JPA for persistence.

## What is included

- User management with roles and active/inactive status.
- Role-based access control using bearer token authentication.
- Financial record CRUD with filtering and soft delete.
- Dashboard summary API with totals, category breakdowns, monthly trends, and recent activity.
- PostgreSQL persistence through JPA entities and repositories.
- Basic rate limiting for API traffic.
- Built-in API docs endpoint (`GET /api/docs`).
- Validation and structured error responses.
- Basic integration tests.

## Assumptions

- Simple practical authentication is implemented using a login endpoint and bearer token.
- Three seed users are created on startup:
  - Admin: `11111111-1111-1111-1111-111111111111`
  - Analyst: `22222222-2222-2222-2222-222222222222`
  - Viewer: `33333333-3333-3333-3333-333333333333`
- Data is persisted in PostgreSQL.
- Admin users can manage users and records. Viewer and analyst users can read records and dashboard summaries.

## Run it

Set environment variables first:

```bash
export DB_URL="jdbc:postgresql://db.lgralscnpifjtwxestna.supabase.co:5432/postgres?sslmode=require"
export DB_USERNAME="postgres"
export DB_PASSWORD="YOUR_DB_PASSWORD"
```

```bash
./gradlew bootRun
```

If port `8080` is busy, run on another port:

```bash
SERVER_PORT=8081 ./gradlew bootRun
```

## Test it

```bash
./gradlew test
```

Tests run with H2 using the `test` profile.

## Basic UI for quick testing

A simple dashboard UI is available at:

- `http://localhost:8080/`

If you run on another port, update the URL accordingly (for example `http://localhost:8081/`).

What you can do from UI:

- login using role credentials (admin/analyst/viewer)
- inspect current session user
- create financial records (admin)
- quick-fill sample record data with one click
- load records with filters
- view dashboard summary metrics
- inspect raw API JSON responses

## Quick Testing Flow (Interview Demo)

1. Open UI and login as admin:
  - URL: `http://127.0.0.1:8081/` (or your running port)
  - Username/password: `admin/admin123`

2. Click **Who Am I** and verify role is `ADMIN`.

3. Create one `INCOME` record and one `EXPENSE` record.

4. Click **Load Summary** and show:
  - total income
  - total expense
  - net balance

5. Click **Load Records** and verify records table data.

6. Switch login to viewer (`viewer/viewer123`) and try creating a record:
  - Expect forbidden response (role restriction works).

7. Try invalid input (negative amount) as admin:
  - Expect validation error response.

## API Overview

### Authentication

- `POST /api/auth/login`

Sample body:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Use returned token in header:

```http
Authorization: Bearer <accessToken>
```

Demo credentials:

- `admin / admin123`
- `analyst / analyst123`
- `viewer / viewer123`

### API Docs

- `GET /api/docs`

Returns authentication and endpoint reference in JSON format.

### Users

- `POST /api/users` create a user
- `GET /api/users` list users
- `GET /api/users/me` return the current authenticated user
- `GET /api/users/{id}` get a user
- `PUT /api/users/{id}` update name, role, or active status

### Records

- `POST /api/records` create a financial record
- `GET /api/records` list records with optional filters
- `GET /api/records/{id}` get a single record
- `PUT /api/records/{id}` update a record
- `DELETE /api/records/{id}` soft delete a record

Supported filters on `GET /api/records`:

- `dateFrom`
- `dateTo`
- `category`
- `type`
- `search`
- `page`
- `size`

### Dashboard

- `GET /api/dashboard/summary?months=6`

Returns:

- total income
- total expense
- net balance
- category totals
- monthly trends
- recent activity

## Error format

Validation and business errors return a structured JSON body with status, message, path, and optional details.

## Example request header

```http
Authorization: Bearer <accessToken>
```

## Assignment Coverage (Simple)

- User and role management: done
- Financial records CRUD + filtering: done
- Dashboard summary APIs: done
- Role-based access control: done
- Validation and error handling: done
- Data persistence (PostgreSQL): done
- Integration tests: done
- README documentation: done

## Optional Enhancements Status

- Authentication using tokens or sessions: done
- Pagination for record listing: done
- Search support: done
- Soft delete functionality: done
- Rate limiting: done
- Unit tests or integration tests: done
- API documentation: done (`/api/docs` + README)
>>>>>>> a97e1e7 (Initial commit: Complete Kotlin Spring Boot finance app with auth, role-based access, rate limiting, and test UI)
