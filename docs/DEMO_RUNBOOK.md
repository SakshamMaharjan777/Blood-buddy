# BloodBuddy — Demo Runbook (CPP401 defence)

**Format assumed:** ~15 minutes, live in front of the evaluator, with the repo and
generated artifacts handed over afterwards. There is a **Q&A cheat-sheet** at the
end — the evaluator said they may ask a few questions, and that section is where
you win the marks.

Anything marked **[tool-verified]** has a command in this repo that proves it and
prints a pass count. Anything marked **[rehearse]** works in the design but has not
been exercised end-to-end through the browser against the live database — click it
once yourself before you rely on it in front of anyone.

---

## 0. Before you start (T-10 minutes)

Open **two terminals** and one browser window with tabs ready.

**Terminal 1 — the mail catcher (§2.4).** It must be running or every
registration writes a `FAILED` notification row instead of a real email:

```bash
.tools/mailpit/mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
```

**Terminal 2 — the app.** In IntelliJ: open `backend/` and press **▶** on
`BloodBuddyApplication` (the `BB_DB_*` env vars are already in the run config).
Or from this terminal, which is the same thing:

```bash
cd backend
eval "$(python - <<'PY'
import re
s = open('.idea/workspace.xml', encoding='utf-8').read()
def v(n):
    m = re.search(r'<env name="\s*%s\s*" value="([^"]*)"' % n, s); return m.group(1).strip()
for k in ('BB_DB_HOST','BB_DB_PORT','BB_DB_USER','BB_DB_PASSWORD'):
    print("export %s='%s'" % (k, v(k)))
print("export BB_SERVER_PORT=8081")
PY
)"
"/c/Program Files/JetBrains/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3/bin/mvn.cmd" -DskipTests compile spring-boot:run
```

**Browser tabs to pre-open** (never make the evaluator watch you type a URL):

| Tab | URL |
|---|---|
| The app | `http://localhost:8081/CODE/HTML/landing.html` |
| Mailpit inbox | `http://127.0.0.1:8025` |
| Frontend evidence | `http://localhost:8081/resptest/real-backend-check.html` |
| Backend evidence | `http://localhost:8081/resptest/qa-harness.html` (runs offline in mock mode) |

**Seeded accounts** — all with password `BloodBuddy#2026`:

| Role | Email |
|---|---|
| Donor (B+, available) | `arun.s@example.com` |
| Requester | `sita.g@example.com` |
| Hospital staff (linked to TUTH) | `bloodbank@tuth.edu.np` |
| Administrator | `saksham@bloodbuddy.np` |

**If you rehearsed**, put the database back first (recipe in §5). The known-good
state is **21 users / 14 donors / 5 hospitals / 40 inventory rows / 4 requests /
3 notifications / 7 timeline rows / 39 donor↔hospital affiliation rows**,
TUTH B+ = 12.

> **Note before the first run:** `backend/src/main/resources/static/` is a
> *generated copy* of the frontend. If you changed any page since the last run:
> `python tools/sync_frontend_to_backend.py`.

---

## 1. The 15-minute script

### 0:00 — Landing page (1 min)
- The problem in one sentence: Nepal coordinates blood through Facebook posts; this is the searchable registry.
- **Drag the window narrow**, or open DevTools' device toolbar at 360px: the layout reflows. Responsive is a **mandatory** criterion — show it, don't describe it.
- Point at **"Find blood now"** → the public emergency page exists for people with no account. Say that out loud; it is a deliberate design choice (guests are not forced to register in an emergency).
  **[tool-verified]** `resptest/responsive-check.html` → 92/92 page×width combinations.

### 1:00 — Register a donor (3 min) — *this is the §2.4 money shot*
1. **Register** → pick *Blood Donor* → step 2 → fill it in (try submitting with a bad email first: **inline field error, no alert box** — client-side validation, §2.1/§4).
2. Complete it. The success screen appears **only because the server accepted the POST**.
3. **Switch to the Mailpit tab and refresh**: `Welcome to BloodBuddy — confirmation for <name>` is sitting there. That is the §2.4 mandatory notification.
4. Say the line that matters: *"That account is a row in TiDB, and that email is a real SMTP delivery — not a toast that pretends."*
5. If they ask to see the record: the admin portal's user table shows the new account (or show the DB — §4 has the query).

  **[tool-verified]** `resptest/real-backend-check.html`: register → `201` + donor row created.

