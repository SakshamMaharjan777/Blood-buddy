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
4. Start the mail catcher (§2.4 dev inbox — **Mailpit**, already downloaded to
   `.tools/mailpit/`, gitignored, no install and no Docker needed):
   ```
   .tools/mailpit/mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
   ```
   Read captured mail at **http://127.0.0.1:8025**. Or skip this step: mail
   failures are recorded as `FAILED` on the notification row and logged, and
   **nothing else breaks** — registration and every business rule still succeed.
5. Run `BloodBuddyApplication` (green ▶). App: **http://localhost:8081/CODE/HTML/landing.html**

> **Why 8081, not 8080?** On the development machine port 8080 is held by the
> EDB/PostgreSQL PEM Apache service (`PEMHTTPD-x64`), which serves its own 404
> page — the app's Tomcat then fails with *"Port 8080 was already in use"* after
> Hibernate has already created the tables, so it looks like a routing bug when
> it is only a port clash. Set `BB_SERVER_PORT=8080` once that service is gone,
> or `server.port` in `application.properties` to any free port.

> Local XAMPP/MariaDB fallback: set `BB_DB_HOST=localhost`, `BB_DB_PORT=3306`,
> `BB_DB_SSLMODE=DISABLE`.

## Frontend sync

The static folder is a **copy**, not a link — after any frontend edit run:

```
python tools/sync_frontend_to_backend.py
```

## The frontend on the real backend (B7)

Since B7 the pages talk to this API by default (`MOCK = false` in `bb-api.js`);
the old localStorage demo store is reachable only through the documented override
`localStorage.bb_force_mock = '1'`, which is what the mock QA suite uses. The
session is the `JSESSIONID` cookie — `nav.js` reconciles its cached `bb_session`
with `GET /api/auth/me` on every page load, and Log out calls
`POST /api/auth/logout`.

With the app running, the end-to-end frontend contract is checkable in one page:

```
python tools/sync_frontend_to_backend.py
open  http://localhost:8081/resptest/real-backend-check.html   → 39/39
```

It loads the real pages with the demo store **pinned empty**, so any rendered row
must have arrived over HTTP, and asserts the 401/403 boundary, the session, logout,
the donor write path, the Donor ↔ Hospital affiliation read (`GET
/api/donors/{id}/hospitals`) and two registrations — a donor and a hospital-staff
account that must come back linked to the hospital it named. It prints the SQL to
undo every row it creates.
The mock suite is unaffected and still runs offline:
`resptest/qa-harness.html` → **268/268**.

## REST API (B3)

The Appendix B endpoints are live. Groups: `/api/auth/*` (register, login, forgot,
me), `/api/donors/*` (search, one donor, compatible set, `{id}/photo` upload +
retrieval, `{id}/hospitals` — the affiliation read), `/api/requests*`
(list/get/lookup/create/patch), `/api/hospitals/*`
(inventory board + stock set), `/api/admin/users*`, `/api/profile/{role}` and
`/api/contact`.

