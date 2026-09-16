# BloodBuddy Backend (CPJ119 CPP401)

Spring MVC + Hibernate/JPA + MySQL backend. The frontend is served by the same
app from `src/main/resources/static/` — one origin, no CORS, `bb-api.js` needs
zero changes (`BASE = ''`).

## Run it (IntelliJ)

1. **File → Open…** → select this `backend/` folder → *Open as Project*.
   IntelliJ's bundled Maven imports `pom.xml` automatically (no Maven CLI needed).
2. **Database is TiDB Cloud** (MySQL-compatible, TLS required):
   - TiDB Cloud console → cluster → **Connect** → copy the **Host**, **Port** (`4000`), and **User** (looks like `xxxxx.root`).
   - In the console's **SQL editor** create the schema once (serverless tiers do not
     support `createDatabaseIfNotExist`):
     ```sql
     CREATE DATABASE IF NOT EXISTS bloodbuddy CHARACTER SET utf8mb4;
     ```
   - Hibernate (`ddl-auto=update`) then creates/updates all tables from the JPA
     entities on first boot.
3. **Set your credentials as environment variables** — Run → *Edit Configurations…*
   → *Environment variables* (click the 📄 icon for one-per-line editing):
   ```
   BB_DB_HOST=gateway01.<region>.prod.aws.tidbcloud.com
   BB_DB_PORT=4000
   BB_DB_USER=<youruser>.root
   BB_DB_PASSWORD=<your TiDB Cloud password>
   ```
   (No secrets ever go into git — `application.properties` only references env vars.)
4. Start MailHog (fake SMTP for §2.4 — no real emails in dev):
   ```
   docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
   ```
   No Docker? Skip it for now — email failures are logged and do not block
   registration; the inbox UI is at **http://localhost:8025** when it runs.
5. Run `BloodBuddyApplication` (green ▶). App: **http://localhost:8080/CODE/HTML/landing.html**

> Local XAMPP/MariaDB fallback: set `BB_DB_HOST=localhost`, `BB_DB_PORT=3306`,
> `BB_DB_SSLMODE=DISABLE`.

## Frontend sync

The static folder is a **copy**, not a link — after any frontend edit run:

```
python tools/sync_frontend_to_backend.py
```

## Configuration

Secrets come from environment variables (see `application.properties`):
`BB_DB_USER` / `BB_DB_PASSWORD`, `BB_SMTP_HOST` / `BB_SMTP_PORT` /
`BB_SMTP_USER` / `BB_SMTP_PASSWORD`, `BB_MAIL_FROM`. Defaults are local-dev
values (root / empty password / MailHog on 1025).

## Structure (CPJ119 §2.2 layering)

```
src/main/java/com/bloodbuddy/
├── BloodBuddyApplication.java
├── controller/   REST controllers — HTTP only, no business logic
├── service/      ALL critical logic: Appendix A compat chart, donor matching,
│                 status transitions, inventory decrement, email triggers
├── repository/   Spring Data JPA repositories (Hibernate, §2.3)
├── model/        JPA entities: USER, DONOR, BLOOD_REQUEST, HOSPITAL,
│                 BLOOD_INVENTORY, EMAIL_NOTIFICATION (proposal ERD)
└── dto/          request/response payloads (entities never leak to JSON)
```
