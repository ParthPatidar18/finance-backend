# Finance Data Processing & Access Control Backend

A production-structured Spring Boot backend for the Zorvyn Finance Dashboard assignment.

---

## Tech Stack

| Layer        | Technology                  |
|--------------|-----------------------------|
| Framework    | Spring Boot 3.2             |
| Language     | Java 17                     |
| Security     | Spring Security + JWT (HS256) |
| Database     | H2 In-Memory (swap-ready for MySQL/PostgreSQL) |
| ORM          | Spring Data JPA / Hibernate |
| Validation   | Jakarta Bean Validation     |
| Build        | Maven                       |

---

## Project Structure

```
src/main/java/com/zorvyn/finance/
├── config/
│   ├── DataSeeder.java          # Seeds default users + sample records on startup
│   └── SecurityConfig.java      # Spring Security + JWT filter chain
├── controller/
│   ├── AuthController.java      # POST /api/auth/login, /register
│   ├── UserController.java      # GET/PUT/DELETE /api/users/**
│   ├── FinancialRecordController.java  # Full CRUD /api/records
│   └── DashboardController.java # GET /api/dashboard/summary
├── dto/                         # Request/Response objects (no entity exposure)
├── entity/
│   ├── User.java
│   └── FinancialRecord.java     # Includes soft-delete flag
├── enums/
│   ├── Role.java                # VIEWER | ANALYST | ADMIN
│   └── TransactionType.java     # INCOME | EXPENSE
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
├── repository/
│   ├── UserRepository.java
│   └── FinancialRecordRepository.java  # Custom JPQL for filters + aggregations
├── security/
│   ├── JwtUtils.java            # Token generation + validation
│   ├── JwtAuthFilter.java       # Per-request token extraction
│   └── UserDetailsServiceImpl.java
└── service/
    ├── AuthService.java
    ├── UserService.java
    ├── FinancialRecordService.java
    └── DashboardService.java    # All aggregation logic
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run

```bash
cd finance-backend
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

