# AI Coding Agent Instructions

## Project Overview

**Project**: EV İşleri Takip (Family Operations Tracking System)  
**Purpose**: A family operations management web application for task tracking, inventory control, budget management, and automated shopping list generation  
**Tech Stack**: 
- Frontend: React 18 + TypeScript + Zustand
- Backend: Node.js + Express + TypeScript
- Database: PostgreSQL
- Email: Nodemailer (Gmail)
- Authentication: JWT + Email-based OTP

---

## Architecture & Components

### System Overview

The application follows a client-server architecture with clear separation of concerns:

1. **Authentication Layer** - Email-based OTP login (no passwords)
2. **Family Isolation** - All data scoped to `family_id` for privacy
3. **Task Management** - Supports multiple frequencies (daily, weekly, monthly, quarterly, biannual, yearly)
4. **Inventory System** - Tracks stock with automatic low-stock detection
5. **Shopping Lists** - Auto-generates from low stock, sends via email
6. **Budget Tracking** - Monitors family expenses by category

### Key Components

#### Backend Structure
- **Controllers** (`src/controllers/`): Business logic for auth, tasks, inventory, shopping, budget
- **Routes** (`src/routes/`): Express router definitions for all API endpoints
- **Middleware** (`src/middleware/auth.ts`): JWT verification and role-based access
- **Config** (`src/config/`): Database pool and email transporter setup
- **Models** (`src/models/database.sql`): Complete PostgreSQL schema with 10 core tables

#### Frontend Structure
- **Pages** (`src/pages/`): Full-page components (LoginPage, Dashboard - TBD)
- **Store** (`src/store/authStore.ts`): Zustand for global auth state + localStorage persistence
- **API** (`src/utils/api.ts`): Axios client with auto token injection and typed API methods
- **Components** (TBD): Reusable UI components for tasks, inventory, shopping lists

### Data Flow

```
User (React Frontend)
  ↓
API Client (axios with JWT)
  ↓
Express Routes + Auth Middleware
  ↓
Controllers (business logic)
  ↓
PostgreSQL Database
  ↓
Response JSON → Frontend State (Zustand)
```

### Critical Design Decisions

1. **Email-based Auth**: No password complexity - 6-digit OTP sent to email, valid for 15 mins
2. **Family ID Scoping**: Every query includes `WHERE family_id = $X` to prevent data leakage
3. **Frequency Model**: Tasks stored with frequency field + optional recurrence_data JSONB for future expansion
4. **Inventory Threshold**: Auto-shopping list generation when `quantity <= min_threshold`
5. **No authentication state in DB**: Login codes stored in temporary table (consider Redis for production)

---

## Development Workflows

### Build & Run

#### Backend
```bash
cd backend
npm install
npm run dev          # ts-node-dev with auto-restart
npm run build        # Compile TypeScript to dist/
npm start            # Run compiled dist/index.js
npm test             # Jest test suite
```

#### Frontend
```bash
cd frontend
npm install
npm start            # React dev server on :3000
npm run build        # Production build to build/
npm test             # Jest tests
```

### Setup Order
1. **PostgreSQL**: Create database `ev_isleri_takip`, run `backend/src/models/database.sql`
2. **Backend**: Copy `.env.example` → `.env`, configure email (Gmail requires app password)
3. **Frontend**: Set `REACT_APP_API_URL=http://localhost:5000/api` if needed
4. **Run**: Backend first (needs DB), then Frontend

### Common Tasks

- **Add new endpoint**: 
  1. Create controller function in `backend/src/controllers/`
  2. Add router in `backend/src/routes/`
  3. Add API method to `frontend/src/utils/api.ts`
  
- **Add to database**: 
  1. Update `backend/src/models/database.sql`
  2. Add controller methods for CRUD operations
  3. Ensure `family_id` is included in all WHERE clauses

- **Email functionality**: 
  1. Uses Nodemailer with Gmail
  2. Requires `EMAIL_USER` and `EMAIL_PASSWORD` (Gmail app password, not account password)
  3. Email templates inline HTML in controllers

---

## Project Conventions

### Code Style

- **Naming**: 
  - Controllers: Verb-noun format (`createTask`, `getInventory`)
  - Tables: Plural lowercase (`users`, `shopping_lists`)
  - Columns: Snake_case in DB, camelCase in API responses
  
- **File Organization**:
  - Backend: MVC pattern (Model=DB schema, View=API response, Controller=logic)
  - Frontend: Feature-based folders under pages/components
  - Shared types in separate `types/` folder (TBD)

- **Imports**: 
  - Backend: relative paths, group by type (external, config, local)
  - Frontend: absolute paths with `src/` prefix (via tsconfig)