### 4:00 — Requester flow (3 min)
Log out (top-right) and log in as **`sita.g@example.com`**.
1. **Find Donors** → the grid is **real donor rows from the database**. Change blood group / district and re-search: the list changes. Search a group with nobody in that district → the **widen fallback** explains itself instead of showing "0 results".
2. Show **pagination** at the bottom (page size 5/10/20).
3. **My Requests** → submit a request (or point at a seeded one) → it appears in the tracker with a status pill and a timeline.
4. Point out that the timeline entry says *"compatible donors notified"* — and that those alerts are in the Mailpit inbox as `Urgent B+ request in Kathmandu`.

  **[tool-verified]** donor search 6 cards, tracking 1 card, over HTTP with localStorage pinned empty.

### 7:00 — Donor side of the lifecycle (2.5 min)
Log out, log in as **`arun.s@example.com`**.
1. **Donor Portal** → *Requests near you* lists only requests Arun may actually serve.
2. **Accept** one. The card disappears, the record changes, and the requester gets an email.
3. Say the BR-1 point: *"Appendix A is written recipient → donor, so a B+ donor may serve B+/AB+ but must never be offered an A+ patient. Reading that chart backwards is the classic bug; the chart lives in one place in the service layer, and the server re-checks it on accept."*

  **[tool-verified]** submitted → accepted → **re-read from the server** shows `Matched` with the donor and match time persisted.

### 9:30 — Hospital portal (2 min)
Log out, log in as **`bloodbank@tuth.edu.np`**.
1. **Inventory** — the 8 blood groups with live stock levels.
2. Adjust stock (it writes through the API).
3. **Requests** — the TUTH queue. A matched request shows **Mark fulfilled**; fulfilling it **deducts the units from stock** (BR-4) and closes the request (BR-2).
4. Mention BR-5: a hospital can only touch its own queue — enforced in the service layer, and the controller refuses a caller-supplied hospital id when the session already has one.

  **[rehearse]** fulfilling and the stock adjust write path (the reads are verified; the decrement is verified at the service layer by the smoke run).

### 11:30 — Admin portal (2 min)
Log out, log in as **`saksham@bloodbuddy.np`**.
1. **Dashboard**: KPIs, charts, and the **users table** (real accounts, paginated, searchable).
2. **Suspend** a user → their next login is refused with the reason.
3. **Moderation** (`Moderation` in the nav): the request queue, filter by hospital, forward an unrouted request to a hospital.
4. Note the **"Reset demo data" button is hidden** — deliberate, see Q&A.

  **[rehearse]** suspend / restore / forward (the users list itself is tool-verified; the service rules are covered by the smoke run).

### 13:30 — Role guardrails (1 min)
1. As the admin, type `donor-dashboard.html` in the URL → bounced to your own dashboard (client-side UX guard).
2. Then the honest bit: *"That guard is UX. The real gate is server-side: an anonymous API call answers 401, a wrong role answers 403 — and Spring Security enforces it on every `/api/**` route, not the page."*

  **[tool-verified]** anonymous `GET /api/donors/search` → **401**; a requester on `/api/admin/users` → **403**.

### 14:30 — The tools (60 seconds, high marks per second)
Open the **Postman collection** (`docs/postman/BloodBuddy.postman_collection.json`) and click **Run collection**: folder `00` proves the 401 boundary while logged out, `01` logs in *and asserts the `JSESSIONID` capture in its Tests tab*, and `99` logs out then shows `/api/auth/me` answering 401. That is §2.6 in one click.

Then run these two live; they print their own verdicts:

```bash
# frontend against the real backend (app must be running)
open  http://localhost:8081/resptest/real-backend-check.html   # REAL-BACKEND-DONE 39/39

# backend service layer against the live database, then it exits itself
cd backend
BLOODBUDDY_SMOKE=true BLOODBUDDY_SMOKE_EXIT=true SPRING_JPA_SHOW_SQL=false \
  ".../maven3/bin/mvn.cmd" -DskipTests compile spring-boot:run   # smoke run: 31 checks, 0 failed
```

---

## 2. Artifacts to hand over