H2 Console (dev): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:financedb`
- Username: `sa` | Password: *(empty)*

### Run Tests

```bash
mvn test
```

---

## Default Users (seeded on startup)

| Username | Password    | Role    | Permissions                          |
|----------|-------------|---------|--------------------------------------|
| admin    | admin123    | ADMIN   | Full access                          |
| analyst  | analyst123  | ANALYST | Read records + dashboard             |
| viewer   | viewer123   | VIEWER  | Dashboard summary only               |

---

## API Reference

All endpoints (except `/api/auth/**`) require:
```
Authorization: Bearer <token>
```

### Auth

| Method | Endpoint            | Description           | Access  |
|--------|---------------------|-----------------------|---------|
| POST   | `/api/auth/login`   | Login, returns JWT    | Public  |
| POST   | `/api/auth/register`| Register new user     | Public  |

**Login Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Login Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJ...",
    "tokenType": "Bearer",
    "username": "admin",
    "email": "admin@zorvyn.com",
    "role": "ADMIN"
  }
}
```

---

### Financial Records

| Method | Endpoint           | Description                   | Access         |
|--------|--------------------|-------------------------------|----------------|
| GET    | `/api/records`     | List records (paginated)      | ADMIN, ANALYST |
| GET    | `/api/records/{id}`| Get single record             | ADMIN, ANALYST |
| POST   | `/api/records`     | Create record                 | ADMIN          |
| PUT    | `/api/records/{id}`| Update record                 | ADMIN          |
| DELETE | `/api/records/{id}`| Soft delete record            | ADMIN          |

**Query Parameters for GET `/api/records`:**

| Param      | Type   | Example            | Description              |
|------------|--------|--------------------|--------------------------|
| type       | enum   | `INCOME`/`EXPENSE` | Filter by type           |
| category   | string | `Salary`           | Partial match on category|
| startDate  | date   | `2026-01-01`       | ISO date (inclusive)     |
| endDate    | date   | `2026-04-30`       | ISO date (inclusive)     |
| page       | int    | `0`                | Page number (0-indexed)  |
| size       | int    | `10`               | Records per page (max 100)|
| sortBy     | string | `date`, `amount`   | Sort field               |
| sortDir    | string | `asc`/`desc`       | Sort direction           |

**Create Record Request:**
```json
{
  "amount": 50000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "notes": "Monthly salary"
}
```

---

### Dashboard

| Method | Endpoint                 | Description           | Access                    |
|--------|--------------------------|-----------------------|---------------------------|
| GET    | `/api/dashboard/summary` | Full dashboard data   | ADMIN, ANALYST, VIEWER    |

**Response includes:**
- `totalIncome` — sum of all INCOME records
- `totalExpenses` — sum of all EXPENSE records
- `netBalance` — income minus expenses
- `incomeByCategory` — map of category → total
- `expensesByCategory` — map of category → total
- `monthlyTrends` — year/month breakdown with income, expenses, net
- `recentActivity` — last 5 transactions

---

### Users (ADMIN only)

| Method | Endpoint          | Description              | Access |
|--------|-------------------|--------------------------|--------|
| GET    | `/api/users`      | List all users           | ADMIN  |
| GET    | `/api/users/me`   | Own profile              | ALL    |
| GET    | `/api/users/{id}` | Get user by ID           | ADMIN  |
| PUT    | `/api/users/{id}` | Update role/active status| ADMIN  |
| DELETE | `/api/users/{id}` | Deactivate user          | ADMIN  |

**Update User Request:**
```json
{
  "role": "ANALYST",
  "active": true
}
```

---

## Role & Access Control Matrix

| Action                        | VIEWER | ANALYST | ADMIN |
|-------------------------------|--------|---------|-------|
| View dashboard summary        | ✅     | ✅      | ✅    |
| View financial records        | ❌     | ✅      | ✅    |
| Create financial records      | ❌     | ❌      | ✅    |
| Update financial records      | ❌     | ❌      | ✅    |
| Delete financial records      | ❌     | ❌      | ✅    |
| View own profile              | ✅     | ✅      | ✅    |
| Manage users (CRUD)           | ❌     | ❌      | ✅    |

Access control is enforced at two levels:
1. **HTTP layer** — via `SecurityConfig` (`authorizeHttpRequests`)
2. **Response layer** — 403 Forbidden with descriptive error message

---

## Error Responses

All errors follow a consistent shape:

```json
{
  "success": false,
  "message": "Descriptive error message",
  "timestamp": "2026-04-06T10:00:00"
}
```

Validation errors additionally include a `data` map of field → message:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than 0",
    "category": "Category is required"
  }
}
```

| Scenario                    | Status Code |
|-----------------------------|-------------|
| Success                     | 200 / 201   |
| Validation failure          | 400         |
| Bad credentials             | 401         |
| Insufficient role           | 403         |
| Resource not found          | 404         |
| Duplicate username/email    | 409         |
| Server error                | 500         |

---

## Design Decisions & Assumptions

1. **Soft Delete** — Financial records are never hard-deleted. A `deleted` flag is set to `true`. This preserves historical data for audit trails and dashboard accuracy remains unaffected since all queries filter `deleted = false`.

2. **H2 In-Memory** — Used for zero-setup local development. Swapping to MySQL/PostgreSQL requires only changing `application.properties` (datasource URL, driver, dialect). No code changes needed.

3. **JWT Authentication** — Stateless, 24-hour expiry. No refresh token implemented (out of scope for this assignment). Token is validated per request via `JwtAuthFilter`.

4. **Role registration** — The `/api/auth/register` endpoint allows specifying any role including ADMIN. In a production system, self-registration would be restricted to VIEWER/ANALYST, with ADMIN creation gated behind an existing ADMIN session.

5. **Pagination capped at 100** — Prevents accidental large dumps. Default page size is 10.

6. **User deactivation over deletion** — `DELETE /api/users/{id}` sets `active = false` rather than removing the row, preserving referential integrity (records reference users via FK).

7. **Category as free text** — Not an enum/lookup table, allowing flexibility. Can be migrated to a separate `categories` table in a production schema.

---

## Switching to MySQL (Production)

Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/financedb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Add MySQL dependency to `pom.xml`:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```
