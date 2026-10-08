# CampusConnect — Campus Event Manager

A small campus event manager. The frontend uses plain JavaScript and Vite; the Java 25 backend uses the JDK HTTP server, JDBC, and MySQL.

## Project at a glance

- **Problem statement:** Campus event information is often scattered across messages and posters, making it difficult for students to discover events and for organizers to maintain accurate details.
- **Objective:** Build one simple web application to manage campus events and demonstrate how a browser UI, REST API, Java backend, and relational database work together.
- **Audience:** College students, faculty, and event organizers (demo data only; no login or access control is included).
- **Features:** Add, list, and remove events with a small responsive form. Without the backend, events are saved in browser storage.

## Technology stack: what, why, where

| Technology | What it does / why it is used | Where to find it / project example |
|---|---|---|
| HTML5 | Gives content meaning and structure; semantic elements improve accessibility. | `frontend/index.html` contains the page, event form, and event list. |
| CSS3 | Controls layout, colors, responsiveness, and small animations without a UI framework. | `frontend/src/styles.css` styles the cards, form, filters, and mobile layout. |
| JavaScript | Adds browser-side behavior, validation, events, and HTTP API calls. | `frontend/src/main.js` handles form submission and calls `fetch('/api/events')` through the configured API base URL. |
| Java 17 | Runs the REST API using the JDK's built-in HTTP server. | `backend/src/main/java/com/campusconnect/events/CampusConnectApplication.java` |
| JDBC / MySQL | Stores events; SQL values are passed with prepared statements. | `CampusConnectApplication.java` and `database/schema.sql` |
| Jackson | Reads and writes API JSON. | Declared in `backend/pom.xml`. |
| MySQL / SQL | Stores event data in a structured, persistent table. | `database/schema.sql` creates and seeds the `campus_connect` database. |
| Maven | Downloads Java dependencies and builds/runs the backend. | `backend/pom.xml` lists Jackson and MySQL dependencies. |
| Vite | Serves the frontend during development and builds static files. | `frontend/package.json` and `frontend/vite.config.js`. |

## System workflow

1. An organizer fills in the short event form. Browser JavaScript checks the required values and date.
2. If the API is available, `main.js` sends the event to the Java REST API.
3. The Java server validates the event and stores it in MySQL. If the API is unavailable, the browser stores it locally instead.

```text
HTML form + vanilla JS + dashboard
                 │ HTTP / JSON (fetch)
                 ▼
    JDK HTTP server
                 ▼
     JDBC / prepared SQL
                 │
                 ▼
              MySQL
```

## Database design

One table, `events`, is enough for this learning project:

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT, primary key, auto-increment | Unique event identifier |
| `title` | VARCHAR(120), not null | Event name |
| `description` | VARCHAR(1000) | Short event summary |
| `category` | VARCHAR(40), not null | Workshop, Seminar, Social, or Sports |
| `venue` | VARCHAR(120), not null | Campus location |
| `start_time` | DATETIME, not null | Event date and time |
| `created_at` | TIMESTAMP | Creation time, assigned by MySQL |

No foreign keys are needed because this version intentionally has no separate users, attendees, or categories tables.

## Folder structure

```text
.
├── README.md
├── docker-compose.yml              # Optional local MySQL service
├── database/
│   └── schema.sql                  # Table definition and demo rows
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/campusconnect/events/
│       ├── java/com/campusconnect/events/
│       │   ├── CampusConnectApplication.java
│       │   └── model/Event.java
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── styles.css
```

## Setup and run

### Requirements

- Java 17 or newer (only needed for the backend)
- Maven 3.8+
- Node.js 20+ and npm
- MySQL 8+ **or** Docker Desktop for the optional Compose database

### 1. Optional: start MySQL for shared API storage

**Option A — Docker (quickest):** from the project root run `docker compose up -d`. This starts MySQL on port `3306`, creates `campus_connect`, and loads `database/schema.sql` the first time the named volume is initialized.

**Option B — Existing MySQL:** execute `database/schema.sql` with your MySQL client. Set the connection environment variables below if needed.