| What | Where | State |
|---|---|---|
| Written report | `docs/BloodBuddy_Final_Report_v2.docx` / `.pdf` | present (the 39-page version with the cover logo + 4 diagrams); still needs its **9 screenshot placeholders** filled |
| Proposal | `docs/BloodBuddy_Proposal_Revised.docx` | ready |
| Postman collection (§2.6) | `docs/postman/BloodBuddy.postman_collection.json` | ready — 60 requests; import it and run the folders in order |
| Frontend evidence | `resptest/qa-harness.html` → 268/268 · `responsive-check.html` → 92/92 | re-runnable |
| Backend evidence | `resptest/real-backend-check.html` → 39/39 · `tools/api_check.py` → 94/94 · smoke run → 31/31 | re-runnable |
| Email evidence (§2.4) | Mailpit inbox at `127.0.0.1:8025` (53 captured messages, including the welcome mail) | **screenshot it** — the inbox is not part of the repo |
| Session log | `PROGRESS.md` — every decision, bug found and why | ready |

---

## 3. Numbers you can quote

| Evidence | Result |
|---|---|
| Frontend QA harness (22 pages + interactions) | **268 / 268** |
| Responsive check (23 pages × 4 widths) | **92 / 92** |
| Frontend against the real backend | **39 / 39** |
| Service-layer smoke run (BR-1 … BR-9, live TiDB) | **31 / 31** |
| REST API over HTTP (`tools/api_check.py`) | **94 / 94** |
| Data model | 6 entities + 9 tables — incl. the Donor ↔ Hospital **M:N** join table (`donor_hospital_affiliation`, 39 rows) |
| Seeded demo data | 21 users · 14 donors · 5 hospitals · 40 inventory rows · 4 requests |

---

## 4. If they ask — answers ready

**"Where does the business logic live?"**
In the service layer, per §2.2 — `DonorService` (the Appendix A chart and matching), `RequestService` (the lifecycle and every rule), `InventoryService`, `AuthService`, `AdminService`, `ProfileService`, `NotificationService`. Controllers are HTTP-only; the pages only present. The frontend's own validation and role guard are UX, and I can point to the server-side counterpart for each.

**"How do I know this isn't dummy data?"**
The frontend check loads the pages with the browser's demo store **pinned to empty arrays**, so a rendered row cannot have come from localStorage — and then it writes through the API and **re-reads the record from the server** to show the change was stored. The data is in TiDB Cloud, which the app reached over TLS.

**"What's still frontend-only?"**
Client-side validation (the server validates every field again), the RBAC page guard (the server returns 401/403), and pagination (currently slicing a full response — `?page=&size=` is the next step).

**"Why is CSRF disabled?"** *(a real question for a Spring Security project)*
Because the frontend's `fetch` layer has no token plumbing, so enabling it would break every write until `bb-api.js` reads the `XSRF-TOKEN` cookie and echoes the header. It's written down as a decision in `SecurityConfig`'s javadoc rather than left implicit — and it's the first thing I'd fix before any real deployment.

**"Why does it run on 8081?"**
Port 8080 on this machine is held by the EDB/PostgreSQL PEM Apache service, which answers with its own page — the app's Tomcat then failed with "port already in use" *after* Hibernate had created the tables, which looks like a routing bug. It's one env var (`BB_SERVER_PORT`).

**"Database — why TiDB Cloud, not local MySQL?"**
MySQL-compatible serverless with TLS; the app is driver-compatible with a local XAMPP/MariaDB install too (`BB_DB_PORT=3306`, `BB_DB_SSLMODE=DISABLE`). The schema is created by Hibernate from the JPA entities.

**"Prove §2.4."**
Three things: the mail in the Mailpit inbox, the `email_notifications` row marked **SENT** with `sentAt`, and the **body stored on the row** so what was sent is readable from the database. And I can show the failure path: with the mail server stopped, the same suite fails **exactly the two delivery checks** and the app keeps working — an outage is recorded, never fatal.

**"Could you send real email?"**
Yes — four environment variables (`BB_SMTP_HOST/PORT/USER/PASSWORD` + auth/TLS flags) with zero code change; the config documents the Gmail values. The dev inbox is a local catcher so nothing leaves the machine.

**"Show me something that failed and how you found it."** *(a strong question to be ready for)*
Two favourites: (1) a `@OneToOne` with `orphanRemoval` deleted the donor row whenever a donor saved their profile — found because the smoke run checked the row still existed after the save, while the profile round-trip check *passed*; (2) in real mode every API call 404'd because `BASE` repeated the `/api/` prefix that every path already carried — invisible in mock mode, found by running the fetch path for real, and now guarded by a check that was negative-tested (re-injecting the bug makes it fail).