Error contract: **404** unknown row, **409** business-rule refusal (the message is
the rule's own sentence), **400** validation/bad labels/malformed JSON. See
`web/ApiExceptionHandler`.

The API is **session-authenticated** since B6 — see "Security (B6)" below.

Verify the whole surface over HTTP against a running app:

```
python tools/api_check.py            # 94 checks, incl. the RBAC and rule refusals
```

(It logs in as the seeded accounts first, and prints cleanup SQL — it creates a
guest request, a member request and a couple of accounts, and nothing can undo
those through the API.)

The **Postman deliverable** (§2.6) is `docs/postman/BloodBuddy.postman_collection.json`
— 60 requests in run order, covering every endpoint here (including the
Appendix B hospital-scoped routes and the additive `?page=&size=` pagination)
plus the rule refusals.
Import it, then Collection Runner. Its login request captures `JSESSIONID` from
the cookie jar and stores `userId`/`donorId`/`hospitalId` as collection variables,
so no request hardcodes a row id.

## Security (B6)

Seeded demo accounts (all with password `BloodBuddy#2026`): `arun.s@example.com`
(donor), `sita.g@example.com` (requester), `bloodbank@tuth.edu.np` (hospital,
linked to TUTH), `saksham@bloodbuddy.np` (admin).

The role split the frontend enforces in `nav.js` is now enforced server-side
(`config/SecurityConfig`, plus `@PreAuthorize` on the role-restricted controllers):

| Who | What |
|---|---|
| public | the static site, `POST /api/auth/{register,login,forgot}`, `POST /api/contact`, `POST /api/requests` (the guest emergency form), `GET /api/requests/lookup/*` (BR-8) |
| ADMIN | `/api/admin/**` |
| HOSPITAL or ADMIN | `/api/hospitals/**` |
| any signed-in account | `/api/donors/**`, `/api/requests`, `/api/profile/**`, `/api/auth/me` |

**Sign in** with `POST /api/auth/login` (JSON) — credentials are verified by
`AuthService` (BCrypt) and `web/SessionAuthenticator` then opens a real session, so
the response sets a `JSESSIONID` cookie and every later request is identified by it.
`POST /api/auth/logout` ends it. Anonymous API calls answer **401** and wrong-role
calls **403**, both as the same `ApiError` JSON body the controllers use.

Two deliberate choices, both stated in `SecurityConfig`'s javadoc rather than left
implicit: **CSRF is disabled** (the frontend's `fetch` layer has no token plumbing,
and adding it is a frontend change, so it is a decision to make on purpose); and
**BR-5 is not a URL rule** ("a hospital may only touch its own stock" is a
comparison between the session's hospital and the row's, so it stays in the service
layer — the controller just refuses to take a hospital id from the caller when the
session has one).

Known limitation, recorded because it is real: suspending an account blocks the
NEXT login but does not kill a session that is already open (that needs a
`SessionRegistry` plus `HttpSessionEventPublisher`). It is asserted as such in
`tools/api_check.py` so it cannot be forgotten.

## Email notifications (§2.4, B4)

Every notification is recorded in `email_notifications` **before** it is sent and
the row is stamped after, so the table is the evidence trail: `SENT` with
`sentAt`, or `FAILED` with the server's own error. The plain-text body that went
out is stored on the row too, so what was sent can be read without a mail client.

`service/NotificationService` decides WHO and WHEN (registration confirmation,
compatible-donor fan-out, requester updates); `service/NotificationMailer` builds
and sends the message. **A mail failure never fails the business operation** — the
user's action succeeded and the `FAILED` row plus a log warning is the honest
record of what did not.

Which server it talks to is configuration only (see the table in
`application.properties`), so the dev inbox and the real §2.4 demo are one code
path:

| | Dev (default) | Real delivery (Gmail) |
|---|---|---|
| `BB_SMTP_HOST` | `localhost` | `smtp.gmail.com` |
| `BB_SMTP_PORT` | `1025` | `587` |
| `BB_SMTP_USER` / `BB_SMTP_PASSWORD` | *(empty)* | the Gmail address / a 16-char **app password** |
| `BB_SMTP_AUTH` / `BB_SMTP_STARTTLS` | `false` / `false` | `true` / `true` |
| `BB_MAIL_FROM` | `no-reply@bloodbuddy.local` | **the same Gmail address** (Gmail rewrites anything else) |

Gmail needs 2-Step Verification before it will issue an app password. Any other
SMTP relay works through the same five variables.

## Service smoke run (B2, B4)

The service layer can verify itself against the live database on boot — the
checks a compiler cannot make (transaction boundaries, the BR-3 row lock, lazy
loading with `open-in-view=false`, FK ordering, `ddl-auto` column additions, and
since B4 that the §2.4 mail actually leaves the app).

It is **off by default**, logs one PASS/FAIL line per check, and undoes every row
it created:

```
BLOODBUDDY_SMOKE=true SPRING_JPA_SHOW_SQL=false \
  "…/maven3/bin/mvn.cmd" -DskipTests compile spring-boot:run
```

Add `BLOODBUDDY_SMOKE_EXIT=true` for **headless mode**: the app shuts itself down
when the run finishes and its **exit code is the result** (0 = every check
passed), which is what turns the run into a one-command verification instead of a
process someone has to watch and kill.

Last run: **31 checks, 0 failed** with the catcher running (exit 0), and the demo
data verified back at 21 users / 14 donors / 4 requests / 3 notifications / 40
inventory rows. Run the same command with the catcher **stopped** and exactly two
checks fail — the mail-arrival ones — while registration and every business rule
still pass: that is the "an SMTP outage is recorded, never fatal" contract, and it
is verified by running it both ways rather than assumed.

## Configuration

Secrets come from environment variables (see `application.properties`):
`BB_DB_USER` / `BB_DB_PASSWORD`, `BB_SMTP_HOST` / `BB_SMTP_PORT` /
`BB_SMTP_USER` / `BB_SMTP_PASSWORD` / `BB_SMTP_AUTH` / `BB_SMTP_STARTTLS`,
`BB_MAIL_FROM`, and `BB_SERVER_PORT` (default **8081** — 8080 is taken by the
EDB PEM Apache service here). Defaults are local-dev values (root / empty
password / Mailpit on 1025).

Feature switches in `application.properties`: `bloodbuddy.seed` (demo data on
first boot, default `true`), `bloodbuddy.smoke` (the service smoke run, default
`false`) and `bloodbuddy.smoke.exit` (headless mode, default `false`).

## Structure (CPJ119 §2.2 layering)

```
src/main/java/com/bloodbuddy/
├── BloodBuddyApplication.java
├── web/          THE CONTROLLER LAYER (§2.2, B3 DONE): AuthController,
│                 DonorController, RequestController, HospitalController,
│                 AdminController, ProfileController, ContactController — HTTP
│                 only, no business logic — plus ApiExceptionHandler
│                 (404/409/400), ApiError, and ActorContextArgumentResolver
│                 (reads the session principal) + AuthenticatedAccount /
│                 SessionAuthenticator (B6 sign-in)
├── service/      ALL critical logic (B2, DONE): Appendix A compat chart,
│                 donor matching, status transitions, inventory decrement,
│                 email triggers. AuthService, DonorService, RequestService,
│                 InventoryService, AdminService, ProfileService,
│                 NotificationService (WHO/WHEN + the SENT/FAILED audit trail) and
│                 NotificationMailer (the SMTP edge, B4) + BusinessException /
│                 NotFoundException / ActorContext / PhotoCodec
├── repository/   Spring Data JPA repositories (Hibernate, §2.3)
├── model/        JPA entities: USER, DONOR, BLOOD_REQUEST, HOSPITAL,
│                 BLOOD_INVENTORY, EMAIL_NOTIFICATION (proposal ERD) — plus the
│                 Donor ↔ Hospital M:N (`donor_hospital_affiliation` join table,
│                 CPJ119 §2.3; a donor registers with the partner hospitals in
│                 their district). `HospitalResolver` turns a submitted hospital
│                 name into the staffed-hospital link (BR-5) for registration and
│                 the request flow alike
├── dto/          request/response payloads (B2, DONE) — entities never leak to JSON
└── config/       seeder (demo data), password encoder bean, smoke runner
```

Every dependency in the pom maps to a numbered requirement: web (§2.2), data-jpa
(§2.3), mail (§2.4), security (§3 RBAC), validation (§4, both tiers).

Business rules are implemented once, in the service layer: BR-1 (Appendix A
compatibility, recipient → donor direction — `DonorService.compatible`), BR-2
(legal transitions, `RequestService.requireTransition`), BR-3 (one donor per
request, enforced under a pessimistic row lock), BR-4 (fulfilment deducts stock,
never below zero), BR-5 (a hospital touches only its own queue), BR-7 (a decline
hides a request for one donor only), BR-8 (the public lookup sees `EM-` guests
only), BR-9 (BCrypt, no plaintext credentials).