### Patterns & Practices

- **Error Handling**: 
  - Backend: Try-catch blocks, return 500 with error message
  - Frontend: Catch API errors, display user-friendly messages
  - Specific error codes (400 for validation, 401 for auth, 404 for not found, 500 for server)

- **Validation**: 
  - Frontend: Basic form validation before sending
  - Backend: Detailed validation (email format, required fields, permission checks)
  - All mutations check `family_id` ownership

- **Database Patterns**:
  - Always use parameterized queries (`$1, $2`) to prevent SQL injection
  - Soft deletes not implemented; consider adding `deleted_at` for audit trail
  - Foreign key constraints enforced; cascading deletes for child records

### Async/Await
- Both backend (Express) and frontend (React hooks) use async/await consistently
- No callback hell or Promise chaining

---

## Critical Integration Points

### External Dependencies

1. **Nodemailer** (Backend): Email sending via Gmail SMTP
   - Requires app-specific password (not account password)
   - Templates currently inline HTML; consider template files
   
2. **PostgreSQL** (Backend): Main data store
   - Connection pooling via `pg` package
   - Indexes on frequently queried columns (family_id, email, dates)

3. **JWT** (Backend): Stateless session management
   - Signed with `JWT_SECRET` (15+ char recommended)
   - Expires after `JWT_EXPIRE` (default 7 days)

4. **Zustand** (Frontend): Global state without Redux boilerplate
   - Single auth store with localStorage persistence
   - Used for token, userId, familyId

### Configuration

All config via environment variables (`.env` file):
- `NODE_ENV`: development/production
- `PORT`: Server port (default 5000)
- `DATABASE_URL`: PostgreSQL connection string
- `JWT_SECRET`: Token signing key
- `EMAIL_SERVICE`, `EMAIL_USER`, `EMAIL_PASSWORD`: Gmail credentials
- `FRONTEND_URL`: For email links (used in invite/login code emails)
- `REACT_APP_API_URL`: Frontend's API base URL

### Data Persistence

- **Primary**: PostgreSQL on `DATABASE_URL`
- **Session**: JWT tokens (stateless)
- **Client State**: LocalStorage (auth token, userId, familyId)
- **Temporary**: Login codes in `login_codes` table (15-min expiry)

---

## Important Notes for AI Agents

### Before Making Changes
1. **Check family_id scoping**: Every SELECT/UPDATE/DELETE must filter by `family_id` from JWT
2. **Review existing patterns**: Look at similar controllers (e.g., `tasks.ts` before adding new endpoint)
3. **Database locks**: Large dataset operations may need pagination (not implemented yet)

### When Adding Features
- **New Task Frequency**: Update `database.sql` task schema, then controller logic
- **New Inventory Feature**: Add column to `inventory` table, backfill existing records
- **Email Notifications**: Use `sendEmail()` from `config/email.ts`
- **New API Endpoint**: Must have auth middleware, family ID validation, proper error handling

### Testing
- **Backend**: Unit tests for controllers (Jest), integration tests for API
- **Frontend**: Component tests, API mock testing (MSW recommended)
- **Database**: Test data setup script needed (currently manual)

### Common Gotchas
1. **Email sending fails**: Gmail requires app-specific passwords, not account password
2. **CORS issues**: Frontend CORS middleware must include frontend URL
3. **TokenExpiry**: JWT expires after 7d; implement refresh token mechanism (TBD)
4. **Date handling**: Use `date-fns` for date operations (already installed frontend)
5. **SQL Injection**: ALWAYS use parameterized queries; never string interpolation

---

## Key Files by Feature

| Feature | Files |
|---------|-------|
| Email Auth | `controllers/auth.ts`, `routes/auth.ts`, `pages/LoginPage.tsx` |
| Task Management | `controllers/tasks.ts`, `routes/tasks.ts` |
| Inventory | `controllers/inventory.ts`, `routes/inventory.ts` |
| Shopping Lists | `controllers/shopping.ts`, `routes/shopping.ts` |
| Budget Tracking | `controllers/budget.ts`, `routes/budget.ts` |
| Database | `models/database.sql` |
| API Client | `frontend/src/utils/api.ts` |
| State Management | `frontend/src/store/authStore.ts` |

---

## References

- [Backend README](../README.md) - Project overview and API documentation
- [Database Schema](../backend/src/models/database.sql) - Complete DB structure with indexes
- [Environment Example](../backend/.env.example) - Configuration template
- [Express Router Setup](../backend/src/index.ts) - Main server initialization

---

## Last Updated
January 25, 2026

*This file documents current implementation patterns. Update as architecture evolves.*