**"Biggest known limitation?"**
Suspending an account blocks the next login but does not kill a session that is already open — that needs a `SessionRegistry` and an `HttpSessionEventPublisher`. It's written down and asserted in the tests as today's behaviour rather than hidden.

**"What relationships does the data model use?"** *(§2.3 explicitly names 1:1, 1:N and M:N)*
All three, and they are exercised, not just declared: **1:1** User ↔ Donor (a role-scoped extension), **1:N** User → BloodRequest, Donor → BloodRequest, Hospital → BloodInventory/BloodRequest, BloodRequest → Timeline/Notifications, and **M:N** Donor ↔ Hospital through the `donor_hospital_affiliation` join table — a donor registers with the partner hospitals in their own district, readable from both sides. The affiliation never moves a rule: BR-1 is blood group and BR-5 is the staff account's hospital, so the M:N is what a hospital counts as its donor base, not a matching input.

**"Anything not finished?"**
Every mandatory item is built. What is left is polish: pagination is client-side slicing rather than `?page=&size=`, and the report's screenshot placeholders still need the final images.

---

## 5. Afterwards — put the demo data back

The seeder is idempotent (it will not re-seed over existing rows), so **booting again does not undo your demo actions**. Every click rewrote real rows. To restore the known-good state:

```bash
cd backend
eval "$(python - <<'PY'
import re
s = open('.idea/workspace.xml', encoding='utf-8').read()
def v(n):
    m = re.search(r'<env name="\s*%s\s*" value="([^"]*)"' % n, s); return m.group(1).strip()
print('BBH=%s' % v('BB_DB_HOST')); print('BBU=%s' % v('BB_DB_USER')); print('BBP=%s' % v('BB_DB_PASSWORD'))
PY
)"
mysql_bb() { /c/xampp/mysql/bin/mysql.exe --host="$BBH" --port=4000 --user="$BBU" --password="$BBP" --ssl -D bloodbuddy "$@"; }

# what did the demo leave behind?
mysql_bb -e "SELECT 'users' t, COUNT(*) n FROM users
  UNION ALL SELECT 'donors', COUNT(*) FROM donors
  UNION ALL SELECT 'requests', COUNT(*) FROM blood_requests
  UNION ALL SELECT 'notifications', COUNT(*) FROM email_notifications
  UNION ALL SELECT 'inventory', COUNT(*) FROM blood_inventory
  UNION ALL SELECT 'timeline', COUNT(*) FROM request_timeline
  UNION ALL SELECT 'affiliations', COUNT(*) FROM donor_hospital_affiliation;"
mysql_bb -e "SELECT public_code, status FROM blood_requests ORDER BY request_id;"
```

- **Want a request gone?** Run the four `DELETE`s the frontend check prints for you
  (timeline → declines → notifications → the request), in that order.
- **Want a request back to `Pending`?** `UPDATE blood_requests SET status='PENDING', donor_id=NULL, matched_at=NULL WHERE public_code='…';`
- **Want the stock back?** TUTH's B+ should be **12**:
  `UPDATE blood_inventory bi JOIN hospitals h ON h.hospital_id=bi.hospital_id SET bi.units=12 WHERE h.short_name='TUTH' AND bi.blood_group='B_PLUS';`
- **Want the whole demo dataset from scratch?** Point the app at a fresh schema and let
  the seeder run once (`bloodbuddy.seed=true`, default).

> **Two traps worth knowing:** run one statement at a time (a multi-statement `-e` stops
> at the first error and silently skips the rest), and the column is **`public_code`**,
> not the `publicCode` the entity declares — Spring Boot's naming strategy snake-cases it.

---

## 6. The 60-second version (if you're cut short)

1. Landing → **drag it narrow** (responsive, mandatory).
2. **Register** → **Mailpit** shows the confirmation mail (§2.4).
3. Log in as the **donor**, **accept** a request → the record changes (real data).
4. Log in as the **admin** → users, moderation, and the 401/403 story.
5. Open `real-backend-check.html` → **39/39**. Close with: *"268 frontend checks, 92 responsive combinations, 39 against the live backend, 31 service-layer checks against TiDB, 94 over raw HTTP."*