Database defaults for local learning: database `campus_connect`, username `campus_user`, password `campus_pass`. Change these before exposing the app beyond your own machine.

### 2. Optional: start the backend

In a terminal, change to `backend` and run `mvn exec:java -Dexec.mainClass=com.campusconnect.events.CampusConnectApplication`. The REST API is available at `http://localhost:8080`; check `http://localhost:8080/api/events` in a browser.

Environment overrides: `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

### 3. Start the frontend

In a terminal, change to `frontend`, run `npm install`, then `npm run dev`. Open the local address printed by Vite (normally `http://localhost:5173`). The page works without the backend using browser storage; if the API is running, Vite forwards `/api` requests to port 8080 and events are stored in MySQL.

### 4. Try the CRUD flow

Add an event and remove it from the list. With the API running, rows are stored in MySQL. Without it, rows stay in browser storage on this device.

### Useful commands

- Frontend production build: `npm run build` inside `frontend`
- Backend build: `mvn clean package` inside `backend`
- Stop optional database: `docker compose down` (data remains in the volume)
- Reset optional demo database: `docker compose down -v` then `docker compose up -d` (deletes local demo data)

## API overview

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/events` | List all events, newest date first |
| GET | `/api/events/{id}` | Get one event |
| POST | `/api/events` | Create an event |
| PUT | `/api/events/{id}` | Replace an event's editable fields |
| DELETE | `/api/events/{id}` | Delete an event |

`POST`/`PUT` JSON example: `{"title":"Design Thinking Workshop","description":"Explore user-centered design.","category":"Workshop","venue":"Innovation Lab","startTime":"2027-03-18T14:30:00"}`. Validation errors return HTTP 400 with a readable message; unknown IDs return HTTP 404.

## Key code sections to explain

- **Browser validation and storage:** `frontend/src/main.js` checks the date, calls the API when available, and falls back to browser storage.
- **REST routes and validation:** `CampusConnectApplication.java` handles CRUD requests and rejects invalid or past-dated events.
- **SQL CRUD:** the same class demonstrates `INSERT`, `SELECT`, `UPDATE`, and `DELETE` with prepared statements.
- **Schema:** `database/schema.sql` declares the primary key, required fields, and sample rows.

## Viva questions and short answers

1. **What is a REST API?** A way for clients and servers to exchange resources over HTTP using methods such as GET, POST, PUT, and DELETE.
2. **Why use JSON?** It is a compact, readable format that JavaScript and Java can serialize and parse.
3. **What is CRUD?** Create, Read, Update, and Delete—the four basic data operations implemented by the event endpoints.
4. **What does JDBC do?** It allows Java code to connect to a relational database and execute SQL statements.
5. **Why use prepared statements?** They keep user input separate from SQL commands and reduce SQL injection risk.
6. **What is a primary key?** A column (here `id`) that uniquely identifies each row in a table.
7. **What is CORS?** A browser security rule for cross-origin requests. The backend allows the local Vite development origin for this demo.
8. **Why validate on both frontend and backend?** Browser validation improves usability; server validation is authoritative because clients can bypass browser checks.
9. **How does the dashboard update?** Vanilla JavaScript recalculates its counts whenever the event list is refreshed.
10. **How is SQL injection reduced?** SQL values are passed as `?` parameters through prepared statements, not concatenated into query strings.
11. **What happens when an event ID is missing?** The API responds with HTTP 404.
12. **What could be added next?** User sign-in, event registration, pagination, tests for more edge cases, and deployment with managed secrets.

## Short presentation script

“CampusConnect is a simple campus event manager. The page uses semantic HTML, responsive CSS, and vanilla JavaScript. A small Java 25 server handles the REST API and uses prepared JDBC statements to store events in MySQL. I can demonstrate CRUD by adding, editing, and deleting an event.”

## Notes for a classroom demo

- This is an educational local demo, not a production system. It has no authentication, authorization, CSRF protection, or deployment hardening.
- The Compose password is a local placeholder credential; replace it for real use and never commit real secrets.
- Use future event times because the service rejects past events.
