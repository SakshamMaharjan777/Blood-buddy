# BloodBuddy — Project Progress & Context

> Read this file first in any new session. It summarizes the proposal, what's built, and what's next.
> **Update this file at the end of every session** (status, new files, next steps).
> Last updated: 2026-09-17 (session #28 — **THE SESSION-#27 HAND-OFF IS CLOSED: THE STALE APP IS GONE, THE DEMO DATABASE IS PRISTINE AGAIN, AND THE INTERRUPTED CONFIRMATION IS FINISHED — `real-backend-check.html` READS `REAL-BACKEND-DONE 42/42` FROM THAT PRISTINE DB.** No code, test or document changed; it is a *state* session. Item 1 was already true (PID 21268 gone, 8081 and 8025 free), so there was nothing to kill. Item 2: the DB carried session #27’s confirmation run — **25 users / 16 donors / 7 requests / 30 notifications / 13 timeline**, `BB-5MN8VX` re-pointed at **NMC / 10** — so a **full `mysqldump` was taken first** (`.tools/wt/db-before-session28.sql`; TiDB needs it *without* `--single-transaction`) and the restore then ran **one statement at a time**: the three run requests (`EM-L6AVFN`, `BB-9EK9LM`, `BB-4TTYVZ`) with their notifications/timeline, the three `api-check+…` accounts **and the browser check’s own `b7check+…` account — which the printed cleanup SQL does not cover** — with their two donors, `BB-5MN8VX`’s extra timeline rows and its `hospital_id=NULL` + `E28094` label. Re-verified by re-reading every count: **21 users / 14 donors / 5 hospitals / 40 inventory / 4 requests / 3 notifications / 7 timeline / 39 affiliations / 0 declines / 0 donor photos**, TUTH B+ = 12, only Bikash Tamang suspended. Item 3: Mailpit was started against the **repo-local** db (which still held session #27’s **27 messages**) and emptied via the API, the app booted from the runbook’s recipe (`Demo data ready: 21 users…` then `Tomcat started on port 8081`) and the check was run headless against the pristine DB → **42/42**, with both session-#27 pins inside it (a hospital session’s unfiltered `/api/requests` is its OWN queue, and the queue page’s rendered card ids are a subset of it). The check’s own footprint (`BB-2T877U` + `b7check+…`/`b7staff+…`) was removed with the SQL the page itself prints, and pristine was re-verified. **Left exactly as the rehearsal expects: app stopped and 8081 free, Mailpit up on 8025 with an empty inbox, DB pristine.** Everything from #26+#27 is **still uncommitted** (this session touched only `PROGRESS.md`). Previous: session #27 — **THE WALKTHROUGH WAS RUN END TO END, IT FOUND TWO REAL BR-5 DEFECTS (both fixed and pinned), AND BOTH REPORT COPIES NOW CARRY EVERY FIGURE — NO PLACEHOLDER LEFT.** The session-#26 hand-off's last two items are closed. **The walkthrough** (hand-off item 5) was driven against the live TiDB database, checkpoint by checkpoint, verified **from the outside** (HTTP + SQL, not screenshots): register → **201** + the welcome mail in Mailpit + a `SENT` notification row, donor search + widen fallback, request submit → tracker → donor alerts, donor accept → **Matched** (and the O− request correctly **not** offered to the B+ donor), hospital fulfil with the stock going **13 → 11** for a 2-unit request, admin suspend (next login refused with its reason) and forward with its timeline entry, anonymous 401 / wrong-role 403, then the tools — **Newman 76 · 107 · 0**, **qa-harness 268/268**, **responsive 92/92**, smoke **31/31**. **Two real defects came out of it, both in the hospital-scoping story and both invisible to reading:** (1) **BR-5 was not enforced on `GET /api/requests?hospitalId=…`** — `PUT /api/hospitals/8/inventory/B+` correctly answered 409 while the *same data behind a query parameter* answered **200 with Patan's queue** to a TUTH session (and the whole table with no filter); `RequestController.list` now pins a hospital actor to its own hospital, refuses the admin-only views (`?guest`/`?unrouted`/`?requester[Id]`) and treats "no filter" as "my queue"; (2) **`hospital-requests.html` hardcoded the demo's `Bir Hospital, Kathmandu` into real mode**, so a TUTH session rendered Bir's queue — the page now sends no hospital at all and lets the server answer with the session's own. Both are **pinned**: `tools/api_check.py` **95 → 98** and `resptest/real-backend-check.html` **39 → 42** (the page's rendered card ids must be a subset of the signed-in hospital's queue), and every quoted count was updated, Figure 5-1's screenshot **re-captured** (it now reads 42/42) so the picture and its caption cannot disagree. **On the report side:** the four remaining placeholders are **drawn, not waited for** — a new tracked `tools/report/build_diagrams.py` (Pillow only, self-checking for label overflow and box overlap) produces the lifecycle flow (Figure 1-1), the 5-actor/24-use-case diagram (3-2), the Level-1 DFD with 18 numbered flows (3-5) and a rendered repository tree for the file-structure figure (4-1, which also retires the human IntelliJ screenshot); **v1 was rebuilt too**, so both copies carry **identical** text, tables and captions, **20 images, 0 placeholders, 51 pages**. The DB was then put back exactly (21/14/5/40/4/3/7/39, TUTH B+ = 12, `BB-5MN8VX` unrouted), **Mailpit's inbox emptied** and the app stopped. Then both BR-5 fixes were re-tested **from the pristine database** — `tools/api_check.py` **98 passed / 0 failed**, Newman still **76 · 107 · 0** — and that confirmation run is where the session was **stopped mid-step**: the app is **still RUNNING on 8081 (PID 21268)** and the DB is **no longer pristine** (25 users / 16 donors / 7 requests / 30 notifications, TUTH B+ = 15), with the cleanup SQL printed in `.tools/wt/api_check_final.log` and the first hand-off items listing every row to remove. Everything is **uncommitted**. Previous: session #26 — the report images, and the report no longer contradicts itself. The session-#26 hand-off's live thread is closed: `build_report.py` now has its `FIG_IMAGES` screenshot entries (Figures 4-2/4-3/4-4/4-5/5-1) **and** an `APPENDIX_IMAGES` mechanism keyed by appendix title, because Appendix E and F had been left rendering prose with **no images at all** after their placeholder lines were deleted (E-1 reproduces the 5-1 montage; F-1…F-5 are the landing page, the auth pair, the donor dashboard, the hospital queue and the legal row — captions included, and all 19 figure captions match the LIST OF FIGURES exactly). The v2 hand-in copy was **backed up, rebuilt and Word-finalised**, then verified by diffing its stripped text against the backup: `[INSERT SCREENSHOT …]` **19 → 1** (only Figure 4-1, the IntelliJ project view, which a headless browser cannot capture), images **5 → 16**, PDF **48 pages · 16 image XObjects · 2.15 MB**. That rebuild also proved something worth knowing: **v2 had never received session #26's text corrections** — it still said "266 checks", "16/16 over the stub" and "VS Code", because §26.11 edited `bb_content.py` but never rebuilt. Now it says 268/268 · 92/92 · 39/39 and IntelliJ IDEA. **The one thing session #26 did not sweep was the report's backend narrative, and it was self-contradictory:** §5.5 claimed "the remaining mandatory criteria belong to the scheduled backend phase and are not claimed as complete" in the same chapter whose Table 5-1 shows TC-18/20/21 PASS against that backend, while §7.2's limitations said the backend "is the scheduled implementation phase", email was "not yet sending" and the hospital APIs were "stubbed but not live". All of it is corrected (~14 statements: disclaimer, Table 2-2, §3.9 bullets, §4.1 environment → TiDB Cloud + Mailpit + Newman, §4.3.2, §4.3.4, §4.5, §5.5, Table 5-2 FR-01/02/06, Ch6 intro, Table 6-1 rows 13/14 → Complete, §7.2's five bullets, §7.3's first two bullets, and Appendix C → the **nine real tables** instead of "to be exported from MySQL Workbench during the backend phase"). What remains in v2 is four bracketed placeholders — Figure 4-1 plus three diagrams (1-1 lifecycle, 3-2 use case, 3-5 DFD) that have no drawn asset — and the walkthrough. **`docs/BloodBuddy_Final_Report.docx` (v1) was deliberately NOT rebuilt, so v1 is now materially older than v2.** Everything is **uncommitted**, and the demo DB/app state was not touched this session. Previous: session #26 — **§2.6 IS PROVEN, NOT PROMISED.** The session-#25 collection finished its verification: under Newman, from a pristine database, it reports **76 requests · 107 assertions · 0 failures** (~2 min, exit 0), and **a second consecutive run on its own leftovers is ALSO 0 failures** — so the collection is re-runnable, which retires the session-#25 worry that a run poisons the next one. The run exercises the **`?status=Pending`-alone** request, so the session-#25 controller fix is now covered by a green run rather than by a diff. The DB was **not** pristine at session start (the session-#25 runs had left 23 users / 15 donors / 8 requests / 33 notifications / 14 timeline / 1 decline / a photo on a seeded donor): it was restored **before** the run and restored again **after**, verified by re-reading every count — **21 users / 14 donors / 5 hospitals / 40 inventory / 4 requests / 3 notifications / 7 timeline / 39 affiliations**, TUTH B+ = 12, `BB-5MN8VX` **PENDING + unrouted** (hex `E28094`), 0 donor photos, 0 declines. The restore is mechanical because the footprint is contiguous (everything is above the TiDB IDENTITY jump: requests `≥210021`, users `210022+`, donor `210009`) and the accounts are named `postman%` — the exact statement list is in the session-#26 section. `docs/DEMO_RUNBOOK.md` loses its §2.6 caveat, gains a headless Newman command in the "every number" table, and `backend/README.md` carries the verified count. The session-#25 hand-off is also closed: **`tools/api_check.py` 94 → 95** — a `?status=Pending`-alone check that asserts only Pending rows *and* fewer of them than the unfiltered list, **negative-tested against the old, buggy payload** (it fails on it); **95 passed, 0 failed**. Counts updated in the runbook and `backend/README.md`. **All of it is committed and pushed as `1c4ca14`** (6 files, +894/−10, `origin/main` moved `45f8785..1c4ca14`), and the demo start was staged (Mailpit emptied, app stopped so IntelliJ's ▶ is free, DB re-verified pristine). The session then went into the **written report's screenshots**: the "9 placeholders" are really **18** — **19 pages were captured from the running app** (`tools/report/assets/shots/`, puppeteer-core in a gitignored `.tools/shots/`), 5 grouped montages composed by a new tracked `tools/report/build_montages.py`, and the report text corrected (266→268, the 16/16-stub evidence → the real-backend **39/39**, Figure 4-1 VS Code → IntelliJ, plus the TC-20/21 and FR-12/13 rows still marked "scheduled"/"Pending (backend phase)" that are long done). Two real defects surfaced on the way and are fixed: **`resptest/real-backend-check.html` was racing TiDB and reading 35/39** (a fixed 1200 ms sleep vs ~1.6 s round-trips removed the iframe mid-fetch; it now waits for the rows — **39/39**), and the **Postman collection's `memberCode` defaulted to a seeded demo code (`BB-5MN8VX`)**, so a run where the capture did not happen first silently patched the demo row's notes (now `''`, re-verified green with the demo row untouched, and the stale note restored). **The report rebuild itself is NOT done**: `build_report.py` still has no image entries, and since Appendix E/F's placeholder lines were already removed those two appendices currently render imageless — that wiring + the v2-only rebuild + the Figure 4-1 IDE screenshot are the first items next session, then the walkthrough. **Everything from this stretch is uncommitted.** Previous: session #25 — **THE POSTMAN COLLECTION WAS ACTUALLY RUN, AND IT WAS BROKEN — it is now fixed and found a REAL API BUG.** Session #21 only ever validated it statically; run under Newman it executed 60 requests but **15 of 91 assertions failed**, because one admin session ran the whole collection while its folders need different roles (its own request names say "as ADMIN / as DONOR / as HOSPITAL") and `memberCode` was used but never captured. Fixed: **14 `Sign in as …` requests** inserted where the role changes (folders 04/05/06/07/08/98), a **second member request in 04** captures `memberCode` and is kept Pending for the §98 refusals, and **02 restores** whoever its suspend test touched (a run used to leave an account suspended and break every later sign-in as it). Collection **60 → 76 requests**. The second Newman run reached **107 assertions, 1 failed — and that failure was the API's, not the collection's**: `GET /api/requests?status=Pending` answered **every** request because `RequestController.list` ignored `status` when it was the only filter. **Fixed** (a `status`-only branch now calls `requests.forAdmin(status,false,false)`), compile clean, app restarted — **but the third Newman run was NOT completed, so 0-failures is NOT yet proven** (that is step 1 next session). Also added the **operator's checklist** as §0 of `docs/DEMO_RUNBOOK.md` (boot, expected log lines, every number and how to reproduce it solo, a troubleshooting table, shutdown/restore) because the user must run the defence with no assistant. Tooling checked: Python/Java/Node/Chrome/IntelliJ/**Postman desktop** all present; Newman is not installed but runs via `npx newman`. The demo DB was restored **pristine** before the re-run (21/14/5/40/4/3/7/39, `BB-5MN8VX` **PENDING + unrouted**, TUTH B+ = 12) and a stray `STATUS_UPDATE` notification left by a Postman run that had fulfilled `BB-5MN8VX` was removed. **The app is currently RUNNING on 8081** (started for the walkthrough) — stop it before a fresh IntelliJ ▶. Session #25 changes are **uncommitted**. Previous: session #24 — **TWO OF THE THREE PROPOSAL GAPS ARE CLOSED.** Asked whether the backend was complete, a check against CPJ119 + proposal Appendix B found three conformance gaps; two are now fixed and verified. **1 · Server-side pagination** — new `dto/PageResponse`, and `GET /api/donors/search`, `/api/admin/users`, `/api/requests` accept **additive `?page=&size=`** (a paged envelope when asked; the plain array otherwise, so the closed frontend is untouched). **2 · The Appendix B hospital-scoped "local API"** — `HospitalController` gained `POST /api/hospitals/inventory`, `PUT /{hospitalId}/inventory/{bloodGroup}`, `GET /{hospitalId}/donors/nearby`, `POST /{hospitalId}/requests` and `GET /{hospitalId}/requests/{id}/status`; `AdminController` gained `POST /api/admin/users/{id}/deactivate`. All thin delegates — no logic moved — with `requireScopedAccess` keeping **BR-5** for a hospital id taken from the PATH. **3 · literal `DELETE` endpoints — deliberately NOT added** (soft-delete via cancel/suspend is the design; recorded, not hidden). Verified: compile clean · `tools/api_check.py` extended **81 → 94 checks, 0 failed** · smoke run **31/31, exit 0** · Postman collection **50 → 60 requests**, statically re-validated (no undefined `{{variables}}`, every path matches a declared controller route) · demo DB restored pristine (21/14/5/40/4/3/7/39, `BB-5MN8VX` unrouted), 8081 free. Previous: session #23 — **THE SESSION-#22 HAND-OFF LIST IS WORKED.** The mock suite was re-run (**268/268**, fresh profile) to cover the `bb-api.js`/`auth-register.html` edits that landed after #20. `resptest/real-backend-check.html` was **extended for the two new surfaces and is now 39/39** against the booted app — four checks for the Donor ↔ Hospital M:N read (Arun, Kathmandu, correctly affiliated with the **4** Kathmandu partner hospitals incl. TUTH) and three for staff registration **by hospital name** (201, `hospitalId` set, label contains "Tribhuvan"). Docs brought up to date: `backend/README.md` (39/39 · 50 Postman requests · 81 `api_check` · 31 smoke · the new `{id}/hospitals` route · the M:N in the model section), `docs/DEMO_RUNBOOK.md` (all counts, an M:N Q&A, and the now-closed "hospital not linked" limitation) and **the report's Table 3-1** (`tools/report/bb_content.py`) which gained the `Donor ↔ Hospital (affiliation)` M:N row — then **both report `.docx`/`.pdf` were regenerated**: `v1` via `build_report.py` + `finalize_word.py`, and `v2` **patched in place** (it differed only in TOC state, verified by diffing the stripped text, so nothing was lost) and re-exported. Finally **the demo database was cleaned back to the known-good state** — 3 test requests, 5 test accounts, 2 extra donors, their affiliation rows and a leftover forward-timeline row removed, `BB-5MN8VX` re-unrouted (label hex `E28094`) — verified **21 users / 14 donors / 5 hospitals / 40 inventory / 4 requests / 3 notifications / 7 timeline / 39 affiliations**, TUTH B+ = 12; app stopped and 8081 freed. **What is left of the list is the last item: teach the user to operate the system for the defence.** Previous: session #22 — **THE TWO CRITERIA GAPS ARE CLOSED: the M:N relationship and the hospital-staff link** (see the session #22 section — three ways green: smoke **31/31**, real HTTP **81/81**, and **39** join rows in TiDB; it also caught two real defects, a repository method name that cannot exist and a cleanup script that was writing the wrong character). **Right after that: re-run the mock QA harness (268/268) — the `bb-api.js`/`auth-register.html` edits landed after session #20's run — then extend `real-backend-check.html`, then the docs, then TEACH THE USER TO OPERATE IT for the defence.** Note the DB currently carries the last `api_check.py` footprint (2 requests + 3 `api-check+…` accounts, and BB-5MN8VX routed to NMC) — the printed cleanup SQL in `.tools/api_check.log` restores it. Previous: session #21 — **B8 IS DONE: the Postman collection exists** (`docs/postman/BloodBuddy.postman_collection.json`) — **48 requests in 10 folders** covering every Appendix B endpoint plus B3's additions and the BR-1…BR-9 refusals, with **cookie-based login**: `01 · Session` POSTs `/api/auth/login`, asserts `pm.cookies.get('JSESSIONID')` so the capture is *visible in the Tests tab*, and captures `userId`/`donorId`/`hospitalId` into collection variables so **no request hardcodes a row id** (the lesson from B3's `id=1` bug). Folder `00` runs logged OUT on purpose (it asserts the 401 boundary), `99` logs out and re-checks that `/api/auth/me` answers 401. Validated statically — JSON parses, no undefined `{{variables}}`, and **all 48 paths match the 25 routes declared in the controllers** — plus the overlapping live checks from session #20 (401 anonymous, 403 wrong role, logout→401, register 201). It has *not* been run inside Postman/Newman, because neither is installed on this machine; that is stated rather than implied. Previous: session #20 — **B7 IS DONE: the frontend runs on the REAL backend.** `bb-api.js` ships `MOCK = false` (with a documented `localStorage.bb_force_mock = '1'` override, which is how the 266-check mock suite and the Python stub keep running), the cache is no longer the session — `nav.js` reconciles `bb_session` with `GET /api/auth/me` on every page load and Log out really ends the server session — and `auth-register.html` finally **sends `password`** (min 8, BCrypt server-side) and reports the server's own error instead of showing a success screen for a failed signup. Verified **268/268 in mock mode** (266 + 2 new §22 transport checks, so nothing regressed) and **32/32 on the new `resptest/real-backend-check.html`** against the booted app (25 read/security checks **plus the donor write path** — submit, accept, re-read from the server): real transport in every page, the demo store deliberately pinned EMPTY so rendered rows can only have come over HTTP, anonymous 401, wrong-role 403, session `/me`, logout-really-ends-it, and a registration that landed a row in TiDB **and** a `Welcome to BloodBuddy` mail in Mailpit. Demo DB verified back afterwards (21 users / 14 donors / 4 requests / 3 notifications, TUTH B+ = 12, 8081 free). Next: **B8 (Postman)**, then the demo runbook. Previous: session #19 — **B4 IS DONE: §2.4 email is DELIVERED, not just queued.** `service/NotificationMailer` composes and sends the message over SMTP, `NotificationService` records the outcome on the row it already wrote (`QUEUED` → **SENT** with `sentAt`, or **FAILED** with the server's own error), and the plain-text body is stored on the row so the audit table *is* the evidence. **A mail failure never fails the business operation** — registration and every rule still pass with the mail server down (proved by running the suite both ways). The dev catcher is **Mailpit** in a gitignored `.tools/` (no install, no Docker): SMTP 1025 + inbox/API on 8025. Verified **28/28 smoke checks, exit 0** with the catcher up and **26/28 with it stopped — exactly the 2 mail-arrival checks, everything else passing**; 52 messages captured; demo DB unchanged at 21 users / 14 donors / 4 requests / 3 notifications / 40 inventory rows. `application.properties` now drives `BB_SMTP_AUTH`/`BB_SMTP_STARTTLS` from env too, so the real-Gmail §2.4 demo is a 4-variable swap with zero code change (it is only waiting on Google's new-phone delay for the app password). Next: **B8 (Postman) then B7 (`MOCK = false`)** — B5 was already covered by B3's photo endpoints. Previous: session #18 — **B6 IS DONE: Spring Security.** Role rules on every `/api/**` group (anonymous → 401, wrong role → 403 as the same `ApiError` JSON), a real **session-backed login** (`POST /api/auth/login` verifies with BCrypt — in `AuthService`, per §2.2 — then opens a Spring Security session, so a `JSESSIONID` cookie identifies every later request; `POST /api/auth/logout` ends it), and **the dev actor header is gone**: `ActorContextArgumentResolver` now reads the session principal as `AuthenticatedAccount` (reloading the `User` per request, so a role/status change bites immediately). Verified **74/74 over real HTTP** with cookie sessions — including anonymous 401, wrong-role 403 and logout-really-ends-it — and the B2 smoke run still **25/25 with security on** (`backend/target/bb-http-b6.log`, `backend/target/bb-b6smoke.log`); the demo DB was verified back to 21 users / 14 donors / 4 requests / 3 notifications / 40 inventory rows and 8081 is free. Previous: session #17 — **B3 IS DONE: the REST layer answers — 7 controllers over every Appendix B endpoint, a `@ControllerAdvice` mapping the service exceptions to 404/409/400, and an `ActorContext` argument resolver. 56/56 checks pass over real HTTP (`tools/api_check.py`), including the negative cases for BR-1/2/3/5/8; the demo DB was restored afterwards. Next: B4/B6/B7/B8.** Appended to the same session: — **B2 IS DONE: DTOs + the whole service layer (auth, donor search + widen fallback, the request lifecycle with BR-1…BR-8, inventory, admin users, profiles, notification hooks), verified by a 25-check smoke run against live TiDB — 25/25 PASS — which found and fixed two REAL bugs (a `@OneToOne` orphanRemoval that deleted donors on profile save, and a seeder notification guard that added a duplicate welcome row every boot). Next: B3 controllers. Appended to the same session: — **BOOT GATE CLOSED: the B1 seeder fix is verified at runtime** — `Demo data ready: 21 users, 14 donors, 5 hospitals, 40 inventory rows, 4 requests, 3 notifications`, Tomcat on 8081, process stayed up, landing served 200; the session-#16 "22 users" expectation was a miscount (12 + 9 = 21 is correct, see the session), the port-8081 process was stopped again so IntelliJ's ▶ stays free, and the password-holding `workspace.xml.bak-*` is deleted. **Nothing left in front of B2.** Session #16 — **THE FIRST BOOT HAPPENED: the app runs (Tomcat on :8081, TiDB Cloud connected, all 8 tables created)** after clearing two in-flight blockers (IntelliJ stored the `BB_DB_*` env vars with invisible leading/trailing whitespace → Spring fell back to `localhost` → patched in `workspace.xml`; port 8080 is owned by the EDB PostgreSQL PEM Apache service → app moved to **8081**) and fixing a genuine B1 bug (`DemoDataSeeder.fresh` attached a transient `User` to the non-nullable `Donor.user` FK and killed the boot — fixed, compile-verified). **The fix is compiled but NOT yet started: next session is one ▶ away** (expect 22 users / 14 donors / 5 hospitals / 40 inventory rows / 4 requests), then **B2**. Session #15 — **credentials sorted, NO code changed: leaked Gmail app password deleted, dedicated Gmail created (`bloodbuddy1org@gmail.com`), TiDB Cloud Starter cluster "BloodBuddy" created and ACTIVE, `BB_DB_*` set in the IntelliJ run config. **The app has NOT been booted yet** — next is the SQL-Editor `CREATE DATABASE` + first ▶, then B2**. Session #14 — **B1 done: all 6 JPA entities + repositories + seeder, compiles clean**; next B2 service layer. Session #13 — **backend started (B0 done)**: Spring Boot 3.5.16 skeleton in `backend/` (Java 17, Web+JPA+Validation+Mail+MySQL→**TiDB Cloud**), compiles via IntelliJ's bundled Maven 3.9.11; frontend synced into `backend/src/main/resources/static/` by `tools/sync_frontend_to_backend.py` (94 files; static/ is gitignored, re-run after frontend edits). Open `backend/` in IntelliJ → run `BloodBuddyApplication` → app serves frontend at `http://localhost:8080/CODE/HTML/landing.html`. **Class reference project `SpringWebVir` copied to gitignored `reference/` — see its addendum for adopt/avoid patterns; CREDENTIAL-EXPOSURE addendum has the account setup the user must finish before first boot (new Gmail → TiDB cluster → env vars). Plan: B1 entities → B2 service layer → B3 controllers → B4 MailHog email → B5 BLOB photos → B6 security → B7 MOCK=false + harness → B8 Postman. Previous: session #12 — **CPJ119 written report generated**: `tools/report/` builds `BloodBuddy_Final_Report.docx/.pdf` from the reference template with all BloodBuddy content; v2 with cover logo + 4 diagrams is in TEMP — copy out or rebuild. Session #11 — **the FRONTEND IS CLOSED**: every CPJ119 §2.1 requirement is implemented and verified (266/266 harness checks, 92/92 responsive page×width combos, 16/16 real-HTTP). The donor accept/decline loop landed and the AJAX path is now provable, not just written. **Everything mandatory still outstanding is BACKEND / submission artifact** — audit in "Session #11", ordered list in "Next steps")

## Project

**BloodBuddy** — web-based blood donor management system for Nepal (academic project, CPP400/CPP401, Asia e University, BICT, Saksham Maharjan).

- **Proposal doc:** `docs/BloodBuddy_Proposal_Revised.docx` — **overwritten session #10** with the newest revision (user's "blood buddy last.docx", md5 bac06dc9…, supersedes the 09-13 copy). New scope bullets: client-side form validation, AJAX/Fetch to REST endpoints, pagination for donor search + admin listings.
- **Assignment sheet:** `docs/CPJ119_Assignment_Requirements.docx` (added session #10) — CPJ119 Advanced Java spec. Frontend-relevant: §2.1 responsive (mandatory) + client-side validation + AJAX/Fetch; §2.2 "frontend only handles presentation and basic input validation"; §2.5 image handling (BLOB or Base64 in DB); §2.6 REST + Postman; §3 RBAC/search/pagination as differentiators; §4 validation on BOTH frontend and backend.
- **Backend (planned, CPP401):** Spring MVC + Hibernate ORM + MySQL, SMTP email notifications, BLOB image storage, REST API tested via Postman
- **4 roles (RBAC):** Donor, Requester, Hospital Staff, Administrator
- **5 partner hospitals:** TUTH, Bir Hospital, Patan Hospital, Manmohan Memorial Community Hospital, Nepal Medical College Teaching Hospital
- **Key API endpoints (proposal Appendix B):** `/api/auth/*`, `/api/donors/search`, `/api/requests`, `/api/hospitals/{hospitalId}/inventory`, `/api/hospitals/{hospitalId}/requests`, `/api/admin/users`
- **Blood compatibility chart (Appendix A):** used in requester-request.html JS (`COMPAT` map) — real matching logic belongs in the Spring Service layer later

## Tech / conventions (frontend)

- Pure HTML/CSS/JS, no frameworks, no build step
- Fonts: DM Serif Display (display) + DM Sans (body); brand red `#C0202A`; bg `#F7F6F4`; radius 12 cards; inset-ring shadows
- Shared: `CSS/landing.css` + `CSS/landingresponsive.css` (navbar/footer/buttons/sections/CTA) + per-page CSS + `CSS/bb-ui.css` (session #10: pager, inline validation errors, photo upload)
- Inline `<script>` at bottom of each page + shared `CODE/js/nav.js` (session-aware nav, loaded first) + `CODE/js/main.js` (ripples/tilt) + `CODE/js/bb-store.js`
- **Session #10 shared modules (all pure JS, no build step):**
  - `CODE/js/bb-validate.js` — shared client-side validation (email/phone/blood-format/password/future-date/units/terms + required). Inline `<small.field-error>` under the field via `.has-error` on the wrapper (works with `.field-group`/`.form-field`/`.pf-field`/`.ct-field`). Exposed as `BloodBuddyValidate` (`check`, `checkAll`, `bindLive`, `setError`, `clearError`).
  - `CODE/js/bb-api.js` — THE only place pages touch the backend. Promise facade mirroring proposal Appendix B (`auth.register/login/forgot`, `contact`, `donors.search/get`, `requests.list/get/create/update`, `inventory.get/update`, `users.list/setStatus`, `profile.get/save`). **`MOCK = false` — the shipped default since session #20 is the REAL transport**, so the mock adapter below is now reached only by forcing it (`localStorage.bb_force_mock = '1'`, which is how the QA harness, `real-mode-check.html` and the Python stub keep running): mock adapter over BloodBuddyStore with ~60ms latency; **store mutations are synchronous**, only resolution is delayed (QA reads localStorage directly). `MOCK = false` → real `fetch()` to `/api/...` — `BASE` is the origin prefix only and must stay `''` or an absolute origin, because **every path already carries `/api/`** (a path-prefix `BASE` doubles it to `/api/api/...` and 404s every call; verified against `tools/mock_api_server.py`, guarded by harness §22).
  - `CODE/js/bb-paginate.js` — reusable pager (Prev/Next, Page X of Y, per-page select 5/10/20, hides when empty). `BloodBuddyPaginate.create({mount, pageSize, onChange})` → `setTotal()` + `slice(items)`. CPP401: swap client slicing for `?page=&size=`.
  - `CODE/js/bb-compat.js` — the Appendix A blood-compatibility chart, shared (session #10 follow-up). `BloodBuddyCompat.forRecipient(g)` (recipient → compatible DONOR groups) and `donorsFor(g)`. Previously copy-pasted into `requester-request.html` and `emergency-request.html`; the donor dashboard's accept/decline list would have been a third copy, so it was extracted rather than triplicated. **Direction matters** — reading the chart backwards offers a donor a patient they must not give blood to.
  - `bb-store.js` gained a `donors` collection (`bb_db.donors`, `SEED_DONORS` = the 14 donors moved out of requester-search.html) + `Store.donors.all()/find()`; `reset()` clears it too.
- **CTA roles (session #7):** nav.js swaps logged-in `auth-register` CTAs via `ROLE_CTA` AND removes `requester-request` CTAs ("Request blood") for every role except requester — a donor/hospital/admin should never be offered a requester action. Logged-out visitors keep both marketing CTAs. shared demo "database" in localStorage (`bb_db.requests`, `bb_db.inventory`, `bb_db.users`), seeds itself on first visit, migrated from legacy `bb_requests` key. Used by 7 pages: requester-request, requester-search (direct-to-donor requests, added session #6), requester-tracking, hospital-dashboard, hospital-requests, admin-dashboard, admin-requests. All 4 roles read/write the SAME store so the request lifecycle persists across pages. Backend swap (CPP401) = replace its load/save internals with `fetch()`; pages don't change.
- **Session model (demo):** localStorage `bb_session` = `{ role, name }`, set by login/register via `BloodBuddyNav.setSession()`. When set, nav.js **rebuilds `.nav-links` on EVERY page** from the role's link set in `ROLE_LINKS` (portal pages first, e.g. Donor Portal/My Profile), keeping any About/Contact links the page already had (with their `.active` state) — so your portal links follow you onto landing/about/contact/terms/privacy instead of the navbar reverting to generic marketing links. Auth buttons become a **user chip (a link — opens the role's profile page via `PROFILE` in nav.js; every role has a dedicated profile page since session #8)** + Log out; **in-page marketing CTAs (`a[href^="auth-register"]` outside the navbar) are swapped to the role's destination via `ROLE_CTA` (e.g. donor sees "Go to Donor Portal" instead of "Register as donor")**; Esc returns to the dashboard (skips open modals). Absent key = logged out (static navbar + marketing CTAs show as shipped). **Session #7:** in-page `requester-request` CTAs ("Request blood") are REMOVED for logged-in donor/hospital/admin (requester-only action — a donor should never be offered it); requesters and logged-out visitors keep them.
- **RBAC page guard (session #7, extended #8):** role-private pages enforce the role split in nav.js (see its header comment): `donor-dashboard/history`, `hospital-dashboard/requests/profile`, `admin-dashboard/requests/profile`, `requester-search/request/tracking/profile` allow only their role — i.e. every role's profile page is role-private too (`donor-profile.html` used to be a separate guard; it now lives in the same `RBAC_GUARD` map). Logged-out visitors bounce to `auth-login.html`, wrong-role users to their own dashboard; the bounce stashes the blocked URL in sessionStorage (`bb_return_to`) and the login page offers to continue there — but only if the role picked at login may actually access it (`BloodBuddyNav.canAccess`, never bouncing a requester back into the admin portal). On PUBLIC pages while logged out, nav.js also rewires any hard-coded links to private pages (e.g. landing's portal cards) to `auth-login.html`, keeping the original target in `data-bb-return` so login continues from there. Requester pages stay public by design (emergency flow works logged out). This is demo-level client control — the real gate is Spring Security per role in CPP401.
- Hero treatment: photo + red gradient, hover zoom, `.hero-pill` badge — images rotate across pages:
  - `tim-marshall-cAtzHUz7Z8g-unsplash.jpg` → requester-search, hospital-dashboard, requester-profile, hospital-profile
  - `hero-donation.jpg` → requester-request, hospital-requests (`.alt-hero` class)
  - `faezeh-eslami-Btja3lr5ldk-unsplash.jpg` → donor-dashboard, donor-history, donor-profile, requester-tracking, about, contact
  - `adrian-sulyok-sZO8ILzGKcg-unsplash.jpg` → admin-dashboard, admin-profile
- All data is **demo/hardcoded or localStorage** until the backend exists

## Pages built (status)

| Page | Purpose | Status |
|---|---|---|
| `landing.html` | Public marketing page | ✅ pre-existing; session #5: footer wired; session #6: logged-in users keep portal links; **emergency CTAs now action-first** |
| `auth-login.html` / `auth-register.html` | Auth screens | ✅ pre-existing; session #5: footer links wired; **session #6: Terms/Privacy now link to real pages** |
| `auth-forgot.html` | Password reset request | ✅ session #4 |
| `donor-dashboard.html` / `donor-history.html` | Donor role | ✅ pre-existing; session #5: footer links wired |
| `donor-profile.html` | Donor profile editor (localStorage `bb_donor_profile`) | ✅ session #4 |
| `requester-profile.html` | Requester profile editor — contact details, default hospital/district, urgent-alerts toggle (localStorage `bb_requester_profile`) | ✅ session #8 |
| `hospital-profile.html` | Hospital blood bank profile editor — facility type, hours, blood-bank-open toggle (localStorage `bb_hospital_profile`) | ✅ session #8 |
| `admin-profile.html` | Admin account editor — own details only, read-only role/since fields (localStorage `bb_admin_profile`) | ✅ session #8 |
| `requester-search.html` | Find donors (filter chips, donor cards, request modal) | ✅ pre-existing; session #5: fixed wrong-donor-in-modal bug; **session #6: request modal now persists to the store** |
| `requester-request.html` | Submit blood request form (compat chips, compatible-groups toggle) | ✅ session #4; verified by QA harness session #5; **member-only since session #7** |
| `emergency-request.html` | **Public** emergency request form for guests (no login) — red hero, 102 reminder, compat chips, success state with request ID; persists `EM-*` requests to the shared store | ✅ session #7c |
| `requester-tracking.html` | My Requests tracker | ✅ built session #4; **session #6: FIXED fresh-load render bug** (render() was never called on load) |
| `hospital-dashboard.html` | Blood bank inventory | ✅ session #2; adjust-stock flow verified by harness |
| `hospital-requests.html` | Incoming requests queue | ✅ session #2; **session #5: fixed crash on `Matched` status** |
| `admin-dashboard.html` | Platform admin (KPIs, charts, users table) | ✅ session #3; suspend flow verified; **session #6: "Reset demo data" control added** |
| `admin-requests.html` | Request moderation | ✅ session #4; **session #5: fixed crash on `Matched` status**; forward flow verified |
| `about.html` | About page: hero, mission/vision, problem/solution, values, 5 partner hospitals, project/stack info, CTA | ✅ built session #5 |
| `contact.html` | Contact page: info cards, demo form (success state), FAQ accordion, OpenStreetMap embed | ✅ built session #5 |
| `404.html` (**project root**, not in CODE/HTML) | Branded 404 ("This page has no pulse"); root-absolute paths (`/CSS/...`, `/CODE/HTML/...`) so it works at any URL depth; self-contained (no nav.js); conditional "Go back" via referrer check | ✅ built session #5 |
| `terms.html` | Terms of Service (10 sections, demo/academic disclaimer) | ✅ built session #6 |
| `privacy.html` | Privacy Policy (data table, usage, retention) | ✅ built session #6 |

CSS added: `requester-request.css`, `requester-tracking.css`, `hospital.css`, `admin.css` (sessions #2–4); `about.css`, `contact.css`, `404.css` (session #5); `legal.css` shared by terms+privacy (session #6); `profile.css` shared by requester/hospital/admin profiles (session #8).

## QA harness (session #5, extended in #6–#8)

- `resptest/qa-harness.html` — self-driving test page: loads every page in hidden iframes with a cleared localStorage, runs **268 checks** (render smoke on 23 pages — public pages logged out, private pages under the right session AND logged out to prove the bounce — plus interactions: landing emergency CTAs + logged-out CTA guardrails, EMERGENCY guest submit → store + success ID + status lookup (by ID / not-found / member-ID privacy) + Emergency badges in hospital & admin queues, session-aware quick-search nudge, search modal donor-filter + persistence, request submit → store → tracking redirect, tracking render + cancel, hospital accept→fulfil→inventory decrement, admin forward, stock adjust, user suspend, demo reset, login redirect, register wizard + legal links, contact form, about content, 404 links, session-aware nav + CTAs + profile chip on landing/about/contact for donor + requester roles), plus §21 the donor side of the lifecycle (match direction against Appendix A, accept/decline, requester visibility) and §22 the real-mode transport contract (`BASE` never doubles `/api/`, every endpoint path is origin-relative, ships `MOCK = false`), then dumps a PASS/FAIL report into the DOM.
- Run it (bash, from repo root):
  1. `python -m http.server 8321 --directory . &` (skip if still running)
  2. `"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --virtual-time-budget=120000 --timeout=60000 --user-data-dir=$TMP/chrome-qa "http://localhost:8321/resptest/qa-harness.html" --dump-dom > resptest/qa-dom.txt 2> resptest/qa-stderr.txt`
  3. `grep -o "<title>[^<]*</title>" resptest/qa-dom.txt` → `QA-DONE 198/198` means done (number of checks grows as tests are added — just verify nothing failed); read the `<pre id="out">` block for per-check results. (Needs the big virtual-time-budget: 30s expired early. Chrome may linger after the dump — the results land in `qa-dom.txt` regardless. Chrome console errors are capturable via `--enable-logging=stderr`.)
- **Last result: 268/268 passed** (session #20, re-run green in #23/#26/#27) — verified after the validation/fetch/pagination work, the loose-end follow-up, the donor-side lifecycle, the real-mode transport check and the transport rewrite; the checks grew 216 → 245 → 261 → 266 → 268 (see "Session #10" for what changed and which real bugs it caught). The harness's `withPage` also accepts a `seed` option that writes localStorage BEFORE the iframe loads (used to plant guest EM requests, session #9's role-flow seeds, etc.) — note a seed **replaces** the store's own seeds, because `primeStorage` clears storage first.
- **Run it with a FRESH `--user-data-dir` and a `?v=` cache-buster.** Reusing a profile (or the same URL) makes Chrome serve the previous run's cached pages and report identical stale numbers — that is exactly how a fixed harness appeared to still fail 33 checks.
- **`resptest/responsive-check.html`** covers what this harness structurally cannot: responsive layout. Its iframes are a fixed 1280px, but CPJ119 2.1 makes a "fully responsive layout … across mobile, tablet and desktop" **mandatory**. The responsive check loads all 23 pages at 360/430/768/1280 (media queries resolve against the IFRAME viewport, so a 360px iframe lays out like a 360px phone) and reports any page whose `scrollWidth` exceeds the viewport, naming the widest offenders. Run it the same way; **last result 92/92**.
- **`resptest/real-mode-check.html` + `tools/mock_api_server.py`** cover the one requirement the other two tools structurally cannot: that the AJAX/Fetch layer actually talks to an HTTP API. `qa-harness.html` runs in mock mode (no request is ever made; DevTools Network stays empty), so on its own the fetch requirement is "met" only on paper. `tools/mock_api_server.py` serves the static pages **and** an Appendix B stub on one port, so flipping `MOCK = false` produces a page whose `/api/...` calls resolve against its own origin. Run:
  1. `python tools/mock_api_server.py -p 8322`
  2. **nothing to flip** — `MOCK = false` is the shipped default since session #20; the mock path is only reached through the `localStorage.bb_force_mock = '1'` override
  3. `"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --user-data-dir=$TMP/chrome-real --virtual-time-budget=25000 --dump-dom "http://127.0.0.1:8322/resptest/real-mode-check.html"` → title `REAL-DONE 16/16`
  4. **nothing to set back** — and §22 of the harness now fails if `MOCK` is not `false`.
  The proof does not rely on the page merely rendering: `localStorage` is cleared per page and the `bb_db.*` collections are pinned to empty sentinel arrays, so any rendered rows can only have arrived over HTTP. The stub's stdout logs each matching `GET /api/... -> 200`.

## Session #28 (2026-09-17) — the session-#27 hand-off is closed: the stale app is down, the demo DB is pristine again, and the interrupted confirmation reads 42/42 ✅

Started as “read PROGRESS + the proposal and continue from there”, so this session worked the
session-#27 hand-off list in order. **No product code, test or document changed** — it is a *state*
session, and its value is that the next reader can trust the demo environment again.

### 1. Hand-off item 1 — the “running app” was already gone
`tasklist` for **PID 21268** → *“No tasks are running which match the specified criteria”*, and
`netstat` showed nothing listening on 8081 or 8025. So there was no second app to avoid and nothing
to kill. (Mailpit was down too — which is why §2’s “empty the inbox” had to be done by starting it,
see §3.)

### 2. Hand-off item 2 — the demo database, restored statement by statement
Read first, deleted second. At the stop the DB held **25 users / 16 donors / 5 hospitals / 40
inventory / 7 requests / 30 notifications / 13 timeline / 39 affiliations / 0 declines**, TUTH B+ =
**12** (already back), `BB-5MN8VX` **PENDING but routed to NMC (`hospital_id = 10`)**, and the run’s
four extra accounts (`api-check+1789639954108730900`, `api-check+nurse1789639961113449400`,
`api-check+nurse21789639962147897100`, `b7check+1789640001559`) with two donors and three requests.
- **A full pre-cleanup dump was taken first**: `mysqldump` → **`.tools/wt/db-before-session28.sql`**
  (39 KB, 8 tables). Use it **without `--single-transaction`** — TiDB has no `SAVEPOINT` and the
  flag dies with `Couldn't execute 'ROLLBACK TO SAVEPOINT sp'`. This mattered because the printed
  cleanup SQL covers the run’s own footprint but **not** the browser check’s `b7check+…` account.
- **The restore, one statement per client invocation** (the runbook’s “a multi-statement `-e` stops
  at the first error and silently skips the rest” trap): declines → notifications → timeline →
  `blood_requests` for request ids **270025/270026/270027**; then `BB-5MN8VX`’s extra timeline rows
  (`seq > 0`), the `hospital_id=NULL` + `CONVERT(UNHEX('E28094') USING utf8mb4)` re-label, `arun.s`
  back to `ACTIVE` and a `photo=NULL` sweep; then the `api-check+%` **and `b7check+%`**
  notifications, affiliations, donors and users.
- **Verified by re-reading every count**, not by trusting the statements: **21 users / 14 donors /
  5 hospitals / 40 inventory / 4 requests / 3 notifications / 7 timeline / 39 affiliations /
  0 declines / 0 donor photos**, TUTH B+ = **12**, `BB-5MN8VX` **PENDING + unrouted**
  (`hospital_label` hex **`E28094`**), only **Bikash Tamang** suspended. The session-#27 “divergence
  to settle” is settled: the em-dash label and the NULL hospital are back.

### 3. Hand-off item 3 — the interrupted confirmation, finished: 42/42 from pristine
The session-#27 tail left `.tools/wt/final-dom.html` at **0 bytes**, so the page’s 42/42 stood only
from a pre-restore run. Finished here, in this order: Mailpit started against the **repo-local**
`.tools/mailpit/mailpit.db` — which still held session #27’s **27 messages** — and emptied through
the API (`DELETE /api/v1/messages` → `total: 0`); the frontend `static/` verified already in sync
with `CODE/HTML/hospital-requests.html` and `resptest/real-backend-check.html` (`diff -q` clean, so
the **session-#27 BR-5 page fix was really the thing under test**); the app booted from the
runbook’s own recipe, its log carrying **`Demo data ready: 21 users, 14 donors, 5 hospitals, 40
inventory rows, 4 requests, 3 notifications`** and then **`Tomcat started on port 8081`**; then the
check run headless:
```bash
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu \
  --user-data-dir=$TMP/chrome-rb-s28 --virtual-time-budget=60000 \
  --dump-dom http://localhost:8081/resptest/real-backend-check.html
```
→ **`<title>REAL-BACKEND-DONE 42/42</title>`** (`.tools/wt/rb-s28-dom.html`). Both session-#27 pins
are inside those 42: *“a hospital session asking /api/requests with no filter gets ITS OWN queue”*
and *“…the queue page rendered this hospital’s requests (2 card ids)”*, with `shown == own` =
`[BB-2T877U, BB-7HD4LN]`.

### 4. The run’s footprint removed again — and pristine re-verified
The check registers accounts and submits a request, so it is not free: **23 users / 15 donors /
5 requests / 17 notifications / 9 timeline** immediately after it. Its own page **prints the exact
cleanup SQL** (one statement at a time, FK order timeline → declines → notifications → request),
applied here for `BB-2T877U` and the `b7check+…`/`b7staff+…` accounts, plus the `BB-5MN8VX`
timeline and `arun.s` lines. Re-read afterwards: **21 / 14 / 5 / 40 / 4 / 3 / 7 / 39 / 0 declines /
0 photos**, TUTH B+ = **12**, `BB-5MN8VX` **unrouted (`E28094`) + PENDING**, only Bikash suspended.

### 5. The machine was left in the rehearsal’s start state
- **App stopped; 8081 free** (the `java` PID holding 8081 was killed; only `TIME_WAIT` client
  sockets remain) — IntelliJ’s ▶ will bind first try.
- **Mailpit left UP on 8025 with an EMPTY inbox** (repo-local db, `total: 0`), because that is
exactly what hand-off item 4 expects: “once 8081 is free, Mailpit is up and empty and the DB is
  pristine”.
- **DB pristine**, verified twice — before the confirmation run and after its footprint came out.

### State at session end
- **Nothing is running but Mailpit** (SMTP 1025, inbox 8025, empty). **8081 is free.**
- **DB is pristine:** 21 users / 14 donors / 5 hospitals / 40 inventory / 4 requests /
  3 notifications / 7 timeline / 39 affiliations / 0 declines / 0 donor photos, TUTH B+ = **12**,
  `BB-5MN8VX` **PENDING + unrouted** (`E28094`), only **Bikash Tamang** `SUSPENDED`.
- **Evidence from this session** (all gitignored under `.tools/`): `wt/db-before-session28.sql`
  (the pre-cleanup dump), `wt/rb-s28-dom.html` (**42/42**), `wt/app-s28.log` (the boot lines),
  `wt/mailpit-s28b.log`.
- **Uncommitted:** unchanged from session #27 — the combined #26 + #27 set (the backend BR-5 fix,
  `tools/api_check.py` 98, `resptest/real-backend-check.html` 42, `CODE/HTML/hospital-requests.html`,
  the three docs, both report copies, and the new figure assets `tools/report/build_diagrams.py`,
  `build_montages.py`, `assets/shots/`, `fig1_1/fig3_2/fig3_5/fig4_1`). `origin/main` = `1c4ca14`.

### Hand-off — next session starts here
1. **The user’s own rehearsal is the one open item** (session #27 item 4): drive
   `docs/DEMO_RUNBOOK.md` §0 → §1 → §6 in the **browser** with IntelliJ’s ▶, now that 8081 is free,
   Mailpit is up and empty, and the DB is pristine. Two points to check *visually* because the
   API-level walkthrough cannot see them: the **hospital queue page** must show TUTH’s rows for a
   TUTH session, and **Mailpit’s inbox** should gain exactly one mail per registration.
2. **Commit and push the combined #26 + #27 set** (this session adds only `PROGRESS.md`).
3. Optional polish: the diagrams are dense at 6 pt (fewer flows/cases per figure), and the runbook’s
   §0 table could print the two new check counts side by side.
4. After the rehearsal, §0G / §5 of the runbook puts the demo data back.

## Session #27 (2026-09-17) — the walkthrough run (2 real defects fixed), the report gets its images and its diagrams, and the backend narrative stops saying "scheduled" ✅

Started as "read PROGRESS + the proposal and continue from there", so this session took the
live thread of session #26 hand-off item 4 — **finish the report images** — and closed it, plus
the accuracy problem the rebuild exposed.

### 1. The wiring that was missing (`tools/report/build_report.py`)
- **`FIG_IMAGES`** gained the five screenshot captions: Figure 4-2 →
  `shots/fig4_2_donor_search.png`, 4-3 → `shots/fig4_3_request_forms.png` (the two-form
  montage), 4-4 → `shots/fig4_4_donor_profile.png`, 4-5 → `shots/fig4_5_admin_panels.png`
  (the 2×2 panel grid), 5-1 → `shots/fig5_1_harness.png` (three stacked reports).
- **New `APPENDIX_IMAGES`, keyed by appendix title.** Appendix E and F were rendering
  **imageless** — their `[INSERT SCREENSHOT …]` lines had already been deleted, so nothing
  was left to render. E now carries Figure E-1 (the 5-1 montage, reproduced with a caption
  that says so); F carries F-1 landing, F-2 the auth pair, F-3 donor dashboard, F-4 hospital
  request queue, F-5 the legal row + 404.
- A `FIG_IMAGES` value may be **one `(file, width)` pair or a list of them**, and the new
  `add_image()` **prints a warning if an asset is missing** instead of silently emitting a
  captioned gap — a skipped figure is the failure mode that started this session.
- Widths are chosen so nothing overflows a text block (16.2 × 25.3 cm at these margins):
  the deepest asset is `fig4_3_request_forms.png` (2160×3014) at 13.5 cm ≈ **18.8 cm tall**.

### 2. Placeholders: 19 → 0 (the diagrams were drawn, not waited for)
`bb_content.py`'s screenshot placeholder lines for 4-2/4-3/4-4/4-5 are gone (the images
replace them), and **Figure E-1 / F-1…F-5 were added to `LIST_OF_FIGURES`** so the
front-matter list keeps matching the captions: verified **19 captions = 19 listed**, with no
entry in either direction unmatched.
That first pass left four bracketed placeholders. Rather than leave them for a human, they
are now **drawn by a new tracked tool, `tools/report/build_diagrams.py`** — Pillow only (no
Graphviz, no matplotlib, no browser — the same dependency `build_montages.py` already uses),
so the figures are reproducible from the repo with one command, and the script self-checks
before it saves:
- **Figure 1-1 (request lifecycle)** — the six-box flow (submit → PENDING → admin forwards →
  hospital queue → donor accepts/Matched → fulfil with stock −units), the guest `EM-` entry,
  the Cancelled/Rejected terminal branch, and a numbered legend that spells out each API call
  and the rule it carries (BR-1…BR-5).
- **Figure 3-2 (use case)** — five actors (Donor, Requester, Guest, Hospital Staff,
  Administrator) against 24 use cases on a BloodBuddy boundary, plus the register/log-in note
  and the 401/403 sentence from §3.9.
- **Figure 3-5 (Level-1 DFD)** — processes 1–6, five data stores (the nine tables collapsed to
  the five the flows actually touch), the SMTP server, and **18 numbered flows with a legend**
  (badges on the lines instead of labels on them — the label-per-line version was unreadable).
- **Figure 4-1 (project file structure)** — the last screenshot placeholder. A headless
  browser cannot photograph IntelliJ, so the figure is a **rendered tree of the actual
  repository** (backend packages, frontend modules, tools, resptest, docs) instead: honest
  evidence of the structure, with no human in the loop. Caption lost its "(IntelliJ IDEA)"
  qualifier and `LIST_OF_FIGURES` followed.
The self-checks are the point: each figure reports its size + ink coverage and warns on a
label that does not fit its box or **two boxes that overlap** — I cannot see the rendered PNG,
so the geometry is asserted instead. Type sizes are chosen against the printed page (19 px on
a 1460 px canvas at 16 cm ≈ 6 pt — the same order as the proposal's own diagrams), because the
first draft came out at 4 pt and would have been unreadable in print.

### 3. v2 rebuilt, Word-finalised, verified by diff
- Backed the old hand-in copy up **first** to `.tools/report-backup/v2-before.{docx,pdf}`
  (gitignored), then `python tools/report/build_report.py docs/BloodBuddy_Final_Report_v2.docx`
  and `python tools/report/finalize_word.py "D:/APJ_PROJECT/docs/BloodBuddy_Final_Report_v2.docx"`
  — **the absolute path matters**, a relative one fails inside Word COM.
- **Verification** (the session-#23 method): strip text from old and new, `difflib` the two.
  Every non-TOC difference is an intended one; the only other churn is TOC page numbers,
  which shift because the figures added pages. Images **5 → 16**; `[INSERT SCREENSHOT …]`
  **19 → 1**; `[INSERT DIAGRAM …]` 3 (unchanged); PDF **48 pages, 16 image XObjects,
  2.15 MB**.
- **The diff proved v2 had never received session #26's text corrections.** It still said
  "266 automated functional checks", "16/16 against the REST stub" and "Figure 4-1 … (VS
  Code)" — because §26.11 edited `bb_content.py` but the build was never re-run. Rebuilding
  is what put 268/268 · 92/92 · 39/39 and IntelliJ IDEA into the hand-in copy.

### 4. The report still described the backend as unbuilt (fixed — ~14 statements)
Session #26 fixed the *counts* and left the *narrative*. The report was contradicting itself
in the same breath:
- **§5.5**: "The remaining mandatory criteria belong to the scheduled backend phase and are
  not claimed as complete" — two pages after Table 5-1 shows TC-18 (39/39), TC-20
  (Postman) and TC-21 (SMTP) **PASS**.
- **§7.2 Limitations**: the backend "is the scheduled implementation phase", email "not yet
  sending, as delivery depends on the backend phase", the hospital APIs "designed and stubbed
  but not live", server-side guarantees "specified but not yet in force".
- **Table 2-2 / Table 3-3 / §4.1 / §4.3.2 / §4.3.4 / §4.5 / Table 5-2 / Ch6 / Table 6-1 /
  §7.3 / Appendix C** carried the same frontend-phase tense ("scheduled with the backend",
  "service-side auth scheduled", "implementation scheduled; design Chapter 3", Table 6-1 row
  13 "Scheduled (backend phase)", "Complete the Spring MVC backend" as future work, and
  Appendix C's "To be exported from MySQL Workbench during the backend phase").
Rewritten to what was actually built and **recorded**: Spring Security session login (BCrypt),
MySQL-compatible **TiDB Cloud Serverless** persistence, **Mailpit** SMTP with per-row
SENT/FAILED, **Newman** 76 requests / 107 assertions, and Appendix C now documents the
**nine real tables** (users, donors, hospitals, blood_inventory, blood_requests,
request_timeline, email_notifications, request_declines, donor_hospital_affiliation). §7.2's
limitations are now real limits (browser store survives only as a forced offline path, no
hospital integration feed, local mail catcher, in-request rather than queued sending, no DB
exclusion for two simultaneous accepts) instead of "not built yet". **This was an editorial
change to the user's own document; the full list is in the diff of `bb_content.py` (see #6).**
Two judgement calls worth naming: §4.1's "VS Code" became **IntelliJ IDEA** (the direction
§26.11 already took for Figure 4-1), and §7.3's first two "future scope" bullets were
replaced by genuine future work rather than re-worded to-dos.

### 5. Both copies rebuilt (v1 too) and proven identical
`docs/BloodBuddy_Final_Report.docx` was rebuilt as well, so the two copies agree:
**identical body text, identical tables (167 rows), identical captions, 20 images each, zero
placeholders, 51 pages** — verified by diffing the stripped text (TOC page numbers excluded)
and the table/caption sets. (The PDFs carry 19 image XObjects because the 5-1 montage is
referenced twice — Figure 5-1 and Figure E-1 — and Word stores it once.)

### 6. The walkthrough (hand-off item 5) — run end to end, from the outside
Mailpit up, DB pristine, 8081 free, then the app booted from the runbook's own recipe; the log
carried exactly the expected line (`Demo data ready: 21 users, 14 donors, 5 hospitals, 40
inventory rows, 4 requests, 3 notifications`, `Tomcat started on port 8081`). Every §1
checkpoint was then verified **over HTTP and in TiDB**, not by looking at a screenshot:

| §1 checkpoint | what was actually done | result |
|---|---|---|
| 0:00 landing + responsive | `GET /CODE/HTML/landing.html`; `responsive-check.html`; `qa-harness.html` | **200**, **92/92**, **268/268** |
| 1:00 register (§2.4) | `POST /api/auth/register` as a new donor | **201**; TiDB row `DONOR/ACTIVE` + donor `O_MINUS`; Mailpit went **0 → 1** with *“Welcome to BloodBuddy — confirmation for …”*; `email_notifications` row **SENT** with `sentAt` set |
| 4:00 requester | login `sita.g`, `GET /api/donors/search?blood=B+&district=Kathmandu` | 5 real donor rows; **widen fallback** (`O-` in Dharan → `widened:true` + *“showing O- donors in all districts”*); `POST /api/requests` **201** with the timeline line *“Request submitted — 11 compatible donors notified”*; tracker showed 3 rows; inbox filled with *“Urgent B+ request in Kathmandu”* per compatible donor |
| 7:00 donor | login `arun.s`, `GET /api/requests` (his board), `PATCH {status:Accepted}` | Board listed only requests a B+ donor may serve — **the O− request (`BB-5MN8VX`) was correctly NOT offered** (the chart read the right way); accept **200** → `Matched`, donor stamped, `donorAt` set, timeline line added; requester got *“BB-CUT9Z7 — a donor has accepted your request”* |
| 9:30 hospital | login `bloodbank@tuth`, inventory read, `PUT /api/hospitals/inventory/B+`, `PATCH {status:Fulfilled}` | 8-group board; **12 → 13** via the API; queue held TUTH's own rows only; fulfil **200** → `Fulfilled` + *“2 units of B+ issued…”* and stock **13 → 11** (BR-4 arithmetic is the API's) |
| 11:30 admin | login `saksham`, `GET /api/admin/users?page=0&size=4`, suspend, `?unrouted=true`, forward | paged envelope (22 users, 6 pages); suspend **200** → that account's next login **refused with its reason** (409); forward to Patan **200** + timeline *“Forwarded to Patan Hospital, Lalitpur by platform admin”* |
| 13:30 guardrails | anonymous / wrong-role calls | `GET /api/admin/users` → **401** anonymous, **403** as the requester, both in the same `ApiError` shape |
| 14:30 tools | Newman, real-backend-check, `api_check.py`, smoke run | **76 · 107 · 0**; **39/39** (then 42/42, see §7); **95/95** (then 98/98); **31/31** exit 0 |
Everything else in the runbook’s §0 table reproduced too (harness 268/268, responsive 92/92).

### 7. The walkthrough found TWO real defects — both fixed, both pinned
Neither was visible from reading the code; both came out of doing the 9:30 checkpoint for real.
1. **BR-5 was not enforced on the generic queue endpoint.** `PUT /api/hospitals/8/inventory/B+`
   answered **409** as it should, but `GET /api/requests?hospitalId=8` — *the same data behind a
   query parameter* — answered **200 with Patan's queue** to a TUTH session (and with no filter
   at all it returned **the whole table**). `RequestController.list` now pins a hospital actor
   to its own hospital: another hospital's id or label → 409, the admin-only views
   (`?guest`, `?unrouted`, `?requester[Id]`) → 409, and **no filter means “my queue”**. Admin
   behaviour is untouched (it may address any hospital) and requesters/donors are untouched.
2. **`hospital-requests.html` carried the demo's single hospital identity into real mode.**
   It hardcoded `HOSPITAL = 'Bir Hospital, Kathmandu'` and asked the API for *that* queue — so a
   TUTH session rendered **Bir's** rows, which is the opposite of what the runbook tells the
   user to say, and after fix (1) the same call is refused. The page now asks with **no hospital
   at all** and lets the server answer with the session's own (mock mode keeps the demo label).
**Pinned so neither can come back:** `tools/api_check.py` **95 → 98** (own queue with no filter,
`?hospitalId=<other>` → 409, the admin-only views → 409; the pre-fix server fails the first
and answers 200 to the second — that is the negative test) and `resptest/real-backend-check.html`
**39 → 42** (the API's own-queue rule, the **page's rendered card ids ⊆ the signed-in hospital's
queue**, and “no other hospital's request appears on it” — the pre-fix page showed Bir's).
Re-verified after the fixes: **42/42**, **98/98**, **Newman 76 · 107 · 0**, **qa-harness 268/268**
— no regression anywhere, including the Postman collection’s own hospital folder.

### 8. Numbers rippled, then the demo state was put back
Every quoted count updated where it is claimed (`tools/api_check.py`’s header, the runbook’s §0
/ §3 / §6 and its Q&A, `backend/README.md`, and the report’s TC-18 / Table 4-1 row 12 /
Appendix E / Figure 5-1 caption / LIST_OF_FIGURES / Chapter 7). The Figure 5-1 **screenshot was
re-captured** (`node .tools/shots/capture.js fig5_1c` → the page now reads 42/42) and the montage
re-composed, so the picture and the caption cannot disagree. Both reports were rebuilt again.
Finally the walkthrough’s own footprint was removed with the §5 recipe, one statement at a time,
and verified by re-reading every count: **21 users / 14 donors / 5 hospitals / 40 inventory /
4 requests / 3 notifications / 7 timeline / 39 affiliations / 0 declines / 0 donor photos**,
TUTH B+ = **12**, `BB-5MN8VX` **PENDING + unrouted** (`E28094`, notes NULL), the seeded four
requests back to their seeded statuses, the only `SUSPENDED` account still the seeded Bikash
Tamang. Mailpit’s inbox was **emptied** (it held the walkthrough’s ~100 mails) and the app was
**stopped** — 8081 was free again.

### 9. The confirmation run from pristine — and the state the session was stopped in
Both BR-5 fixes were then re-tested **from a pristine database**, not just from the walkthrough’s
own leftovers: the app was restarted against the restored DB and the checks re-run. `tools/api_check.py`
came back **98 passed, 0 failed** (`.tools/wt/api_check_final.log`), the three new BR-5 checks
included, and the Newman collection was green earlier in the session (**76 requests · 107
assertions · 0 failures**). The *browser* half of that confirmation — `resptest/real-backend-check.html`
run against the pristine DB — is the step that **was still in flight when the session was stopped**:
`.tools/wt/final-dom.html` is 0 bytes, so the page’s **42/42** stands only from the pre-restore run
(`.tools/wt/rb3-dom.html` reads 42/42).
Consequently the stop left **the app RUNNING and the DB holding that run’s footprint**, not pristine
— the exact counts and the accounts to remove are in the block below, and the cleanup
SQL for the three `api-check+…` accounts is printed at the tail of `.tools/wt/api_check_final.log`.
**All of it was done in session #28** (and the one row that list missed — the browser check’s
`b7check+…` account — was removed with it).

### State at the stop — superseded, and now closed
**Session #28 stopped the stale app, restored the database and finished the interrupted
confirmation; the live state is in “Session #28 — State at session end”.** What the stop left was:
Spring Boot running on **8081 (PID 21268)**, the DB carrying the confirmation run (**25 users / 16
donors / 7 requests / 30 notifications / 13 timeline**, TUTH B+ = **15**, `BB-5MN8VX` re-pointed at
**NMC / 10**), Mailpit holding **27 messages**, and the rows to remove listed as accounts
**270026–270029** (`api-check+…`, `b7check+…`), donors **270012/270013** and requests
**`EM-L6AVFN` / `BB-9EK9LM` / `BB-4TTYVZ`** — with the note that the printed cleanup SQL did not
match the browser check’s `b7check+…` account and that §8’s em-dash label had been re-pointed. Every
one of those items was done in session #28, and the em-dash label (**`E28094`**) re-applied.

## Session #26 (2026-09-17) — the Postman collection is GREEN under Newman, twice, and the DB is back ✅

Session #25 ended mid-verification: the collection had been fixed (60 → 76 requests,
14 role sign-ins) and the second run's one failure had turned out to be a **real API
bug** (`?status=` alone was ignored) which was fixed — but **the third run was never
completed, so 0 failures was not proven**. This session closed that, and the answer
is the same one session #21 should have produced: **the collection was never broken
JSON, it was broken when executed** — 0 failures is only a fact because it was run.

### 1. The run — 76 requests, 107 assertions, 0 failures
```
npx --yes newman run docs/postman/BloodBuddy.postman_collection.json
```
Run against the booted app (8081) with Mailpit up (8025), from a database restored to
the known-good state first. Result: **76 requests · 107 assertions · 0 failed · exit
0**, 2m01s, average response 1519ms (full log: `.tools/newman-run1.log`). No
`requestError` anywhere in the log — the `√` count is 107, matching the assertion
table, so nothing is being silently skipped.
- **The `?status=`-only bug is now covered by a green run**, not just by a diff: the
  collection carries `Requests · GET /api/requests?status=Pending (narrow any queue)`,
  which is exactly the request that failed in session #25.

### 2. It is re-runnable — a second consecutive run is also 0 failures
Run 2 was started **without cleaning run 1's leftovers**: **76 requests · 107
assertions · 0 failed** again (`.tools/newman-run2.log`). That matters because the
session-#25 failure mode was a run leaving state behind (an account left suspended
broke every later sign-in as it; one run fulfilled a demo request). The two fixes for
that — the sign-in-per-folder pattern and folder 02's restore — hold up under exactly
the condition that used to break it. So the runbook can say "run it as many times as
you like" instead of "restore first".

### 3. The database was NOT pristine at the start (and is now)
The session-#25 runs had left a real footprint, which is worth recording because
nothing in PROGRESS said so: **23 users / 15 donors / 8 requests / 33 notifications /
14 timeline / 1 decline**, plus a **68-byte photo on Arun (the seeded donor)** — the
collection's §2.5 upload targets the seeded `donorId`, and `api_check.py` deliberately
avoids that by uploading to its OWN throwaway donor.
- Restored **before** the run, then restored **again after it**, with the same list,
  and verified by re-reading every count: **21 users / 14 donors / 5 hospitals / 40
  inventory / 4 requests / 3 notifications / 7 timeline / 39 affiliations / 0
  declines**, TUTH B+ = 12, `BB-5MN8VX` **PENDING + unrouted** (`hospital_id IS NULL`,
  `HEX(hospital_label) = E28094`), **0 donor photos**. The one `SUSPENDED` account is
  **Bikash Tamang** — deliberately seeded that way for the admin-suspend demo.
- **Why the restore is mechanical:** TiDB's IDENTITY jump separates the demo data from
  everything a run creates (demo requests are 1–4, runs start at 210021+; demo users
  are 13–33, run accounts are 210022+), and the accounts are named `postman%`.
  `.tools/dbq.sh` (new, gitignored) wraps the client with the credentials read
  from `backend/.idea/workspace.xml` — one statement per invocation, and it never
  prints the password.

### 4. Docs updated (the §2.6 caveat is gone)
- **`docs/DEMO_RUNBOOK.md`** — §0's *"§2.6 caveat — the collection does not currently
  pass (15 assertions fail)"* is **deleted** and replaced with the verified numbers +
  the re-runnable note; the operator's *"every number, and how to reproduce it
  YOURSELF"* table gained a **Newman row**; §14:30 gained the headless command next to
  the Postman GUI instructions; the artifacts table, the *"numbers you can quote"*
  table and the 60-second-version closing line now carry **107 assertions, 0
  failures**. **§5 (put the demo data back) gained the Postman footprint block** — the
  11 statements above, in order, with the "one statement at a time" trap restated and
  the `E28094` em-dash warning (session #22's bug) kept next to it.
- **`backend/README.md`** — the §2.6 paragraph now says **76 requests, verified
  end-to-end under Newman (107 assertions, 0 failures), safe to run twice**, and names
  the `Sign in as …` pattern as the reason one run can cover four roles.
- No product code changed this session — `RequestController`'s `?status=` fix from
  session #25 was already in place and is now *verified* rather than *compiled*.

### 5. The regression check — `api_check.py` 94 → 95
**`tools/api_check.py`** now runs the lone-filter case through the *other* harness as
well, so the bug cannot come back unnoticed:
> `?status=Pending on its own -> ONLY Pending rows (a lone filter is not ignored)`

It sits after the unfiltered admin list and asserts **both halves** — every row is
`Pending` **and** there are strictly fewer of them than in the unfiltered list —
because "returns some Pending rows" would also have passed on the buggy version.
Result against the booted app: **95 passed, 0 failed.**
**Negative-tested:** the same payload equality was fed the unfiltered table (what the
old code answered) and the predicate reported **False**, while the real filtered
payload passes — so the check fails on the bug rather than merely passing on the fix.
Counts updated where they are quoted: runbook §0's table, §3's table of numbers, §6's
closing line and `backend/README.md`.
The run's own footprint (guest `EM-X8SHQR`, member `BB-VK9Z5S`, 2 `api-check+…`
accounts, its photo, and `BB-5MN8VX` forwarded to NMC) was cleaned with the SQL the
script prints, and the pristine counts were re-verified afterwards.

### 6. Committed and pushed
**`1c4ca14`** — *"Prove the Postman deliverable by running it, and guard the bug it
found"* — 6 files, +894/−10, pushed to `origin/main` (`45f8785..1c4ca14`). It carries
session #25's four files (`RequestController`'s `?status=` branch, the 76-request
collection, runbook §0, the uncommitted state PROGRESS had been warning about) **and**
this session's docs plus the new check. Nothing in `backend/.idea/` or `.tools/` is
tracked, so no credentials moved.

### 7. The demo was staged for the user, then deferred
Per the user's request the start line was set up and left there: **Mailpit's inbox
emptied** (it held 398 mails from every test run — at the defence, "the welcome mail
arrived" is far stronger when it is the *only* mail there), **the app process on 8081
stopped** (java PID 21708) so IntelliJ's ▶ is free, and the DB re-verified pristine one
last time so the boot is the only unknown.
- The hand-over was written out as **checkpoint → what good looks like → what it
  proves** for all nine click-script steps, plus the two traps: don't press ▶ while
  8081 is held (Tomcat fails *after* Hibernate has run, which looks like a bug), and
  Mailpit must be up **first** or the registration writes `FAILED` instead of sending.
- **The user then chose not to rehearse it today**, so the dry run did **not** happen:
  the script is written and the start is staged, but nobody has walked it end to end
  in the browser against live TiDB. It is still outstanding (hand-off item 5) and the
  honest label is still `[rehearse]`.

### 8. The report's screenshots — there are 18 placeholders, not 9 (half done)
Asked to "fill the report's 9 screenshot placeholders", the first check was the claim
itself: **both DOCX files carry 18** (`[INSERT SCREENSHOT…]`, across 11 figures), and
the "9" in the runbook was stale. They split three ways — 8 public pages, 7
role-restricted pages, 3 self-driving harness pages.

- **Captured from the running app, all 19 pages**, into
  `tools/report/assets/shots/` (tracked). Tooling is gitignored in `.tools/shots/`:
  `puppeteer-core` 25.11.0 driving the installed Chrome, plus `capture.js` (the 19
  targets), `probe.js` (which page a role actually lands on), `fails.js` (the failing
  lines from a check), `netprobe.js` (every `/api/` response with its frame) and
  `iframeprobe.js` (one page in one iframe).
- **The session's key trap, learned the hard way:** a screenshot of a role page needs
  **both** halves of the session — the `JSESSIONID` cookie **and** a seeded
  `localStorage.bb_session`. `nav.js`'s `RBAC_GUARD` bounces on the *cached* session at
  load time, so a fresh browser context with only the cookie lands on `auth-login.html`.
  The first pass produced **byte-identical images per role** (three requester pages =
  one file) which is what exposed it; `node probe.js` then showed `-> auth-login.html
  <-- BOUNCED` for all nine. Seeding via `evaluateOnNewDocument` fixed it.
- Harness pages are waited on by their own `document.title` (`QA-DONE n/n` etc.) and
  then scrolled to the verdict line, so the picture shows a number rather than a log.
- **`tools/report/build_montages.py` (new, tracked)** composes the grouped figures —
  Figure 4-3 (both request forms), Figure 4-5 (four admin panels, 2×2), Figure 5-1
  (three reports), Appendix F's auth pair and legal row — and prints **size + "ink"
  coverage for every capture**, so a blank or error page cannot slip in silently.
  All 19 measure 2.4 %–29.7 % ink: real pages, none blank.
- **One placeholder cannot be filled by a browser:** Figure 4-1 asks for an IDE
  project view. It is left as a (now correctly worded) placeholder for the user —
  see the hand-off.

### 9. Two real defects found on the way (both fixed)
Neither was the point of the session; both came out of *running* things rather than
reading them, again.
1. **`resptest/real-backend-check.html` was flaky and read 35/39.** Every API call
   made *inside* its iframes was `net::ERR_ABORTED` (proved with `netprobe.js`),
   because the page slept a fixed **1200 ms** and then removed the iframe while its
   fetch was still in flight — TiDB Cloud is answering in **~1.6 s per call** here.
   The API was correct all along; the *check* was racing it. Now it **waits for the
   rows** (20 s poll) and polls the login notice instead of guessing: **39/39**.
2. **The Postman collection's `memberCode` variable defaulted to `BB-5MN8VX`** — a
   *seeded demo request*. `requestCode` was left empty (correct) but this one was not,
   so any run where the capture did not happen first silently **PATCHed the demo row's
   notes**. Session #25 hit exactly that and its restore never checked the column.
   Default changed to `''`; re-ran Newman — **76 requests / 107 assertions / 0 failed**,
   and this time the patch landed on the run's own request (`BB-PTPE9D`) while
   `BB-5MN8VX` kept `notes IS NULL`. The demo row's stale note is restored to NULL.

### 10. A gotcha worth keeping (it cost real time here)
`tools/sync_frontend_to_backend.py` writes **`src/main/resources/static/`**, but a
**running** app serves **`target/classes/static/`** — `spring-boot:run` copied the
resources into `target/` when it started. Editing a page and re-syncing changes
nothing until the resources are recopied or the app restarts; a fixed check kept
failing here because the browser was still being served the old file. (Copied the
tree across by hand to avoid a restart mid-capture.)

### 11. Report text: a numbers-accuracy pass (`tools/report/bb_content.py`)
The user asked for three corrections — and the honest radius of them turned out wider,
because the surrounding claims were still from the frontend-only phase:
`266/266 → 268/268` (and TC-19's `266→264 → 268→266`); the real-mode/`16/16`-stub
evidence replaced by the **real-backend check, 39/39**; **Figure 4-1 "VS Code" →
IntelliJ IDEA** (caption, placeholder and LIST OF FIGURES). Beyond those: **TC-20
(Postman) and TC-21 (SMTP) were "Pending (backend phase)" → PASS**, **FR-12/FR-13
("Spring Mail scheduled", "BLOB storage scheduled") → implemented**, the Chapter 7
conclusion and Appendix E's tool list now carry the real figures (95 HTTP checks,
31 service-layer, 76 Postman requests), and Figure 5-1's caption became "Automated
Verification Reports (268/268 · 92/92 · 39/39)". Leaving "scheduled" next to a
39/39 screenshot would have contradicted itself.

### Hand-off — next session starts here
1. ~~Finish the Postman verification + clean its footprint.~~ **DONE here.**
2. ~~Add the `?status=`-alone check to `tools/api_check.py`~~ **DONE here (94 → 95).**
3. ~~Commit + push.~~ **DONE here — `1c4ca14` is on `origin/main`.**
4. ~~**FINISH THE REPORT IMAGES.**~~ **DONE in session #27 (§1–§4 there)**: wiring, appendix
   mechanism, rebuild and verification all landed. What is *left* of this item is 4d only
   (Figure 4-1 needs a human). Original text:
   The captures exist and the text is fixed; what is missing is the wiring and the rebuild:
   a. *(DONE #27 — `FIG_IMAGES` + `APPENDIX_IMAGES`)* **`tools/report/build_report.py` had no
      entry for any screenshot.** It needs
      `FIG_IMAGES` entries for the screenshot captions (Figure 4-2 →
      `shots/fig4_2_donor_search.png`, 4-3 → `shots/fig4_3_request_forms.png`, 4-4,
      4-5 → `shots/fig4_5_admin_panels.png`, 5-1 → `shots/fig5_1_harness.png`) **plus
      an appendix mechanism**: `bb_content.py`'s Appendix E and F placeholder lines
      have already been **removed**, so until that lands both appendices render their
      prose with **no images at all** (E wants `fig5_1_harness.png`; F wants landing,
      the auth pair, donor-dashboard, hospital-requests, the legal row — captions
      included).
   b. *(DONE #27 — rebuilt and finalised; v1 deliberately left older)* **Rebuild v2 ONLY** (the hand-in copy):
      `python tools/report/build_report.py docs/BloodBuddy_Final_Report_v2.docx` then
      `python tools/report/finalize_word.py <ABSOLUTE path to that docx>` (Word COM
      fills the TOC and writes the PDF; a *relative* path fails there). **Back the
      current v2 up first**, then compare the stripped text to prove only the intended
      changes happened (session #23's method).
   c. *(DONE #27 — 19→1 screenshot placeholders, 16 images, 48-page PDF, 19 captions = 19 listed)*
      **Verify:** no `[INSERT SCREENSHOT` left in the built docx, the images present,
      page count sane, LIST OF FIGURES matching the captions.
   d. **Figure 4-1 needs a human** — an IntelliJ project-view screenshot, or drop the
      figure. A headless browser has no IDE, so this one is not automatable.
   e. Re-capturing later is one command: `node .tools/shots/capture.js [name-filter]`
      with the app up on 8081 (and remember §10 before blaming the page).
5. ~~**Then the walkthrough.**~~ **DONE in session #27 (§6–§8 there)**: run end to end
   against live TiDB, every checkpoint verified over HTTP + SQL, **two real defects found and
   fixed**, the suites extended to pin them, the DB restored and Mailpit emptied afterwards.
   What is *left* of this item is the user’s own rehearsal — driving it in the **browser**
   (§0 → §1 → §6) rather than the API, which is what the `[rehearse]` labels used to mean and
   is now marked `[tool-verified]` at both checkpoints it covered. Start the app in IntelliJ
   (**8081 is free**), Mailpit is running with an empty inbox, the DB is pristine.
6. **Commit** the four modified files and the two new report assets (see the state
   below) — nothing from this stretch is committed yet.

### State at session end
- **App STOPPED, 8081 free** — stopped on purpose so a fresh IntelliJ ▶ is one click.
- **Mailpit up on 8025 with an EMPTY inbox** (0 messages, cleared deliberately).
- **DB pristine**, re-read after every run this session: 21 users / 14 donors / 5
  hospitals / 40 inventory / 4 requests / 3 notifications / 7 timeline / 39
  affiliations / 0 declines, TUTH B+ = 12, `BB-5MN8VX` `PENDING / —` (`E28094`), 0
  donor photos. The one `SUSPENDED` account is Bikash Tamang (seeded that way).
- `origin/main` = **`1c4ca14`**. **Uncommitted** (all of it from the report-screenshot
  stretch): `PROGRESS.md`, `docs/postman/BloodBuddy.postman_collection.json` (the
  `memberCode` default fix §9.2), `resptest/real-backend-check.html` (the wait-for-rows
  fix §9.1), `tools/report/bb_content.py` (§11); **untracked**:
  `tools/report/assets/shots/` (24 PNGs — 19 captures + 5 montages) and
  `tools/report/build_montages.py`.
- `.tools/shots/` holds the gitignored capture tooling **and its `node_modules`**
  (puppeteer-core); `.tools/dbq.sh` is the one-statement SQL helper. Logs:
  `.tools/newman-run{1,2,3}.log`, `.tools/api_check-run.log`,
  `.tools/shots/{capture,harness}.log`. Credentials are never printed by any of them.
- **`backend/target/classes/static/` was refreshed by hand** (a build artifact,
  gitignored) so the captures saw the fixed check page — see §10.

## Session #25 (2026-09-17) — the Postman collection was run for real (and found an API bug) 🔶

This session began as "teach the user to run the defence alone" and turned into the
most valuable kind of check: **the §2.6 deliverable had never actually been executed.**
Session #21 validated its JSON statically and said so — that is exactly the gap that
bit here.

### 1. Run it, and watch it fail — 15 of 91 assertions
```
npx --yes newman run docs/postman/BloodBuddy.postman_collection.json
```
60 requests executed, **15 assertions failed**. The cause was structural, not a typo:
`01 · Session` logs in as **one admin account**, but folders 02–99 need **different
roles** — the request names say so themselves ("Lifecycle 1 · PATCH as ADMIN", "… as
DONOR", "… as HOSPITAL"). Newman also carries the cookie jar across the whole run, so
one session cannot serve them all. `memberCode` was referenced in 05 and 98 but **never
captured** (it held a stale value, which is why a run PATCHed `BB-5MN8VX` and, as
admin, **fulfilled a demo request**).

### 2. The fix — role sign-ins, a real `memberCode`, and a restore
- **14 `Sign in as …` requests** inserted where the role changes: 04 requester;
  05 admin → donor → hospital → donor → requester (one per event); 06 hospital;
  07 donor; 08 hospital then admin; 98 donor(A+) → donor(A) → hospital(**Patan**, a
  DIFFERENT hospital, so BR-5 is a genuine cross-hospital refusal) → requester.
  Each one captures `sessionEmail/sessionRole/userId/donorId/hospitalId` and clears
  the id variables it does not set, so no stale id leaks into a later folder.
- **A second member request in 04** (`… → memberCode, left Pending`) — a dedicated
  PENDING B+ fixture for the §98 refusals, kept out of the lifecycle so it is not
  terminal by the time BR-1/BR-5 ask.
- **02 gained a restore** after its suspend test: a run could otherwise leave a user
  suspended and break every later sign-in as that account.
- Collection **60 → 76 requests**.

### 3. The second run found a REAL API BUG (fixed)
**107 assertions, 1 failed** — and it was the server's fault, not the collection's:
> `GET /api/requests?status=Pending` returned **every** request.

`RequestController.list` only passed `status` into the filtered branches; with no
other filter it fell through to `requests.all()`, so **a lone `?status=` was silently
ignored**. That is a genuine defect the static validation could never have caught.
Fixed with a `status`-only branch that calls `requests.forAdmin(status, false, false)`.
Compile clean; app restarted. **A `?status=`-alone check belongs in `tools/api_check.py`
as well — do that next session.**

### 4. The operator's checklist (the reason the session started)
`docs/DEMO_RUNBOOK.md` gained **§0 — "Run it yourself"**: the two boot commands with

their expected log lines, the three tabs, **every number with how to reproduce it
solo**, a "if it looks wrong" table (port busy, Mailpit down, cached harness, DB
connection, suspended user), how to shut down, and how to restore the data. Written
because the user will face the defence **with no assistant**. It also carries the
honest §2.6 caveat until the Newman run is green. Dependencies were verified present:
Python 3.13, Java, Node/npm/npx, Chrome, IntelliJ 2026.1 and **Postman desktop**
(`%LOCALAPPDATA%\Postman\Postman.exe`). Newman is not installed but `npx newman` works.

### 5. Database + repo state at session end
- Demo DB **pristine**: 21 users / 14 donors / 5 hospitals / 40 inventory / 4 requests /
  3 notifications / 7 timeline / 39 affiliations, `BB-5MN8VX` **PENDING and unrouted**
  (hex `E28094`), TUTH B+ = 12. A stray `STATUS_UPDATE` notification (id `210106`, to
  Kiran, claiming `BB-5MN8VX` was fulfilled) was deleted — the Postman run had
  genuinely fulfilled it, and its status/timestamps were reset too.
- **The app is RUNNING on 8081** (started for the walkthrough). Stop it before a fresh
  IntelliJ ▶.
- **Session #25 changes are uncommitted**: `backend/.../web/RequestController.java`
  (the `?status=` fix), `docs/DEMO_RUNBOOK.md` (§0), and
  `docs/postman/BloodBuddy.postman_collection.json` (76 requests).

### Hand-off — next session starts here
1. **Finish the Postman verification**: app up, then
   `npx --yes newman run docs/postman/BloodBuddy.postman_collection.json` → **expect 0
   failures**. Fix whatever is left, then **clean the run's footprint** (it creates
   Postman-flagged accounts + requests and can set a photo on a seeded donor).
2. **Add the `?status=`-alone check to `tools/api_check.py`** (94 → 95) so the bug just
   found cannot come back, and update the count in README/RUNBOOK if it changes.
3. **Commit + push** the session #25 changes (the user asked for the code to be pushed;
   `45f8785` is what is on `origin/main` today).
4. **Then the walkthrough** — with §0 of the runbook as the script, since the user must
   drive the defence alone.

## Session #24 (2026-09-17) — the proposal gaps: two closed, one recorded ✅

The user asked the direct question — "is everything backend related complete?" — so
this session checked the code against **CPJ119 §2–§4** and the **proposal's Appendix B**
rather than against PROGRESS's own claims. Mandatory CPJ119 was already met; three
things the proposal promised were not. Two are closed here; the third is a deliberate
choice now written down.

### 1. Server-side pagination (proposal §1.4, CPJ119 §3) — CLOSED
- **`dto/PageResponse.java` (new)** — `items/page/size/total/totalPages/hasNext`,
  zero-based like Spring Data's `Pageable`.
- **Additive on purpose.** `GET /api/donors/search`, `GET /api/admin/users` and
  `GET /api/requests` take optional `page`/`size`: **with** `?page=` they answer the
  envelope; **without** it they answer the bare array every existing page, the QA
  harness and the Postman collection already consume. That is what makes this a
  conformance fix rather than a frontend rewrite — the `bb-paginate.js` pager keeps
  slicing client-side, and the 268/39 green suites are untouched by construction.
- Slice-after-filter (the user list's `q` filter and the donor widen fallback are
  computed in Java), so the window is computed server-side but the filtered set is
  loaded first. DB-level `Pageable` is the scale upgrade; stated, not implied.

### 2. The Appendix B hospital-scoped "local API" (proposal §1.3/§1.4, a
"core deliverable") — CLOSED
Six routes existed only under other names or not at all. Every one added is a **thin
delegate** onto a service that already existed, so **no logic moved**:

| Appendix B | now served by |
|---|---|
| `POST /api/hospitals/inventory` | `HospitalController.postBoard` → `InventoryService.setUnits` |
| `PUT /api/hospitals/{hospitalId}/inventory/{bloodGroup}` | `HospitalController.setUnitsAt` |
| `GET /api/hospitals/{hospitalId}/donors/nearby` | → `DonorService.search(blood, hospital.district, true)` |
| `POST /api/hospitals/{hospitalId}/requests` | → `RequestService.create` (hospital from the PATH) |
| `GET /api/hospitals/{hospitalId}/requests/{requestId}/status` | → `RequestService.get` + a queue-ownership check |
| `POST /api/admin/users/{id}/deactivate` | → `AdminService.setStatus(id, "Suspended")` |

- **BR-5 travels with the path.** `requireScopedAccess(hospitalId, actor)` lets an
  administrator name any hospital and a staff account name **only its own** (a staff
  account with no hospital link is refused rather than silently treated as admin).
  Verified over HTTP: staff on another hospital → **409**, admin on the same id → 200.
- `POST /{hospitalId}/requests` builds a fresh `RequestCreateRequest` with the body's
  hospital **overwritten by the path** — the same "identity never from the body" rule
  the request service already keeps for the donor and hospital.

### 3. Literal `DELETE` endpoints — NOT added (recorded)
CPJ119 §2.3 says "standard CRUD … via Hibernate". There is still no `DELETE` route:
the "D" is soft-delete (`PATCH status=Cancelled` / `Suspended`), which is the right
behaviour for a request that must stay auditable and an account that must not vanish.
Real Hibernate deletes exist only in the seeder and the smoke runner's cleanup. This is
a deliberate design choice to state in the defence, not an omission to hide.

### Verification
- **Compile clean** (bundled Maven).
- **`tools/api_check.py` extended 81 → 94 checks** — 11 new: the six Appendix B routes
  (including the BR-5 refusals: another hospital's request → 404, cross-hospital PUT →
  409) and four pagination checks that assert *both* halves — the envelope when asked
  **and** that the array is unchanged when not. **94 passed, 0 failed.**
- **Smoke run 31 checks, 0 failed, exit 0** (`backend/target/bb-gaps-smoke.log`) — the
  controller-only change did not disturb the service layer.
- **Postman collection 50 → 60 requests** in a new folder `08 · Appendix B — the
  hospital-scoped local API + pagination`, statically re-validated the session #21 way:
  JSON parses, no undefined `{{variables}}`, **every path matches a declared controller
  route**.
- **Demo DB restored pristine**: 21 users / 14 donors / 5 hospitals / 40 inventory /
  4 requests / 3 notifications / 7 timeline / 39 affiliations, `BB-5MN8VX` unrouted
  (`HEX = E28094`), TUTH B+ = 12; app stopped, **8081 free**.

### A real test defect this session caught
The three Donor ↔ Hospital M:N checks in `api_check.py` picked `cards[0]` as the
"seeded donor". They passed on a clean database and **failed on a dirty one**: an
`api-check` donor left by a previous run registers with no affiliation and its name
("API Check") sorts ahead of the seeded ones. Fixed by naming the fixture (**Arun
Shrestha**) instead of trusting result order — the same class of mistake as the B3
hardcoded `id=1`, in a test rather than the product.

### What is still open
- Pagination is server-side but the **pages still slice client-side**; wiring
  `requester-search`/admin onto `?page=&size=` is a visible improvement, not a
  conformance fix (it would re-open the 268/39 frontend suites).
- No `DELETE` routes (above), CSRF disabled, and a suspended account's live session
  is not force-expired — all recorded.
- The last session-#22 list item stands: **teach the user to operate the system for
  the defence.**

## Session #23 (2026-09-17) — the hand-off list worked: harness re-run, check extended, docs + reports updated, DB restored ✅

This session took the session-#22 "next session starts here" list and worked it top
to bottom (items 1–4). No product code changed — every edit was to a harness, a doc,
or the report source, so the green suites were left green.

### 1. The mock QA harness re-run — 268/268
`resptest/qa-harness.html` was run with a **fresh** `--user-data-dir` and a `?v=22`
cache-buster (the documented trap: a reused profile reports stale numbers). Title:
**`QA-DONE 268/268`**. The two occurrences of the string `FAIL` in the dumped DOM
are the harness's **own render source** (`'FAIL '` inside the two `results.map(...)`
lines), not results — `grep -c` is not the check, the title and the `PASS`/`FAIL`
per-line list are. This is the run that covers the `bb-api.js` and
`auth-register.html` edits from after session #20.

### 2. `real-backend-check.html` extended for the two new surfaces — 39/39
Seven checks were added to the 32 that already passed:
- **The Donor ↔ Hospital M:N read (4 checks):** a donor session exposes its
  `donorId`; `GET /api/donors/{id}/hospitals` answers **200 with an array**; every
  item carries the `HospitalOption` shape (`id`/`label`/`shortName`); and Arun
  (Kathmandu) is affiliated with **exactly the 4 Kathmandu partner hospitals**
  (Bir, Manmohan, NMC, TUTH) — a number that would differ under a 1:N and is the
  point of the relationship.
- **The staffed-hospital link (3 checks):** a hospital account registered **by the
  name `"TUTH"`** returns **201**, its `hospitalId` is set (BR-5 scoping active),
  and its `hospitalLabel` contains **"Tribhuvan"**.
Result against the booted app on 8081: **`REAL-BACKEND-DONE 39/39`** (log
`resptest/real-backend-dom.txt`). The page prints the SQL to undo the rows it
creates (one donor account, one staff account, one request) and that cleanup was
run this session (see 4).

### 3. Docs + the report
- **`backend/README.md`** — real-backend-check **32 → 39/39** (and what the new
  checks prove), Postman **48 → 50**, `api_check.py` **74 → 81**, smoke **28 → 31**,
  the donor route list now names **`{id}/hospitals`**, and the `model/` section
  documents the M:N join table + `HospitalResolver`.
- **`docs/DEMO_RUNBOOK.md`** — all four counts updated (39/39 · 31/31 · 81/81 ·
  50 requests), the known-good-state line and the restore query now include the
  **39 affiliation rows**, a new **"What relationships does the data model use?"**
  Q&A answers 1:1 / 1:N / M:N with the affiliation, and the **"Anything not
  finished?"** answer no longer claims a newly registered hospital account is
  unlinked (session #22 fixed that).
- **`tools/report/bb_content.py`** — Table 3-1 gained the
  **`Donor ↔ Hospital (affiliation)` / M:N** row, and the compatibility row's note
  now says it is a **rule, not a physical join table** (so the two M:N rows cannot
  be read as the same thing).
- **Reports regenerated.** `build_report.py` + `finalize_word.py` rebuilt
  `BloodBuddy_Final_Report.docx/.pdf`. `BloodBuddy_Final_Report_v2.docx` had to be
  handled differently: a raw text compare showed `v2` was **~3,800 chars longer
  than the script output**, which first looked like unique content — but diffing the
  stripped text proved the **only** difference was the already-resolved TOC, so
  rebuilding would have been safe. To be certain nothing was lost it was **patched
  in place** instead (the M:N row inserted after the `BloodRequest →
  EmailNotification` row, the compatibility note updated) and then run through
  `finalize_word.py` at an **absolute path** (`finalize_word.py` with a relative
  arg fails in Word COM with "couldn't find your file"). Both `.docx` now contain
  the row; both `.pdf` re-exported.

### 4. The database restored to the known-good state
Restoring was two footprints, not one — the `api_check.py` leftovers session #22
flagged **plus** the rows this session's extended check created. Deleted, one
statement at a time (the multi-statement trap):
- **3 test requests** — `EM-X9QVBN`, `BB-F9PFUK` (the `api_check` ones) and
  `BB-CY4LUB` (this session's write-path check) — with their timeline, declines and
  notifications first.
- **5 test accounts** — the 3 `api-check+…` and the `b7check+…` / `b7staff+…` —
  plus their registration notification rows, the **2 extra donors** and their
  **affiliation join rows** (join rows first: the FK has no cascade).
- **A leftover forward-timeline row** on `BB-5MN8VX` (`seq=1`, "Forwarded to Nepal
  Medical College… by platform admin", written by `api_check`'s forward check) —
  the request itself had been re-unrouted but its audit entry was still there, so
  the count read **8** until it was deleted to **7**.
- **`BB-5MN8VX` re-unrouted**: `hospital_id=NULL`, label back to the em dash via
  `CONVERT(UNHEX('E28094') USING utf8mb4)` (verified `HEX = E28094`).
Final verified state: **21 users / 14 donors / 5 hospitals / 40 inventory rows / 4
requests / 3 notifications / 7 timeline rows / 39 affiliations**, TUTH B+ = 12, no
`api-check`/`b7`/`smoke` account left, the unrouted bucket holding only
`BB-5MN8VX`. The app was then stopped and **8081 freed**; the static file server on
8321 was stopped too.

### A tooling note
`code_search` (ripgrep) is broken on this machine — `rg.exe` is missing from
`~/.config/manicode/` — so searches this session used `grep` from the terminal.
Worth a look next session, but nothing here depended on it.

### Hand-off — the one item left
**Teach the user to operate the whole system for the defence** (their stated goal:
they want to drive it themselves and ask questions). `docs/DEMO_RUNBOOK.md` holds
the ~15-minute script; the missing piece is the **guided walkthrough of running
it** — what to boot, in what order, what to click, what each number proves, and the
Q&A. Everything else on the session-#22 list is done.

## Session #22 (2026-09-17) — the two criteria gaps closed: the M:N relationship + the hospital-staff link ✅

**Why this session happened.** The user asked "what's next?"; a check against
CPJ119 §2.3 (which names **1:1 / 1:N / M:N**) found the codebase had **zero
`@ManyToMany`** — plenty of 1:1 and 1:N, no many-to-many — even though the
proposal's own §3.5 promised one ("Many-to-Many (Donor to BloodGroups)"). Second
gap: a hospital account registered through the form got **no `staffedHospital`
link**, so BR-5 scoping was inactive for it. Both are now built and verified. No
existing rule moved — that is why this could be added on top of the finished
suites.

### 1. Donor ↔ Hospital — the M:N (CPJ119 §2.3)
- `model/Donor.java`: `@ManyToMany affiliatedHospitals` — the **owning** side,
  join table **`donor_hospital_affiliation`**, named rather than left to
  Hibernate's generated `donors_hospitals` (a generated name says nothing about
  WHY the rows are related).
- `model/Hospital.java`: the `mappedBy` inverse — one join table, no second FK.
- `config/DemoDataSeeder.seedAffiliations(...)`: idempotent (only unaffiliated
  donors are filled, so re-boots can't duplicate join rows) and derived from data
  already in the demo rather than invented — a donor is registered with the
  partner hospitals in **their own district** ("the blood banks near you"). Boot
  log: `Seeded 12 donor↔hospital affiliation(s) (M:N — same-district rule)`.
- **Read from both sides:** `DonorService.affiliatedHospitals(donorId)`
  (`@Transactional(readOnly = true)` because the collection is lazy and
  `open-in-view` is off) → **`GET /api/donors/{id}/hospitals`** returning
  `HospitalOption` (`id`, `label`, `shortName`, `district`, `area`); and
  `HospitalRepository.findAffiliatedDonors` / `countAffiliatedDonors` for the
  hospital side ("who is affiliated with us?" is a portal question, not a profile
  one).
- **It cannot move a rule:** BR-1 is blood group and BR-5 is the staff account's
  hospital, so an affiliation changes neither. It is what the hospital portal
  counts as its donor base and what the donor page calls its nearby blood banks.

### 2. The staffed-hospital link — BR-5 for newly registered staff
- **`service/HospitalResolver.java` (new): one resolver, two callers** — the
  request flow and registration. Order: an **id** (authoritative — a claim about
  a row that must exist, so it 404s), then the **`"name, area"`** label the
  dropdowns submit, then the **bare name or short name** (`"TUTH"`, `"Bir
  Hospital"`). **Unresolved is not an error**: it returns `null`, because a
  request legitimately has NO hospital until an administrator routes it, and a
  staff account naming a hospital we do not have yet should still be created
  (**with a warning**) rather than refused over a typo.
- `dto/RegisterRequest.java`: new **`hospital`** component (the NAME the form
  actually collects). `AuthService` sets `staffedHospital` from id-or-name for
  `Role.HOSPITAL`.
- `CODE/HTML/auth-register.html`: now sends **`hospital: hname`** for the hospital
  role (it used to send `hospitalId: null`, which is why the link was never set).

### Verified three ways — all green
| Evidence | Result |
|---|---|
| `ServiceSmokeRunner` (headless, exit code is the result) | **31 checks, 0 failed, exit 0** |
| `tools/api_check.py` — real HTTP, cookie sessions, booted app | **81 passed, 0 failed** |
| TiDB | `donor_hospital_affiliation` created by `ddl-auto=update`, **39 join rows** |

The 3 new smoke checks: the M:N **reads from both sides** and the count agrees
with the join it summarises; a hospital registered by the **name** `"TUTH"` comes
back **linked** (`hospitalLabel` contains "Tribhuvan"); an **unknown** hospital
name still creates the account, unlinked. The runner also gained `extraUsers`
cleanup so those two hospital accounts cannot leak into the demo data.

**39 rows = 9 Kathmandu donors × 4 Kathmandu partner hospitals + 3 Lalitpur donors
× Patan.** Exactly **2 donors have none** (Pokhara / Chitwan have no partner
hospital) — the honest outcome of the rule, and the reason the relationship
cannot be 1:N. Demo data verified back to **21 users / 14 donors / 5 hospitals /
40 inventory rows / 4 requests / 3 notifications**.

### Two real defects this session caught (both fixed)
1. **`countByAffiliatedHospitals_HospitalId` cannot exist.** A derived query name
   is resolved against the *entity being queried*: `affiliatedHospitals` is a
   property of **`Donor`**, not `Hospital`, so the whole context died at startup —
   `No property 'affiliatedHospitals' found for type 'Hospital'`. Replaced with an
   explicit `@Query` count (the join says the same thing in a way the repository
   actually owns).
2. **`CONVERT(CHAR(0x2014) USING utf8mb4)` is NOT the em dash in TiDB** — it
   keeps only the low byte (`0x2014 & 0xFF = 0x14`), so the "not routed yet" row
   silently stored a control character and the `?unrouted=true` check failed on
   the next run while the *row* looked fine. Fixed to
   `CONVERT(UNHEX('E28094') USING utf8mb4)` and verified at the byte level
   (`HEX(hospital_label) = E28094`, length 1). **This mattered:** the API's unrouted
   filter matches the label, so a mangled label makes the admin's "not routed yet"
   bucket look empty.

### Also in this session
- **Postman collection → 50 requests** (was 48): the M:N affiliation read, and a
  hospital-staff registration that asserts `hospitalId` is set and the label
  contains "Tribhuvan". Re-validated: JSON parses, no undefined `{{variables}}`,
  **every path still matches a declared controller route**.
- `CODE/js/bb-api.js` lost a **duplicated** mock `/api/auth/me` handler left over
  from the B7 edit (dead code, second copy unreachable).
- `HospitalOption` (new), `RegisterRequest` javadoc, `AuthService`'s warning
  comment and the shared hospital-label comment in `request/api_check.py` all
  brought up to date.

### Hand-off — next session starts here
1. **Re-run the mock QA harness (`resptest/qa-harness.html`) — expect 268/268.**
   The `bb-api.js` and `auth-register.html` edits landed *after* session #20's
   run, so they are **not yet covered** by it.
2. **Extend `resptest/real-backend-check.html`** for the two new surfaces
   (`GET /api/donors/{id}/hospitals`, hospital-staff registration by name).
3. **Docs:** `backend/README.md`, `docs/DEMO_RUNBOOK.md`, and the report's
   Table 3-1 relationship row (now 1:1 / 1:N / **M:N**).
4. **The DB carries this session's footprint.** The last `api_check.py` run left
   **2 requests** (`BB-F9PFUK`, `EM-X9QVBN`) + **3 `api-check+…` accounts**, and
   **BB-5MN8VX is routed to NMC** — so the `?unrouted=true` check will FAIL until
   the cleanup SQL is run. That SQL is printed at the end of
   **`.tools/api_check.log`** and also on every run; statements are meant to be run
   **one at a time** (a mysql client stops at the first error in a multi-statement
   `-e`).
5. **Then the thing the user actually asked for: teach them to operate the whole
   system for the defence** (they want to drive it themselves and ask questions).
   `docs/DEMO_RUNBOOK.md` already holds the ~15-minute script; what is missing is
   the guided walkthrough of *running* it.

## Session #21 (2026-09-17) — B8 COMPLETE: the Postman collection (§2.6) ✅

`docs/postman/BloodBuddy.postman_collection.json` — the last mandatory submission
artifact. Import it (File → Import) and the collection variable `baseUrl` already
points at `http://localhost:8081`.

- **48 requests in 10 folders**, in run order: `00 · Public & the security
  boundary` (deliberately logged OUT — it asserts the 401 and registers), `01 ·
  Session`, then admin / donors (§2.5 photo included) / the request lifecycle /
  the PATCH translator / hospitals / profiles, `98 · Rule refusals` (BR-1, BR-3,
  BR-5, BR-8, both §4 validation cases, the 404-not-500 edge), `99 · End the
  session`.
- **The session is captured, not assumed.** `01 · Session → Login` asserts
  `pm.cookies.get('JSESSIONID')` is non-empty — Postman's cookie jar stores the
  `Set-Cookie` automatically, so every later request is authenticated, and the
  assertion makes that *visible* instead of trusting it. `99` logs out and then
  re-requests `/api/auth/me` expecting **401**, so "logout really ends it" is
  asserted rather than described.
- **No hardcoded row ids anywhere.** Login captures `userId` / `donorId` /
  `hospitalId`, the donor search captures `donorId`, the users list captures
  `otherUserId`, and the two POSTs capture `requestCode` / `guestCode`. This is
  the B3 lesson applied: that first HTTP script used `hospitalId=1` and user id
  `10`, and TiDB's IDENTITY ids have gaps, so the *script* was broken while the
  API was right.
- **Tests assert messages, not just statuses** — e.g. the self-suspension refusal
  (409 "…your own account"), BR-1's compatibility sentence, the timeline line
  "issued from" after fulfilment, and that no response ever matches
  `/password/i` (BR-9).

**How it was validated (and what was NOT done):** the JSON was parsed and
cross-checked programmatically — every `{{variable}}` is declared, and all 48
request paths match one of the 25 routes declared across the 7 controllers, so a
typo'd endpoint cannot hide in it. The live behaviours it asserts (anonymous 401,
wrong-role 403, session `/me`, logout → 401, register 201, donor search shape) are
the same ones session #20's `real-backend-check.html` proves **32/32 against the
running app**. It has **not** been executed inside Postman or Newman — neither is
installed here — so if the evaluator wants to watch it run, import it and use
Collection Runner; the folder order is the run order.

## Session #20 (2026-09-17) — B7 COMPLETE: the frontend talks to the real backend ✅

The flip PROGRESS has been pointing at since session #11 is done: the pages no
longer run on localStorage. Register in the browser, the row lands in TiDB, the
confirmation lands in the inbox.

### What changed (frontend only — no backend source was touched)

- **`CODE/js/bb-api.js`** — `MOCK = false` is the shipped default; the demo store
  is now the *override* (`localStorage.bb_force_mock = '1'`, read before the
  script runs). Added the two endpoints the facade never had: **`auth.me()`**
  (`GET /api/auth/me`) and **`auth.logout()`** (`POST /api/auth/logout`), with
  mock routes for both so one call site works in either mode.
- **`CODE/js/nav.js`** — the server owns the session now. New step 5 on every page
  load reconciles the cached `bb_session` with `GET /api/auth/me`: cookie alive +
  cache gone → sign in from the server; cache stale + cookie gone → clear it and
  leave a private page; **no answer at all (offline / static-only server) → change
  nothing**, because a network blip must never log anyone out. Log out POSTs
  `/api/auth/logout` before clearing, and the *mock* mode check reads the same
  localStorage flag directly, so nav.js stays independent of load order and of
  pages that never load bb-api.js.
- **`CODE/HTML/auth-login.html`** — shows the **server's own message** instead of a
  generic "Login failed" ("Invalid email or password.", "That email is registered
  as a Requester account — pick that portal instead.", "This account is
  suspended…"), takes the **role from the response, never from the dropdown** (the
  server validates the portal you picked), and on load offers the way on when the
  cookie outlived a cleared cache — no surprise redirects.
- **`CODE/HTML/auth-register.html`** — **sends `password`** (the gap flagged since
  B2; `RegisterRequest` has always required it), plus `phone`/`area`; the wizard
  now **awaits** the POST so a duplicate email keeps you on step 2 with the
  server's sentence instead of marching to a success screen, and the session is
  built from the response.
- **`CODE/js/bb-validate.js`** — the shared password rule moved 6 → **8**
  characters to match `AuthService.MIN_PASSWORD`, and the register placeholder
  says so. (The login page uses the same rule, so the two are consistent with the
  policy the server actually enforces.)

### Verification — 268/268 mock + 25/25 real

**Mock suite intact:** `resptest/qa-harness.html` went 266 → **268/268**
(`QA-DONE 268/268`). The harness's `primeStorage` now pins `bb_force_mock`, and
§22 was rewritten: the old "ships MOCK = true" check was the *opposite* of the
new truth, so it is replaced by three — the override key exists, `MOCK` is the
override flag and never hardcoded `true`, and the default flip is present. Nothing
else in the 266 changed, which is the point: the flip did not regress the UI.

**Real backend: `resptest/real-backend-check.html` (NEW) → `REAL-BACKEND-DONE
32/32`** against the booted app on 8081. It is the first tool that runs the
*shipped pages* against Spring + TiDB + Security together (the harness is mock,
`real-mode-check.html` talks to the Python stub). Every page is loaded with
`bb_db.*` **pinned to empty sentinels**, so a rendered row can only have come over
HTTP. It asserts: real transport (`isMock() === false`) in every page · anonymous
`GET /api/donors/search` → **401** · login 200 → `/api/auth/me` · requester on
`/api/admin/users` → **403** · cookie-alive/no-cache login notice · requester
search 6 cards, tracking 1, admin users 5, hospital inventory 8, donor board 2 ·
register **201** + donor row (`donorId=120001`) · **logout → next `/me` = 401**.
Run it with the app up: `http://localhost:8081/resptest/real-backend-check.html`
(it needs `python tools/sync_frontend_to_backend.py` first, since it is served
from `static/`).

**"Does it actually keep records?" — now answered by a check, not a claim.**
Seven more of those checks walk the demo's central write: a requester submits
(`201`, code `BB-97WVNK`), a compatible donor signs in, the accept is accepted by
the server (BR-1/BR-3), the row gains the donor stamp and a **server-written**
timeline entry (1 → 2), and then the page **re-reads the request from the
server** — status `Matched` with the donor and match time still on it. So the
records are in TiDB, not in the page's memory. The check prints the SQL to undo
the rows it creates (and this session proved that SQL matters: the column is
`public_code`, not the `publicCode` the entity declares — Spring Boot's physical
naming strategy snake-cases it. Found by the cleanup failing, not by guessing).

**One control was lying and is fixed:** admin-dashboard's "Reset demo data" called
`BloodBuddyStore.reset()` — localStorage, which is only the MOCK transport's
storage. In real mode it would ask "Reset ALL demo data?", appear to succeed and
change nothing. It is now **hidden unless the demo store is the data source**, with
the title explaining why; restoring a real database is a deliberate seed/SQL step
(recipe at the end of this entry), not a button.

**The demo path, end to end:** that registration produced a real account row in
TiDB **and** a mail in the catcher — Mailpit showed
`Welcome to BloodBuddy — confirmation for B7 Check` at 11:01:47. The one account
the check creates is reported with its cleanup SQL and was deleted; the demo DB is
back at **21 users / 14 donors / 5 hospitals / 40 inventory rows / 4 requests / 3
notifications / 7 timeline rows**, TUTH B+ = 12, and 8081 is free.

### Still open (the honest gaps)

- **Registering a NEW hospital account still does not link `staffedHospital`**,
  so BR-5 scoping is inactive for accounts created through the form. The demo is
  unaffected — the seeded hospital account (`bloodbank@tuth.edu.np`) is linked —
  and the fix is small: teach `RegisterRequest`/`AuthService` to resolve the
  form's hospital *name* through the same label resolver `RequestService` already
  uses, or add a public `GET /api/hospitals` so the select can send a real id.
- **Server-side pagination** is still client-side slicing (`bb-paginate.js` over a
  full response). The proposal's pagination scope is met either way; `?page=&size=`
  is the CPP401-shaped upgrade.
- The QA harness is a **mock-mode** suite by design now; the real-side contract
  lives in `real-backend-check.html`.

## Session #19 (2026-09-17) — B4 COMPLETE: §2.4 email delivery, verified both ways ✅

B2 queued notifications and B3 made the API answer; **nothing had ever left the
building**. §2.4's mandatory registration confirmation now actually arrives, and
the proof is a captured message plus an audit row — not a log line saying it was
attempted.

### What was written

- **`service/NotificationMailer` (new)** — THE SMTP EDGE, and the only class that
  knows how to talk to a mail server. It composes from the audit row (subject,
  recipient, request/account details), sends a `MimeMessage` with a plain-text
  **and** an HTML part, and **returns the text body it sent** so the caller can
  store it. `NotificationService` still decides WHO/WHEN; the mailer decides HOW.
- **`service/NotificationService`** — `record()` now writes the row, calls the
  mailer, and stamps the outcome: `SENT` + `sentAt`, or `FAILED` + `errorText`
  (flattened and truncated to the column's 500 chars). Catches `Exception`, not
  just `MessagingException`: a refused connection arrives as a `MailException`, a
  bad address as an `AddressException`. **It never rethrows** — the user's action
  already succeeded, and the FAILED row is the honest record of what did not.
- **`model/EmailNotification`** — gained a `body` column (`TEXT`) holding the
  plain-text message actually sent, so the §2.4 evidence can be read straight out
  of the database with no SMTP client and no screenshots.
- **`application.properties`** — `mail.smtp.auth` and `starttls.enable` were
  **hardcoded `false`**, which would have silently broken the real-Gmail swap:
  they are now `${BB_SMTP_AUTH:false}` / `${BB_SMTP_STARTTLS:false}`, with the
  Mailpit / Gmail / any-relay table written into the file. **Zero code change
  stands between dev mail and a real §2.4 demo.**
- **`config/ServiceSmokeRunner`** — 3 new checks (25 → 28) plus a headless mode
  (`bloodbuddy.smoke.exit`, see below). New checks: the §2.4 confirmation is
  **DELIVERED** (`SENT`, `sentAt` set, addressed to the account, body names that
  account); the compatible-donor fan-out is delivered too, not just the mandated
  mail; and an undeliverable notification is **kept as `FAILED` with a reason**
  rather than dropped or thrown. That last one is the only *deterministic* failure
  available to a test — no recipient address needs no broken SMTP server — and it
  exercises exactly the branch a real outage takes.
- **`config/DemoDataSeeder`** — its 3 demo-history rows now set `recipientEmail`
  like a live notification does, so the audit table reads the same whichever path
  wrote the row. They deliberately keep `body` NULL: they are history that was
  never sent, and inventing the words a past mail "said" would fabricate evidence
  rather than record it. (The 3 rows already in TiDB were backfilled by hand —
  the seeder's guards mean a code change cannot rewrite them.)

### The dev catcher: Mailpit, not MailHog

There was **no catcher on the machine**: no MailHog binary, no Docker, and Python
3.13 (the stdlib `smtpd` catcher is gone in 3.12+). Mailpit — the maintained
MailHog successor, same 1025/8025 convention — was downloaded as a single `.exe`
into **gitignored `.tools/`**: no install, no admin, nothing global:

```
.tools/mailpit/mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
```

Its **HTTP API is what made the verification assertable** (`/api/v1/messages`),
so "did it arrive?" is a query, not an eyeball.

### Also added: headless smoke runs

`bloodbuddy.smoke.exit=true` makes the app shut itself down when the run finishes,
**with the exit code as the result** (0 = all passed). Without it `spring-boot:run`
never returns, so the run cannot be a single command in this terminal. It is what
makes B4–B8 verifiable; the same flag applies to `System.exit` only after the
summary is logged, so the log is always complete.

### Verification — the same suite run BOTH ways

**Catcher ON** → `28 checks, 0 failed`, exit 0 (`backend/target/bb-b4-smoke.log`,
`bb-b4-final.log`). Mailpit held **52 captured messages**, among them
`Welcome to BloodBuddy — confirmation for Smoke Tester` → `smoke+…@example.com`,
with plain-text and HTML parts and the body naming the confirmed account. The
guard-free fan-out delivered 11 alerts per request, and the accept/fulfil mails
went out as `BB-JQKAQH — your request has been fulfilled`.

**Catcher OFF** → `28 checks, 2 failed`, **exit 1**
(`backend/target/bb-b4-noSMTP.log`). Exactly the two mail-arrival checks failed —
`MailConnectException: … Connection refused` on 1025 — while **registration,
BR-1…BR-9 and every other check still passed**. That is the asserted contract:
an SMTP outage is *recorded, never fatal*. The check is therefore known to be able
to fail, not merely observed to pass.

Afterwards: demo DB verified unchanged at **21 users / 14 donors / 5 hospitals /
40 inventory rows / 4 requests / 3 notifications / 7 timeline rows**, TUTH B+ =
12, and 8081 free. The Mailpit database now sits in `.tools/` (gitignored) with
the 52 captured messages — the screenshot evidence for §2.4.

### Left for later (unchanged, and now the whole remaining backlog)

- **B8 — Postman** (its login request must capture the `JSESSIONID` cookie) then
  **B7 — flip `MOCK = false`**, which still needs `auth-register.html` to send
  `password`, the role to come from `GET /api/auth/me`, and the hospital
  `staffedHospital` link.
- The real-Gmail §2.4 demo is **only** waiting on Google's app-password delay for
  `bloodbuddy1org@gmail.com` (2SV is registered, the phone is too new). Nothing
  else blocks it — the env vars already work.
- Cosmetic: PROGRESS's older sections still say "MailHog" historically; the code
  and README now say Mailpit / "SMTP" so they cannot go stale again.

## Session #18 (2026-09-17) — B6 COMPLETE: Spring Security, real role rules + session login ✅

The API is now **authorized, not just documented**: the role split the frontend
enforces in `nav.js` (client-side, therefore a UX guard only) is enforced server-side
by Spring Security, and the B3 **dev actor header is gone** — a caller is whoever the
session says they are.

### What was written

- **`pom.xml`** — `spring-boot-starter-security` (the `spring-security-crypto` that
  B2 used for BCrypt was already there; this brings the filter chain, the session
  context and the method-security annotations).
- **`config/SecurityConfig`** — the whole policy in one readable place, with the
  rules as a table in the javadoc:
  - public: the static site, `POST /api/auth/{register,login,forgot}`, `POST /api/contact`,
    `POST /api/requests` (the guest emergency form) and `GET /api/requests/lookup/*` (BR-8);
  - `ADMIN` → `/api/admin/**`; `HOSPITAL`/`ADMIN` → `/api/hospitals/**`; any signed-in
    account → donors, requests, profile, `/api/auth/me`;
  - anonymous API calls answer **401**, wrong-role calls **403** — both written with
    the same `ApiError` body the controllers use (a custom `AuthenticationEntryPoint`
    and `AccessDeniedHandler`), so `bb-api.js` parses one shape either way.
  - `@EnableMethodSecurity` + `@PreAuthorize` on the two role-restricted controllers
    (defence in depth: a future path change cannot accidentally open the admin surface).
- **`web/AuthenticatedAccount`** — the session principal (userId, email, role).
- **`web/SessionAuthenticator`** — `signIn` builds the authenticated
  `SecurityContext` with a `ROLE_*` authority and **saves it explicitly** (the filter
  chain has already run for this request, so nothing else would persist it);
  `signOut` clears the context and invalidates the session.
- **`web/ActorContextArgumentResolver`** — rewritten (the ONE class B3 flagged, and
  the rewrite is the documented body swap: **no controller and no service changed**).
  It now takes the principal from `SecurityContextHolder`, **reloads the `User` per
  request** (so a suspend or a role change takes effect immediately instead of living
  on in a stale session copy), and returns `ActorContext.anonymous()` when nobody is
  signed in — which is what keeps the public emergency flow working.
- **`web/AuthController`** — `login` now opens the session after `AuthService`
  verifies the password, `POST /api/auth/logout` ends it, `me` answers from the
  session. `LoginResponse` carries the account it opened the session for.
- **`HospitalController`/`AdminController`** — `@PreAuthorize` added; the hospital
  controller also stops taking a hospital id from a non-admin caller (BR-5 remains a
  service-layer comparison, deliberately not a URL rule — see the `SecurityConfig` javadoc).
- **`application.properties`** — the `bloodbuddy.dev.actor-headers` switch is deleted
  (no code reads `X-BB-Email`/`X-BB-User-Id` any more) and the session timeout is set
  (8h) so a demo session survives a working day.

### Two deliberate decisions (both stated in the javadoc, not left implicit)

1. **CSRF is disabled.** The frontend is `fetch` JSON with same-origin credentials and
   no token plumbing; enabling it would break every write until `bb-api.js` reads an
   `XSRF-TOKEN` cookie and echoes a header. That is a change to a finished frontend, so
   it is a decision to record — not a Spring default to flip silently.
2. **BR-5 is not a URL rule.** “A hospital may only touch its own stock” is a
   comparison between the session's hospital and the row's, so it stays in the service
   layer; the controller's job is only to refuse a caller-supplied `hospitalId` when
   the session already has one.

### Verification — 74/74 over real HTTP (+ the B2 smoke run re-run)

`tools/api_check.py` was rewritten to authenticate the way a browser does: it POSTs
`/api/auth/login` and carries the `JSESSIONID` cookie on a cookie jar instead of
sending an identity header. **`HTTP exercise: 74 passed, 0 failed`**
(`backend/target/bb-http-b6.log`), now including the security boundary itself —
anonymous on a member endpoint → **401**, requester/admin surfaces as the wrong role →
**403**, logout really ends the session (the next call → 401) — on top of every
business-rule refusal from B3.

The B2 service smoke run was then re-run **with security on**:
**`smoke run: 25 checks, 0 failed`** (`backend/target/bb-b6smoke.log`) — i.e. adding the
filter chain did not change a single service-layer behaviour.

### Known limitation (recorded because it is real, and asserted in the checks)

**Suspending an account blocks the next login but does not kill a session that is
already open** — the row's `status` is re-read per request, so a suspended *donor*
still holding a session is refused at the business-rule level, but a suspended
account's live session is not force-expired. Doing that properly needs a
`SessionRegistry` + `HttpSessionEventPublisher` and is the natural follow-up (the
check is written to assert today's behaviour, not to hide it).

### A cleanup trap worth remembering (found and fixed here)

Restoring the demo data after the HTTP run exposed two Windows/shell traps — the kind
that make a cleanup look like it worked when it did not:

- **A literal em dash in a console argument arrives as `?`.** The cleanup's
  “make the forwarded request unrouted again” line writes the label `—`; the demo row
  was found holding `hospital_label` hex **`3F`** (`?`) instead of **`2014`**. The
  script now emits `CONVERT(CHAR(0x2014) USING utf8mb4)`, which survives the console,
  and the row was restored to the em dash.
- **A mysql client stops at the first error in a multi-statement `-e`.** Combined with
  the above, the first failing statement silently skipped the rest, so the run left an
  account and a notification behind while appearing to succeed. The tool prints the
  statements **one at a time** with that warning, and the cleanup was completed by hand
  (verified: 21 users / 14 donors / 4 requests / 3 notifications / 40 inventory rows /
  7 timeline rows, TUTH B+ = 12, `BB-5MN8VX` unrouted).

### Leftovers for later

- `auth-register.html` still does not send `password` — **required before B7 flips
  `MOCK = false`** (a register POST without it now fails validation).
- The frontend still has no `fetch` credential/token plumbing, so it is unaffected by
  the session model (`fetch` sends same-origin cookies by default) — but the DEMO
  login in `nav.js` (`bb_session`) is now only the UI's copy of the role; real-mode
  pages must take the role from `GET /api/auth/me` (B7).
- Hospital accounts registered through the form still lack a `staffedHospital` link,
  so BR-5 scoping is not active for them (unchanged from B3).

## Session #17 (part 3, 2026-09-17) — B3 COMPLETE: REST controllers + error mapping ✅

The backend now ANSWERS. `GET /api/donors/search?blood=B+` returned a real donor
array from TiDB; before this session every `/api/**` path 404'd.

### What was written (`backend/src/main/java/com/bloodbuddy/web/`)

- **7 controllers** — `AuthController` (`/api/auth/*`: register/login/forgot/me),
  `DonorController` (search, one donor, compatible set, §2.5 photo upload +
  retrieval), `RequestController` (list/get/lookup/create/patch),
  `HospitalController` (inventory board + stock set, both Appendix B shapes),
  `AdminController` (users list/filter/suspend/restore), `ProfileController`
  (`/api/profile/{role}` get/save), `ContactController`. All HTTP-only: every
  decision stays in the B2 services.
- **`ApiExceptionHandler`** (`@RestControllerAdvice`) + `ApiError` —
  `NotFoundException` → **404**, `BusinessException` → **409**, bean validation →
  **400** with per-field messages, bad enum/label parses → **400**, unique-constraint
  races → **409**. The body's `message` is what `bb-api.js` already reads, so a
  refused action reaches the user as the rule's own sentence.
  **Deliberately no catch-all `Exception` handler** — it would also swallow
  Spring's `NoResourceFoundException` and turn a missing static page into a 500.
- **`ActorContextArgumentResolver`** + `WebConfig` — so controller signatures read
  `create(RequestCreateRequest body, ActorContext actor)` instead of digging
  through the request. ~~It is a **dev stand-in**~~ **(SUPERSEDED in session #18: the
  resolver now reads the session principal — the header-based body described below no
  longer exists, keep it only as history of the B3 phase)**: only
  `spring-security-crypto` is on the classpath at this point (no `SecurityContext`),
  so the actor came from the headers
  `X-BB-Email` / `X-BB-User-Id` behind `bloodbuddy.dev.actor-headers` (default
  true, warning logged at startup). The body of `resolveArgument` is the only thing
  B6 replaces — controllers and services do not change. Absent identity resolves to
  `ActorContext.anonymous()` (never null, so a guest request cannot NPE).
- **DTOs** `ApiAck`, `ForgotRequest`, `ContactRequest`, `InventoryRow`.
- **Service/DTO adjustments the HTTP edge needed:** `neededBy` is now a String
  parsed by the service (the search modal genuinely submits `""`, and Jackson would
  have answered a generic 400 before the rule could say “a needed-by date is
  required”); `RequestService.byRequesterRef` (the demo's tracker asks by NAME) and
  `byHospitalLabel` (queues addressed by “TUTH, Maharajgunj”); `AuthService.me`
  guards a null session; `PhotoCodec` + `DonorService.photoBytes` for the raw-image
  endpoint; BR-1's refusal message reworded (“Donor group A+ cannot donate to a B+
  recipient — see the compatibility chart (BR-1)”).

### Decisions worth keeping

- **BR-5 is enforced by the controller preferring the session's hospital over
  `?hospitalId`.** A hospital account cannot cross hospitals even if it passes the
  parameter; the parameter exists for dev/Postman and for admins, and should be
  dropped for non-admin callers when B6 lands.
- **Wire shapes match what the pages already read**: the search endpoint returns the
  ARRAY by default (`?withMeta=true` adds the widen explanation), the inventory
  board returns the plain `{ "O+": 24, … }` map the dashboard iterates, admin user
  ids are STRINGS (the page calls `.toLowerCase()` on them), and PATCH stays a
  translator into the rule-checked operations.
- **`company`/`requester`/`compatOk`/`timeline` sent by the pages are ignored** by
  design (session decides the requester; compat is recomputed; the server owns the
  audit trail). Jackson drops the unknown properties, so no page edit is needed.
- **§2.5/B5 is effectively covered**: `POST /api/donors/{id}/photo` stores Base64 →
  BLOB and `GET …/photo` serves the bytes with a sniffed content type.

### Verification — 56/56 over real HTTP

`tools/api_check.py` (kept: the Postman collection in B8 is the deliverable, but
this is the check that can assert rule MESSAGES and run in a terminal) against the
booted app on 8081:

```
python tools/api_check.py        →  HTTP exercise: 56 passed, 0 failed
```

Coverage: donor search (+widen, +compat, +registry), single donor, compatible set,
login/me/forgot/register validation, the tracker's name lookup, the TUTH queue by
short label, the unrouted bucket, `{code}` get, guest emergency POST → EM- code +
BR-8 lookup (and the member-code refusal), blank/past needed-by, BR-2 illegal jump,
BR-3 second donor, BR-1 incompatible donor, admin-forward-as-non-admin refusal,
admin forward (assigned + still Pending), cancel as admin, the inventory board
(actor-scoped, Appendix B scoped form, “no hospital” 409, stock set + restore, bad
group label), admin user filter + suspend/restore + self-suspension refusal, profile
get + no-session 409, contact validation, photo 404, and unknown path → 404 (not 500).

**Two of the first-run failures were the SCRIPT's bugs, not the API's** — it used
`/api/hospitals/1/inventory` and user id `10`, but TiDB's auto-increment leaves
gaps, so those ids do not exist. The endpoints were right (they answered a correct
“Unknown hospital: 1”); the ids now come from `/api/auth/me`. Worth remembering
before writing another test that hardcodes a row id.

**Footprint:** the run mutates demo data (creates + cancels a guest request,
admin-forwards BB-5MN8VX). There is no unroute/delete endpoint, so the script
PRINTS the cleanup SQL; after running it the database was verified back at
21 users / 14 donors / 4 requests / 3 notifications / 7 timeline rows / 40 inventory
rows, TUTH B+ = 12, BB-5MN8VX unrouted.

### Left for B6 (deliberately not in the controllers) — ✅ CLOSED in session #18

- ~~**Role enforcement is NOT here.**~~ **Done:** `config/SecurityConfig` now owns the
  rules (“administrators only” = `/api/admin/**`, “hospital staff only” =
  `/api/hospitals/**`, everything else under `/api/**` = signed in) and
  `ActorContextArgumentResolver` reads the session instead of the dev header. The
  reason it was left here still holds: a controller-side check would have been the
  only barrier between a requester and every account on the platform. See session #18.
- `auth-register.html` still does not send `password` (unchanged from the B2 note) —
  needed before B7 flips `MOCK = false`.
- Hospital accounts registered through the form still lack a `staffedHospital` link
  (the form sends no hospitalId), so BR-5 scoping is not active for them yet.

## Session #17 (part 2, 2026-09-17) — B2 COMPLETE: service layer + DTOs, verified against live TiDB ✅

B2 landed in one pass (the plan from session #14, unchanged). Everything is under
`backend/src/main/java/com/bloodbuddy/`. **Compiles clean** (bundled Maven) and was
exercised for real against the TiDB demo database — see "Verification" below.

### What was written

- **`dto/` (15 records)** — `RegisterRequest`, `LoginRequest`, `AccountResponse`,
  `LoginResponse`, `DonorResponse`, `DonorSearchResult`, `RequestCreateRequest`,
  `RequestResponse` (+ nested `TimelineEntry`), `RequestUpdateRequest`,
  `InventoryBoard`, `InventoryUpdateRequest`, `UserAdminResponse`,
  `UserStatusUpdateRequest`, `ProfileDto`, `PhotoUploadRequest`. Component names
  mirror what the pages already read (`bb-api.js` + the mock store's row shape are
  the de-facto wire contract), so the real-mode flip is a mapping, not a rewrite.
- **`service/` (7 services + 4 support types)** — `AuthService` (register/login/
  forgot/me, BCrypt, no user enumeration), `DonorService` (**the one home of the
  Appendix A chart** — `compatible()`, search with the server-side widen
  fallback, §2.5 photo, BR-7 "requests near you"), `RequestService` (the whole
  lifecycle: create, reader queries, BR-1/BR-3 accept, BR-2 transitions, BR-4
  fulfilment with the stock decrement, BR-5 scoping, BR-7 decline, BR-8 guest
  lookup, plus `applyUpdate` — the translator that routes the demo's one generic
  PATCH into those operations), `InventoryService`, `AdminService`,
  `ProfileService`, `NotificationService` (the B4 hook points), and
  `BusinessException` / `NotFoundException` / `ActorContext` / `PhotoCodec`.
- **`config/CryptoConfig`** — one shared `BCryptPasswordEncoder` bean (BR-9), now
  also used by the seeder instead of its own instance.
- **Model additions**: `Hospital.shortName` (so "TUTH" resolves server-side
  without the frontend alias table), and on `User`: `address`,
  `preferredHospital`, `urgentAlerts` (profile-editor fields the ERD never
  modelled — persisting them beats echoing them back unsaved).
  `RequestStatus` gained `label()/fromLabel()/isTerminal()`, `Urgency` gained
  `label()/fromLabel()`, `NotificationType` gained `STATUS_UPDATE` and
  `PASSWORD_RESET`. Repository additions: the BR-3 **pessimistic-lock** finder,
  guest/unrouted/status finds, donor group-set + district-only search,
  hospital short-name lookups, and the seeder's request-less notification guard.
- **`config/ServiceSmokeRunner`** — the B2 verification harness (see below),
  gated by `bloodbuddy.smoke` (default `false`).

### Design decisions worth knowing (they are the graded §2.2 argument)

- **BR-1 lives in exactly one place** (`DonorService.compatible`), using the
  chart in its printed recipient→donor direction; the notification fan-out is
  handed that list rather than re-deriving it, so matching and alerting can never
  disagree.
- **BR-3 is race-safe for real**: the accept path loads the request under a
  `PESSIMISTIC_WRITE` lock and re-checks the donor FK, instead of the read-then-
  write window `existsBy…` alone leaves open.
- **The server owns the timeline**: every transition appends its own entry and a
  client-supplied `timeline` is ignored, so a PATCH cannot rewrite history.
- **Identity never comes from the body**: the accepting donor is the signed-in
  donor, the writing hospital is the staff account's hospital. `ActorContext`
  carries that (B3 fills it from the session, B6 from the principal).
- **`applyUpdate` is a translator, not a field writer** — the demo used
  `status: "Accepted"` for three different events (donor match, admin routing,
  hospital takeover); it is disambiguated by WHO is asking, and forwarding
  deliberately leaves the request `Pending` (routing ≠ matching).
- `compatOk` is recomputed server-side as "at least one available donor can serve
  this recipient group"; seeded rows keep their demo values (they are pinned
examples, not recomputed).

### Verification — `bloodbuddy.smoke=true`, 25/25 PASS

Booted the real app against TiDB with the smoke runner on and `show-sql=false`:

```bash
cd backend
eval "$(python - <<'PY' … )"   # same BB_DB_* recipe as the boot gate above
BLOODBUDDY_SMOKE=true SPRING_JPA_SHOW_SQL=false \
  "…/maven3/bin/mvn.cmd" -DskipTests compile spring-boot:run
```

Log (`backend/target/bb-smoke.log`, gitignored): **`smoke run: 25 checks, 0 failed`
→ `Smoke run cleaned up: 3 request(s) removed, 1 inventory row(s) restored`**. It
covers login/BCrypt, wrong-password refusal, registration (+ the §2.4 hook), the
§2.5 photo BLOB round trip, donor search + widen, BR-1 both directions, BR-2
(illegal transitions refused, terminal states locked), BR-3 double-accept, BR-4
exact decrement, BR-5 wrong-hospital refusal, BR-7 decline idempotency, BR-8
guest-only lookup, admin suspend/restore + no self-suspension, pinned-code
validation, past-date rejection, and unknown-id → NotFound.

**It is re-runnable and self-cleaning** — after the run the database was verified
back at exactly 21 users / 14 donors / 4 requests / 3 notifications / 0 declines /
40 inventory rows, with TUTH's B+ stock restored to 12. Keep it for B3–B7.

### Two real bugs this run found (and fixed)

1. **`User.donor` had `orphanRemoval = true` on the NON-owning side of the 1:1.**
   `ProfileService.save` persists the Donor and then the User; because the
   in-memory `donor` field was never loaded, Hibernate treated the User's null
   donor as an orphan and **deleted the donor row** — the account survived its own
   profile save with no donor, and the later cleanup delete matched nothing
   (`StaleObjectStateException`). Found only because the smoke run checks the row
   still exists after the save; the profile round-trip check itself PASSED while
   the row was already doomed. `orphanRemoval` removed (cascade = ALL kept, so
   deleting an account still deletes its donor).
2. **The seeder's notification guard skipped request-less mail.**
   `notify()` only de-duplicated when the notification had a `request`; the
   registration confirmation has none, so **every boot added another "Welcome to
   BloodBuddy" row for Arun** (3 rows → 4 → …). Fixed with
   `existsByRecipient_UserIdAndTypeAndRequestIsNull`.

Two runner bugs found alongside them (no product impact, recorded because they
cost time): the demo seeder was an **un-ordered** `ApplicationRunner`, so it ran
*after* the `@Order(10)` smoke run and made hospital-label resolution look broken
(fixed: `@Order(0)`); and the runner's cleanup deleted the donor and then the
account, which fights `User.donor`'s cascade-merge (fixed: delete the account,
let the cascade take the donor).

### Leftovers / notes for B3

- **The demo database now carries the new columns** (`hospitals.short_name`,
  `users.address`, `users.preferred_hospital`, `users.urgent_alerts`) —
  `ddl-auto=update` applied them on the smoke boot.
- **Frontend gap found while writing the DTOs:** `auth-register.html` never sends
  `password` (the demo login ignored it). `RegisterRequest`/`AuthService` require
  it, so the page must send it when `MOCK` flips to `false`.
- `GET /api/donors/search` must answer with the array (`DonorSearchResult.donors()`)
  because the page iterates the value directly; the widen message is available on
  the wrapper for when the page is reworked to show the server's own explanation.
- Hospital accounts registered through the form still have no `staffedHospital`
  link (the form sends no hospitalId) — BR-5 scoping for them starts once B3/B6
  sets it; the service logs a warning when it happens.

## Session #17 (part 1, 2026-09-17) — boot gate closed; B1 seeder verified at runtime ✅

The one remaining ▶ from session #16 was performed from the terminal instead of the IDE (equivalent: the same `compile` + `spring-boot:run` with the same env vars), so the result is reproducible and logged rather than only observed. Log: `backend/target/bb-boot.log` (gitignored).

Reusable recipe — credentials are read out of the IntelliJ run config, never retyped:

```bash
cd backend
eval "$(python - <<'PY'
import re
s = open('.idea/workspace.xml', encoding='utf-8').read()
def v(n):
    m = re.search(r'<env name=\"\s*%s\s*\" value=\"([^\"]*)\"' % n, s); return m.group(1).strip()
for k in ('BB_DB_HOST','BB_DB_PORT','BB_DB_USER','BB_DB_PASSWORD'):
    print("export %s='%s'" % (k, v(k)))
print('export BB_SERVER_PORT=8081')
PY
)"
"/c/Program Files/JetBrains/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3/bin/mvn.cmd" -DskipTests compile spring-boot:run
```

**Result — clean end to end:** `HikariPool-1 - Start completed` → `Tomcat started on port 8081 (http)` → `Started BloodBuddyApplication in 11.29 seconds` → **`Demo data ready: 21 users, 14 donors, 5 hospitals, 40 inventory rows, 4 requests, 3 notifications`** → the process **stayed up** (no ApplicationRunner failure, i.e. the transient-`User` bug from session #16 is fixed in practice, not just compiled). `http://localhost:8081/CODE/HTML/landing.html` → **200**.

- **"21 users", not the 22 session #16 predicted — and 21 is the correct number.** `seedUsers()` inserts 12 (`arun, sunita, bikash, sita, ramesh, tuth, bir, patan, anita, admin, kiran, priya`) and `seedDonors()`'s nine `fresh(...)` donors add nine more users, so 21; the 14 donors are 5 pinned + 9 fresh. Nothing is missing — the earlier figure was an estimate carried in the hand-off note, not a verified count.
- **`GET /api/donors/search` → 404 is expected** at this point: nothing maps `/api/**` until B3's controllers exist. It is noted here so a later session does not read that 404 as a regression.
- **The process was stopped afterwards** (`taskkill //F //PID <pid>`) so port 8081 is free for IntelliJ's ▶; the seed is already in TiDB and the seeder is idempotent, so re-running only verifies.
- **`backend/.idea/workspace.xml.bak-20260917-092131` deleted** — it held the TiDB password and outlived its purpose once the boot was confirmed. Only `workspace.xml` itself now carries the credentials (gitignored), as intended.
- Frontend and Java sources untouched this session — the change under test was session #16's one-line `fresh()` fix.

**Outstanding / next:** B2 (Service layer + DTOs) — and nothing else blocks it. Optional cosmetics still open from session #16: drop the redundant explicit `hibernate.dialect`, quiet `show-sql`, and rotate the TiDB root password (it was pasted into the session #15/#16 chat). §2.4's Gmail app password is still pending Google's new-phone delay and is not needed until B4.

## Session #16 (2026-09-17) — first boot reached; TiDB live; three blockers fixed 🔶 (one ▶ remains)

Goal was session #15's two gates (create the schema → first ▶). The app **booted and served** — TiDB connected, all 8 tables created, Tomcat on **8081** — and the boot-stopping seeder bug was found and fixed. **The last action is unfinished: the fix is compiled into `target/classes`, but the app has not been started since.** Session #17 starts one ▶ away.

### Gate 1 ✅ — `bloodbuddy` schema created (TiDB Cloud Starter)

The cluster only had `test`, so the schema had to be created by hand. It was created with XAMPP's bundled client (`/c/xampp/mysql/bin/mysql.exe`) — **a client only**: the server it talked to reports `8.0.11-TiDB-v8.5.3-serverless`, i.e. TiDB Cloud, not a local MariaDB. Nothing was created locally, and the browser console shows the same database (SQL Editor dropdown / Data tab).

Verified afterwards: `SHOW DATABASES` lists `bloodbuddy`; `DEFAULT_CHARACTER_SET_NAME = utf8mb4` (collation `utf8mb4_bin`, TiDB's default); it had 0 tables at that point.

**Reusable recipe** — reads the credentials straight out of the run config instead of retyping them:

```bash
eval "$(python - <<'PY'
import re
s = open('backend/.idea/workspace.xml', encoding='utf-8').read()
def v(n):
    m = re.search(r'<env name="\s*%s\s*" value="([^"]*)"' % n, s); return m.group(1).strip()
print('BBH=%s' % v('BB_DB_HOST')); print('BBU=%s' % v('BB_DB_USER')); print('BBP=%s' % v('BB_DB_PASSWORD'))
PY
)"
"/c/xampp/mysql/bin/mysql.exe" --host="$BBH" --port=4000 --user="$BBU" --password="$BBP" --ssl -D bloodbuddy -e "SHOW TABLES;"
```

(MariaDB's client takes `--ssl`, not MySQL's `--ssl-mode=REQUIRED`; the “Using a password on the command line” warning is harmless. Note the client resolves `<host>` and connects to TiDB via AWS Global Accelerator IPs `99.83.224.222` / `75.2.106.174`.)

### Blocker 1 — IntelliJ stored the env vars with invisible whitespace

The first ▶ died in ~1 s with `Communications link failure` / `java.net.ConnectException: Connection refused` — **not** an auth or TLS error. Cause: the env table held `name=" BB_DB_HOST "` / `value="…tidbcloud.com "` (leading **and** trailing spaces), so `${BB_DB_HOST:localhost}` never matched and the app dialled `localhost:4000`, which refuses instantly. Two hand-retypes had also introduced `BB_DBPORT` (missing `_`) and `BB_DB_USER=3KKFmsXqsbQodT.root` (**missing the capital D** — TiDB usernames are case-sensitive, so that alone would have produced `Access denied`).

- **Fix:** with IntelliJ fully closed (the IDE rewrites `workspace.xml` on exit, so an edit made while it runs is lost), the `<envs>` block was rewritten from a script that stripped all whitespace and reused the host/password already in the file rather than retyping them. Verified: all four rows clean.
- **Backup to delete once the boot is confirmed:** `backend/.idea/workspace.xml.bak-20260917-092131` (gitignored, but it holds the password).
- **Lesson: never retype the credentials into that dialog.** It renders trailing whitespace invisibly, and there is no way to check a cell for it. Patch the file, or paste a known-clean string, and always re-read the file to confirm.
- The network itself was cleared independently: TCP 4000 to `gateway01.ap-southeast-1.prod.aws.tidbcloud.com` connects in ~38 ms, and non-standard ports work in general (an HTTPS probe against 4000 reports an SSL error, which is expected — MySQL speaks first, so the TLS handshake can't complete).

### Blocker 2 — port 8080 belongs to the EDB PostgreSQL PEM Apache service

`netstat` showed PID 5128 (`httpd.exe`, Windows service **`PEMHTTPD-x64`**, session 0) listening on `0.0.0.0:8080` and answering with EDB's default page. So every app URL returned *its* 404, and Tomcat failed with `Port 8080 was already in use` **after** Hibernate had already created the tables (DDL runs before the web server binds) — i.e. it looked like a routing bug rather than a port clash.

- **Fix:** the app now runs on **8081** — `server.port=${BB_SERVER_PORT:8081}` plus a comment naming PEMHTTPD, so it can be moved back with one env var once that service is gone. `backend/README.md` updated (run step + a “Why 8081, not 8080?” note).
- **Trap that cost a whole extra run:** editing a resource in `src/main/resources` does **not** update `target/classes` unless IntelliJ actually rebuilds (its VFS had not noticed the external edit), so a ▶ silently reused `server.port=8080` and failed identically. After any config/resource edit: `File → Reload All from Disk`, then **Build → Rebuild Project** — or compile with the bundled Maven. If a config change appears ignored, check `target/classes/application.properties` first.

### Gate 2 ✅/🔶 — first successful boot, then the B1 seeder bug

Boot #3 got all the way to: `HikariPool-1 - Start completed` → `Database version: 8.0.11` → Hibernate `create table`s → **`Tomcat started on port 8081 (http)`** → `Started BloodBuddyApplication in 11.191 seconds`. The **8 tables are live in TiDB**: `users`, `donors`, `hospitals`, `blood_inventory`, `blood_requests`, `request_timeline`, `request_declines`, `email_notifications`.

It then exited (code 1) inside the seeder:

```
HHH000437 … Unsaved transient entity: (User) … Dependent entities: (Donor)
org.hibernate.TransientPropertyValueException: Not-null property references a transient value
  … com.bloodbuddy.model.Donor.user -> com.bloodbuddy.model.User
  at DemoDataSeeder.donor(DemoDataSeeder.java:185)     ← the 5th donor, right after 4 clean inserts
```

- **Cause:** `seedDonors()` reuses `us.get(0/1/2/11)` for its first four donors (those Users came back from `users.saveAll(...)`, so they are persisted), but the other ten come from `fresh(name, email, district)` → `user(...)`, which only **constructs** a `User` — persisting was `seedUsers()`'s job via `saveAll`. `Donor.user` is a non-nullable FK (`@JoinColumn(nullable = false, unique = true)`), so attaching a transient User fails at insert time. Because the seeder is an `ApplicationRunner`, that failure killed the whole app; the transaction rolled back, leaving the DB empty.
- **Fix:** `fresh()` now returns `users.save(user(name, email, Role.DONOR, district))`. `User.userId` is `GenerationType.IDENTITY`, so the User is inserted and its id assigned before the Donor references it.
- **Verified:** `mvn -DskipTests compile` → exit 0 (IntelliJ's bundled Maven 3.9.11); `target/classes` refreshed. **Not yet verified at runtime — that is the one ▶.**

### Also changed this session

- `application.properties`: **removed the dead `createDatabaseIfNotExist=true`** from the JDBC URL (serverless TiDB rejects it — the file's own comment already said so) and documented the one-off manual `CREATE DATABASE` in its place; `server.port` moved to 8081 (above).
- `backend/README.md`: run step now points at **8081**, with a “Why 8081, not 8080?” note and `BB_SERVER_PORT` added to the documented config keys.
- Frontend untouched; `static/` is still session #13's 94-file sync.

### Outstanding (session #17 starts here)

1. **Press ▶ once and confirm the seed.** Expect `Tomcat started on port 8081` → `Demo data ready: 22 users, 14 donors, 5 hospitals, 40 inventory rows, 4 requests, 3 notifications` and the process **staying up**, then `http://localhost:8081/CODE/HTML/landing.html`. Then delete the `workspace.xml.bak-*` backup.
2. Cosmetic, optional: drop `spring.jpa.properties.hibernate.dialect` — Hibernate 6 warns `HHH90000025: TiDBDialect does not need to be specified explicitly`. `spring.jpa.show-sql=true` also makes the console very noisy (one line per query); turn it down if it gets in the way.
3. **Rotate the TiDB root password when convenient** — it was pasted into this session's chat and screenshots. The cluster's allowlist still pins the current ISP IP; if the app ever reports access-denied, re-add the IP in the console before suspecting the password.
4. The §2.4 SMTP app password is still pending Google's new-phone security delay (session #15) — nothing needs it until B4.
5. **Then B2** — Service layer + DTOs, unchanged from session #14's plan.

## Session #15 (2026-09-17) — credential setup + TiDB Cloud cluster live 🔶 (no code changed)

Goal was to unblock the first boot so B2 could be exercised. **Code was untouched this session** — all work was account/console/IDE setup.

- **Leaked credentials — actioned.** The app password for `sakshammaharjan9@gmail.com` that sat in the public `SpringWebVir` repo has been **deleted from the Google account** (Security → 2-Step Verification → App passwords). That item from the session #13 addendum is closed, and the password string has been redacted out of this file.
- **Dedicated Gmail exists: `bloodbuddy1org@gmail.com`.** Confirmed by the cluster's "Created by" field, so that is the exact spelling to use later for `BB_SMTP_USER` / `BB_MAIL_FROM` — not a guess.
  - **2SV is registered but NOT armed.** Phone `986-4156112` is attached, and Google is showing *"there's a security delay before you can use a new phone for 2-Step Verification."* → **app passwords are therefore still not available**, so §2.4's real-SMTP password remains outstanding.
  - **This blocks nothing.** The DB account authenticates via Google OAuth (not an app password), and dev email is MailHog. Suggested workaround while the delay clears: register an **Authenticator app (TOTP)** as a second factor — it is not subject to the new-phone SMS delay — then retry `myaccount.google.com/apppasswords`.
- **TiDB Cloud Starter cluster created and ACTIVE** (org "BloodBuddy", org id `1372813089209366463`):
  - Instance **BloodBuddy**, instance id `10506285093605772922`, TiDB **v8.5.3**, AWS, region **Singapore (ap-southeast-1)**, Zonal, plan **Starter** — $0 spending limit, no credit card required.
  - Connection (public endpoint, branch `main`): host `gateway01.ap-southeast-1.prod.aws.tidbcloud.com`, port `4000`, user `3KKFmsXqsbQDodT.root`. **The root password is deliberately NOT recorded anywhere in this repo** — it lives only in the IntelliJ run configuration.
  - The client IP was auto-allowlisted (`27.34.64.74`). A Nepali ISP address is usually dynamic, so **if the app later dies with an access-denied error, re-add the current IP in the console before suspecting the password.**
  - The Connect dialog's `Database: sys` dropdown and its CA-cert notice were intentionally ignored: our JDBC URL names `bloodbuddy` itself, and `sslMode=VERIFY_IDENTITY` works against the JVM truststore (public CA).
- **IntelliJ run config `BloodBuddyApplication` now carries all four vars** (Run → Edit Configurations → *Modify options* → *Environment variables*): `BB_DB_HOST`, `BB_DB_PORT=4000`, `BB_DB_USER`, `BB_DB_PASSWORD`. **"Store as project file" is left UNCHECKED**, so the password stays in the IDE and never lands inside the project directory — keep it that way.
- **NOT DONE — the first boot has not happened yet.** Two gates remain, in order:
  1. **SQL Editor → `CREATE DATABASE IF NOT EXISTS bloodbuddy CHARACTER SET utf8mb4;`** — still unconfirmed. Starter ships a `test` database and cannot auto-create ours, so this is required.
  2. **▶ `BloodBuddyApplication`** — expect Hikari pool start → Hibernate `create table …` lines → the B1 seeder's hospitals/donors/requests. Then `http://localhost:8081/CODE/HTML/landing.html` (the port moved to 8081 in session #16 — 8080 is held by the EDB PEM Apache service).
- **Repo inconsistency spotted (fix before it misleads a later session):** `application.properties` still has `createDatabaseIfNotExist=true` in the JDBC URL, yet its own comment *and* `backend/README.md` both state that serverless tiers don't support it. The manual SQL step is the reliable path; that dead flag should be dropped.
- **Next:** finish the two gates above, then **B2** — Service layer + DTOs, unchanged from session #14's plan (register/login with BCrypt, donor search + widen fallback, request create, admin forward, donor accept with the BR-1 check and BR-3 race guard, fulfil with the BR-2 transition and BR-4 decrement, guest EM lookup, B4 notification hook points). B2 needs no credentials to write, so it can proceed regardless of the Gmail delay.

## Session #14 (2026-09-16) — B1: entities, repositories, seed data ✅

Written and compile-verified (**BUILD SUCCESS** via IntelliJ's bundled Maven) WITHOUT the TiDB credentials — they are only needed to actually boot. All under `backend/src/main/java/com/bloodbuddy/`.

- **7 enums (`model/`)** — `Role`, `UserStatus`, `BloodGroup` (carries the Appendix A chart in the RECIPIENT→DONOR direction with `acceptsDonor()` — BR-1's authoritative logic now lives server-side; `bb-compat.js` becomes presentation-only in B2+), `RequestStatus` (PENDING→ACCEPTED→FULFILLED; CANCELLED/REJECTED terminal, BR-2; demo label "Matched" = ACCEPTED), `Urgency`, `NotificationType`, `DeliveryStatus`.
- **6 ERD entities + 1 extra (`model/`)** — `User` (single-table, role discriminator — the class diagram's abstract User realized as one FK target; email unique, `passwordHash` for BCrypt), `Donor` (1:1 extension of User, owning side; **§2.5 photo as real `@Lob MEDIUMBLOB` bytes** — frontend Base64 data-URL is unwrapped at the API edge), `Hospital`, `BloodInventory` (unique hospital+group), `BloodRequest` (publicCode unique "BB-/EM-"; dual ownership requester+hospital FKs; single donor FK = BR-3; timeline as `@ElementCollection` → `request_timeline` table), `EmailNotification`. Plus `RequestDecline` (BR-7's per-donor decline persistence; not in the ERD — demo-driven). Operational link `User.staffedHospital` (BR-5 scoping, also not in the ERD). All Lombok `@Getter/@Setter` only — **NOT `@Data`** (generated equals/toString would recurse into lazy collections).
- **7 repositories (`repository/`)** — derived queries only (reference pattern): donor search (group+district+available + widen fallback pieces), hospital-scoped inventory (BR-5), request queries per reader (tracker/hospital queue/admin queue/donor board, paged variants for the pagination scope), `existsByRequestIdAndDonorIsNotNull` (BR-3 accept race guard).
- **`config/DemoDataSeeder.java`** — bb-store.js demo scenario on first boot: 5 hospitals (Bir carries SEED_INVENTORY exactly; Manmohan/NMC sparse with zero rows), 14-donor registry (names/groups/areas/availability from SEED_DONORS), 11 users + 4 pinned-code requests (BB-8F3K2M / BB-2QX9P1 / BB-7HD4LN / BB-5MN8VX) with timelines, sample notification audit rows. **Idempotent**: every write exists-guarded, wrapped in `TransactionTemplate` (an anonymous ApplicationRunner is not reliably `@Transactional`-proxied — real gotcha). Toggle `bloodbuddy.seed=true|false` (in application.properties). All seeded accounts share password `BloodBuddy#2026` (BCrypt at seed time — `spring-security-crypto` added to the pom; B6's Spring Security reuses the artifact).
- **Seed compat corrections (deliberate):** the demo's donor stamps were medically impossible under Appendix A (B+ donor on an O+ recipient; B+ on an AB- recipient). Seeder ships **BB-8F3K2M as recipient B+ (donor Arun B+)** and **BB-7HD4LN as recipient AB+ (donor Priya B+)** — the original pairs are what B2's matching service would reject. Frontend bb-store.js seeds left as-is for now (display strings in demo mode); do NOT carry the old pairs into any backend-backed flow.
- **Not done in B1 (by design):** DTOs and the Service layer (B2), controllers (B3), `spring-boot-starter-security` (B6 — only `spring-security-crypto` is present, no auto-config, so the app still boots wide-open until B6).
- **Next: B2** — Service layer + DTOs: register/login (BCrypt verify), donor search + widen fallback, request create (publicCode generator, recompute compatOk), admin forward, donor accept (BR-1 check + BR-3 race guard), fulfil (BR-2 transition + BR-4 decrement), guest EM lookup (BR-8), email-triggered notifications (B4 hook points). **User-side blockers unchanged**: TiDB cluster under the new Gmail + Gmail app-password revocation (see session #13 addendum) — still outstanding.

## Session #13 (2026-09-16) — backend started: Spring Boot skeleton (B0) ✅

User decisions: backend lives **in this repo** (`backend/` folder, IntelliJ opens just it) · dev email via **MailHog** fake SMTP. Environment findings: JDK 17.0.12 ✅, no Maven CLI (IntelliJ's bundled 3.9.11 used instead — `"C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"`), XAMPP **MariaDB 10.4** server + MySQL Workbench client (fine via `mysql-connector-j`), Spring Boot **3.5.16** chosen over 4.x (mature, tutorial-compatible).

- **`backend/pom.xml`** — spring-boot-starter-{web,data-jpa,validation,mail,test} + mysql-connector-j, Java 17. **Verified: `mvn compile` exit 0.**
- **`backend/src/main/java/com/bloodbuddy/BloodBuddyApplication.java`** — entry point.
- **`backend/src/main/resources/application.properties`** — **TiDB Cloud** datasource (user's choice, replaces XAMPP plan): `jdbc:mysql://${BB_DB_HOST}:${BB_DB_PORT:4000}/bloodbuddy` with **`sslMode=VERIFY_IDENTITY`** (TiDB Cloud mandates TLS), `enabledTLSProtocols=TLSv1.2,TLSv1.3`, Hikari pool capped at 5 (serverless connection limits), explicit **`TiDBDialect`**; creds via `BB_DB_HOST`/`BB_DB_PORT`/`BB_DB_USER`/`BB_DB_PASSWORD` env vars (IntelliJ Run → Edit Configurations); local XAMPP fallback = `BB_DB_PORT=3306` + `BB_DB_SSLMODE=DISABLE`; MailHog SMTP on 1025 via `BB_SMTP_*`; multipart caps 2MB. **Schema must be created once in the TiDB SQL editor** (`CREATE DATABASE bloodbuddy`) — serverless tiers don't support `createDatabaseIfNotExist`.
- **`tools/sync_frontend_to_backend.py`** — copies CODE/, CSS/, images/, resptest/, 404.html into `backend/src/main/resources/static/` (94 files). **static/ is GENERATED + gitignored** — re-run after any frontend edit. Frontend needs ZERO edits: relative asset paths resolve, `/api/**` is same-origin (bb-api.js `BASE=''`), 404.html sits at static root.
- **`backend/README.md`** — run steps, config, layering map.
- **IntelliJ flow:** File → Open → `backend/` → wait for Maven import → create `bloodbuddy` schema in TiDB Cloud SQL editor → set `BB_DB_*` env vars in Run Configuration → (optional) MailHog → run `BloodBuddyApplication` → http://localhost:8081/CODE/HTML/landing.html
- **Next: B1** — 6 JPA entities (USER, DONOR, BLOOD_REQUEST, HOSPITAL, BLOOD_INVENTORY, EMAIL_NOTIFICATION) + Spring Data repositories + seed data, relationships per the proposal ERD (1:1/1:N/M:N, §2.3).

### Session #13 addendum — reference project `SpringWebVir` (user's class example, io.virinchi)

Copied read-only to `reference/` (gitignored — never committed/submitted) from `~/IdeaProjects/SpringWebVir`. Spring Boot 4.1.0, Thymeleaf, session auth, TiDB Cloud + Gmail SMTP.

- **Adopt:** `@Lob`+MEDIUMBLOB image pattern (§2.5 — as `byte[]`), `@RestController`+ResponseEntity style, derived-query repositories, Lombok in entities.
- **Do better than reference (grading-critical):** reference has NO Service layer (controllers→repos directly — §2.2 wants Controller/Service/Repository), returns entities straight from REST (leaks password field), stores plaintext passwords (we: BCrypt in B6), Security permitAll (we: real role rules in B6).

### Session #13 addendum — CREDENTIAL EXPOSURE + account decisions (read this next session)

**What happened:** `SpringWebVir` is PUBLIC on GitHub (`github.com/javasessionwithashish/SpringWebVir` — lecturer Asish's class repo, user's commits ~Aug 21) and its committed `application.properties` contains the user's **TiDB temp-mail password** + **REAL Gmail app password** (16-char app password for sakshammaharjan9@gmail.com — redacted from this file; it was deleted from the Google account in session #15). Bots scan for these — treat as compromised.

**User decisions (session #13):**
1. **New dedicated Gmail** for BloodBuddy (e.g. `bloodbuddy.<x>@gmail.com`) → TiDB Cloud account + cluster live under it, AND it is the §2.4 SMTP sender (notification mail comes from it, not the personal address). Needs 2-Step Verification enabled before it can mint app passwords.
2. **Real Gmail:** user must REVOKE the leaked app password (myaccount.google.com → Security → 2-Step Verification → App passwords → delete). Not optional; still outstanding as of end of session #13.
3. **Old temp-mail TiDB cluster:** abandon (burner account, low stakes; temp addresses get recycled — nothing to rotate if the account is disposable).

**Outstanding user setup before the backend can boot (all browser-side, ~15 min)** — *status as of session #15: item 1 PARTIAL (Gmail created; 2SV sits in Google's security delay, so no app password yet), item 2 DONE, item 3 NOT YET RUN, item 4 DONE — see session #15:*
1. Create the new Gmail + enable 2FA + mint an app password (keep for B4: `BB_SMTP_USER`/`BB_SMTP_PASSWORD`).
2. TiDB Cloud under the new Gmail → Serverless cluster (region: Singapore/alicloud — the class cluster's host was `gateway01.ap-southeast-1.prod.alicloud.tidbcloud.com:4000`) → strong root password.
3. SQL editor: `CREATE DATABASE IF NOT EXISTS bloodbuddy CHARACTER SET utf8mb4;`
4. IntelliJ Run → Edit Configurations → env vars: `BB_DB_HOST` / `BB_DB_PORT=4000` / `BB_DB_USER` / `BB_DB_PASSWORD` (new cluster's values; never in code files).

**Then:** resume at **B1** — 6 JPA entities (USER, DONOR, BLOOD_REQUEST, HOSPITAL, BLOOD_INVENTORY, EMAIL_NOTIFICATION) + Spring Data repositories + seed data, reference-style (Lombok, derived queries) but WITH Service layer + DTOs.

## Session #12 (2026-09-15) — CPJ119 written technical report (§5)

Generated the final project report (reference = Yatra Report PDF as TEMPLATE ONLY, content = proposal + PROGRESS.md + code). Deliverables + generator:

- **Generator:** `tools/report/build_report.py` (python-docx, Arial/Courier New, A4, real TOC field, bracketed figure placeholders) → `tools/report/finalize_word.py` (Word COM: fills TOC page numbers, exports PDF). Optional output-path arg on both. Assets extracted from the proposal DOCX live in `tools/report/assets/` (cover logo, Fig 3-1 architecture, Fig 3-3 ERD, Fig 3-4 class, Fig 6-1 Gantt).
- **Content module:** `tools/report/bb_content.py` — every fact sourced; zero Yatra terms (verified scan: Yatra/eSewa/flight/airline/seat/JWT/Cloudinary = 0 hits). Backend items honestly marked "Pending/Scheduled (backend phase)" — nothing fabricated. Untested claims avoided throughout Ch5/Table 5-1.
- **v1 delivered:** `docs/BloodBuddy_Final_Report.docx/.pdf` (37pp). **v2 (39pp, adds cover logo + the 4 diagram images)** built to `%TEMP%\bbreport\BloodBuddy_Final_Report_v2.docx/.pdf` because Word had the docs copy locked — **v2 is in TEMP and will be wiped; copy it out or rebuild** with the two commands above.
- **Remaining:** insert the 9 screenshot placeholders (Fig 1-1 lifecycle, 3-2 use case, 3-5 DFD, 4-1 file tree, 4-2 search, 4-3 request forms, 4-4 photo upload, 4-5 admin dashboard, 5-1 QA reports); swap v2 into `docs/`; Postman collection lands with the backend phase.

## Session #10 (2026-09-15, COMPLETE) — proposal v2 + CPJ119 compliance

Docs swap done (see Project section). Gap analysis findings: validation partial (alert/toast only, 9× copy-pasted email regex, no blood-format/date rules), fetch 0 calls, pagination 0 markup; responsive/RBAC/search/analytics already covered; image upload missing. User decisions: wire ALL pages now · keep small demo data, page size 5 · build stubbed photo upload now.

**Code-complete (phases A–E):**
- **A. Infrastructure:** bb-validate.js, bb-api.js, bb-paginate.js, CSS/bb-ui.css, donors collection in bb-store.js (all described in Tech/conventions above).
- **B. Validation wired:** auth-login (email+password), auth-forgot (email), auth-register (role-conditional requireds + email/phone/password/terms), contact (name/email/message), requester-request (blood + needed-by required + future-date), emergency-request (blood/district/needed-by + contact phone), all 4 profile pages (names/email/phone + blood/district where present). Old one-off alert/toast validations replaced by shared `checkAll`; errors inline via bb-ui.css.
- **C. Fetch routing (through bb-api.js mock):** login (`auth.login`), forgot (`auth.forgot`), register (`auth.register` — replaces direct users-table write), contact (`contact`), request form + emergency form (`requests.create`), tracking (`requests.list({requester})` + `requests.update` on cancel), search (`donors.search`), hospital dashboard (`inventory.get/update`), hospital queue (`requests.list({hospital})` + update), admin dashboard (`users.list` + `users.setStatus`), admin moderation (`requests.list` + update), profile pages (`profile.get/save` — save path; load path still localStorage).
- **D. Pagination:** requester-search donor grid (pageSize 6, `<nav id="donor-pager">` after `#donor-grid`), admin-dashboard users table (5, `#users-pager`), admin-requests queue (5, `#queue-pager`), hospital-requests queue (5, `#queue-pager`). All: `setTotal(filtered)` + `slice()` in render; onChange re-renders; bar hides when 0 items.
- **E. Photo upload (stub, CPJ119 §2.5):** all 4 profile pages — hidden `#p-photo` file input (JPG/PNG), FileReader → Base64 data URL → live preview round-trip + Remove button; carried as `photo` in the profile payload saved via `profile.save`; hint text notes Base64/BLOB plan. Styles in bb-ui.css `.photo-upload`.

### Session #10 verification (QA harness, 216 → 245 checks, 245/245)

The first run scored **168/201** (201 because failing early-returns skipped checks). Three of the 33 failures were real bugs in shipped code (see "Bugs fixed (session #10)"); the rest were the harness lagging behind session #10's own changes:

- **Validation now blocks submits** — `requester-request` needs `#r-by` (required + future date) and `emergency-request` needs `#e-by`; the harness filled only the old fields, so the submit never reached `POST /api/requests` (and the emergency block then crashed on `added.id`). The harness now fills a **computed** future date (`futureDate(days)` helper) so the check cannot rot with the calendar.
- **Pagination changed per-page counts** — "users table renders 10 rows" → 5/page + a "2 pages" pager check; the admin queue pages at 5 so the harness widens it to 10 via the per-page `<select>` before hunting `BB-5MN8VX` (page 2 otherwise); the donor-search sort check now widens to 20 first, because paging at 6 would only ever prove the first 6 names are sorted.
- **Search ships pre-filled with B+ / Kathmandu + "available only"** (6 donors → 1 page). The new pager checks clear all three filters first to get the full 14-donor registry (3 pages of 6). Worth knowing before "fixing" a search result that looks too short.
- **`requester-tracking` is per-user now** (`GET /api/requests?requester=…`) — it used to render every request in the store, so the harness's shared seeds (Sita's, Anita's) no longer appear. §7 seeds its own requests owned by the logged-in requester; §18d's cancel-hidden check moved to its seeded `QA-FUL`.
- **Profile saves go through the mocked PUT** — the 40ms wait was shorter than the 60ms mock latency, so the toast/save-bar checks raced it (250ms now).
- **New checks (§19):** inline `.field-error` + `.has-error` on a bad login submit (and no session created), `BloodBuddyAPI.isMock() === true` + the Appendix-B facade groups, donor-search pager Prev/Next + page-size 20, and the Base64 photo round-trip (seeded photo → preview `src`, `.has-photo`, Remove visible, `accept="image/png,image/jpeg"`).

### Session #10 loose ends closed (follow-up pass)

- **Profile load now goes through the API.** All four profile pages swapped `loadProfile()` (a direct `localStorage.getItem(STORAGE_KEY)` parse) for `defaults()` plus `API.profile.get(role)` at the end of the script, which fills the form, re-renders the photo, and **re-baselines the dirty check** so an arriving payload never looks like an unsaved edit. The page still paints `defaults()` immediately, so a slow or absent backend never leaves an empty editor, and the session-name seeding that used to live in `loadProfile()` moved into `defaults()` (the mock returns `null` on a fresh browser, so a new user still sees their own name, not the template's). `STORAGE_KEY`/`saveProfile()` stay on purpose as an offline hedge — the mock PUT writes the same key, and `setItem` (never a read) keeps the demo data legible without an API round-trip.
- **Emergency success ID comes from the response.** `await API.requests.create(...)` is now assigned to `created` and the success screen shows `created.id`, instead of reading `requests.all()[0].id` and depending on the mock happening to write synchronously.
- **Search modal send goes through the facade.** `modal-send` now calls `API.requests.create(...)` (the mock normalizes the hospital label and stamps `status`/`created`), and the redirect to tracking hangs off that promise instead of a flat 1200ms timer; a rejected send re-enables the button as "Retry send" rather than stranding the user on a fake success. The page's now-unused `const store` is gone.
- **The two dashboards follow.** `donor-dashboard` (greeting / `#dash-sub` / not-available pill) and `hospital-dashboard` (blood-bank-open pill) read `bb_donor_profile` / `bb_hospital_profile` straight out of localStorage. Both now call `API.profile.get(role)` and paint from the response; `donor-dashboard` paints the session-name fallback synchronously first so the greeting is never blank, and `hospital-dashboard` already ships "Blood bank open" in the markup so only the closed case repaints. `donor-dashboard` gained the one `<script src="../js/bb-api.js">` it needed (bb-store.js is deliberately NOT loaded — the mock's profile branch never touches the store).
- **Visual pass on the request/emergency forms (loose end 6) — clean.** The check ran in the browser rather than by eye: submit each form empty, then read `getComputedStyle` for every error. Both forms show the right number of errors (requester-request 2 = blood + needed-by; emergency 4 = blood/district/needed-by/contact), each `.field-error` computes to `display: block` with a real box (~310x16px, 640px for the full-width contact field), colour `rgb(192,32,42)` on an effective background of white → **6.03:1 contrast** (passes WCAG AA 4.5:1 at 0.74rem), and each control's ring computes to `rgb(192,32,42) 0 0 0 1px inset` — i.e. **`bb-ui.css`'s red ring actually wins**, because it is the last stylesheet on those pages and the page rules only set `width` on `.form-field .filter-select`. No `boxShadow` error styling is left in any page. Tool + artifacts: `resptest/visual-forms.html` (re-runnable, `--screenshot` supported), `resptest/visual-requester-request.png`, `resptest/visual-emergency-request.png`.
- **5 more checks (245 total):** the success screen's EM id must equal the id of the request actually persisted, the send-redirect must fire once the POST resolves, and a **six-page** source guard (4 profile pages + both dashboards) asserts each one calls `API.profile.get(` and does not `getItem` a `bb_*_profile` key — the load-path swap is deliberately *not* externally observable, so the contract is asserted against the served source rather than pretended to be a behaviour.

### Donor side of the lifecycle closed (session #10 follow-up)

The demo used to run requester → admin → hospital with no donor step at all, even though the seeds and the tracker both claimed "Arun Shrestha accepted the request". That made the lifecycle one-sided: a donor could read their history but never act.

- **`CODE/js/bb-compat.js` (new) — the Appendix A compatibility chart, in one place.** It was already copy-pasted into `requester-request.html` and `emergency-request.html` (recipient → compatible donor groups); a third copy on the donor dashboard would have been the point where they drift, so the shared module now exposes `BloodBuddyCompat.forRecipient(g)` / `donorsFor(g)` and both forms read from it. Used by the request forms for the live compat chips and by the dashboard to decide which open requests a donor may answer.
- **Donor dashboard gained "Requests near you" with Accept / Decline.** `#nearby-card` lists open requests the logged-in donor can actually serve, matched through the chart in the correct direction — **Appendix A is written recipient → compatible DONOR groups**, so a B+ donor may serve B+ and AB+ recipients but must NOT be offered an A+ one. Reading the chart backwards (an easy mistake, and invisible in a demo where everyone is O+ or B+) would offer a donor a patient they must not give blood to. Accepting calls `PATCH /api/requests/{id}` (status `Matched`, `donor`, `donorAt`) and writes a timeline entry; declining hides the row for that donor only, without touching the request.
- **`Matched` became a first-class status.** It existed in the seeds but was only ever the *crash* answer (sessions #5's blank queue): `STATUS_META` had no entry. Now labelled everywhere — `Donor matched` in the hospital queue (with a **Mark fulfilled** button that closes the request and deducts the units from stock), `Matched` in the requester tracker as its own tab, and an explicit label in admin moderation instead of the generic fallback.
- **10 more checks (§21, 245 → 261):** the match direction (a B+ donor is offered B+/AB+ and never A+), Accept → `Matched` + donor stamp + timeline + the hospital sees "Donor matched" + fulfilment deducts stock, Decline hides without mutating, and the tracker's Matched tab counts the committed request.

### Real-mode transport verified (session #10 follow-up)

`bb-api.js` had a real `fetch()` transport, but `MOCK = true` meant **no network request was ever made** — the AJAX/Fetch requirement (proposal §1.4, CPJ119 §2.6) was structurally met and invisible at runtime. A grader watching DevTools Network would see nothing. Rather than leave that as a surprise, the fetch path was made runnable and then proven:

- **`tools/mock_api_server.py` (new)** — serves the static site AND an Appendix B REST stub on one port (so relative `/api/...` calls resolve against the same origin), with the request log on stdout. `-p 8322` by default.
- **`resptest/real-mode-check.html` (new)** — with `MOCK = false` and the store pinned empty, it loads 5 representative pages and asserts each rendered its rows over HTTP (16/16). Confirmed against the stub log: `GET /api/donors/search`, `/api/admin/users`, `/api/profile/{donor,hospital}`, `/api/hospitals/inventory`, `/api/requests` — all `200`.
- **A real bug surfaced on the first run: in real mode every call 404'd.** `MOCK = true` shipped with `BASE = '/api'`, but every path passed to `request()` **already begins `/api/`** — so real mode asked for `/api/api/donors/search`. The pages rendered an empty UI with no JavaScript error, and mock mode never reaches the concatenation, so nothing in the existing harness could have caught it. `BASE` is now `''` (same-origin) with the rule documented in the file.
- **5 more checks (§22, 261 → 266)** guard that invariant, because it is invisible until real mode is switched on: `BASE` must not repeat the `/api/` prefix, the composed URL must not contain a doubled `/api/api/`, every `request()` path must be origin-relative, and the file must ship `MOCK = true`. **The guard was negative-tested** — re-injecting `BASE = '/api'` makes §22 fail (266 → 264) and restoring it passes, so it is known to be able to fail rather than merely observed to pass. (Note the first version of the check was itself wrong: counting `/api/` occurrences lets `/api/api/donors/search` pass, because the regex scans non-overlapping — hence the explicit doubled-segment test.)

**Known loose ends / next-session entry points:**
1. ~~QA harness not run yet~~ — **done**, 245/245 (see "Session #10 verification" above).
2. ~~Profile page **load** path still reads localStorage directly~~ — **done** (see "Session #10 loose ends closed").
3. Tracking reads by `session.name` equality (pre-existing); mock `requests.list({requester})` preserves that behavior — fine for demo, note for CPP401 (real auth = server-side filter by user id).
4. ~~`requester-search` modal send (`modal-send`)~~ — **done**; hospital dashboard's direct `store.inventory` reads in `render()` remain store-direct (acceptable: read path).
5. ~~Emergency success screen grabs the new id via `requests.all()[0].id`~~ — **done**.
6. ~~Old `bloodSel.style.boxShadow` inline-error styling — visual pass~~ — **done** (computed-style check + screenshots; see "Session #10 loose ends closed").
7. ~~`donor-dashboard` / `hospital-dashboard` profile reads~~ — **done**, both now go through `API.profile.get`.
8. `about.html`, `donor-history.html`, `landing.html`, `privacy.html`, `terms.html` and root `404.html` do not load `bb-api.js`. All are static/marketing pages, so that is correct today — but any of them must gain it before it grows a request/inventory call. (`donor-dashboard` was on this list and is now off it: it fetches both the profile and the nearby requests.)
- Session #9 added section 18 (role-walk holes): search filters actually filter + widen fallback + sort, request-form hospital persistence, register → users table, tracking Accepted/Fulfilled tabs + plural units + cancel-hidden-when-done + live open-count pill, admin live pending count, admin hospital filter, profile session prefill, hospital pill vs blood-bank toggle, footer legal links.

## Session #11 (2026-09-15) — frontend CLOSED, and what CPJ119 still mandates

**Trigger:** "is everything needed for the front end done?" — worth checking the assignment sheet rather than trusting the summary. Answer: **yes, for the frontend.** Every §2.1 requirement (semantic HTML/CSS/JS, mandatory responsive, client-side validation, dynamic UI, AJAX/Fetch, search+filter, pagination, RBAC, dashboard analytics) is built and verified by the three tools above.

**Work done in this session** (recorded in the three "follow-up" subsections above and in §22):
- The **donor side of the lifecycle** (was the biggest functional gap): `bb-compat.js`, Accept/Decline on `donor-dashboard`, `Matched` as a labelled status everywhere. 245 → 261 checks.
- **Real-mode transport made runnable and proven**: `tools/mock_api_server.py` + `resptest/real-mode-check.html`, 16/16 over real HTTP. 261 → 266 checks.
- **A real bug found doing it:** in real mode every call 404'd (`BASE = '/api'` against paths that already carry `/api/`). See "Bugs fixed (session #10)" #5.
- Remaining gates **negative-tested** rather than assumed: re-injecting `BASE = '/api'` fails §22 (266 → 264).

### Audit — what is still MANDATORY (all backend / deliverable, none of it exists)

Verified against the repo: **0 `.java` files, no `pom.xml`, no `build.gradle`, no `.sql`, no Postman collection.**

| CPJ119 | Requirement | Status |
|---|---|---|
| §2.2 | Spring MVC, Controller / Service / Repository layers | ❌ not started |
| §2.3 | MySQL + Hibernate (JPA annotations, 1:1 / 1:N / M:N, CRUD) | ❌ not started — proposal ER diagram names 6 entities (USER, DONOR, BLOOD_REQUEST, HOSPITAL, BLOOD_INVENTORY, EMAIL_NOTIFICATION) |
| §2.4 | SMTP — **registration confirmation is the only mandatory notification** (login alerts / reset links are optional) | ❌ not started |
| §2.5 | Images stored **in the database** (BLOB or Base64), upload + retrieval via API — *explicitly not as static files* | ⚠️ frontend stub done (FileReader → Base64 → `profile.save`); backend ❌ |
| §2.6 | REST APIs for all major operations + **Postman collection exported as JSON, submitted with the project** | ✅ session #21 — `docs/postman/BloodBuddy.postman_collection.json` (48 requests) |
| §5 | Written technical report per the Teams guidelines | ❌ not started |

**Two traps this audit surfaced:**
1. **§2.2: "All critical application logic must reside in the backend. The frontend should only handle presentation and basic input validation."** The business rules currently live in the BROWSER — the Appendix A chart (`bb-compat.js`), the donor-match direction, the RBAC split, the `Matched`/`Fulfilled` transitions. Those have to move into the Service layer, deliberately, or that grading criterion is at risk. The facade and Appendix B endpoint list are already shaped for it, so it is a move rather than a rewrite.
2. **`tools/mock_api_server.py` is NOT the backend and must not be presented as one.** It is a Python test stub (session #11) written only to prove the fetch layer works. It satisfies none of §2.2–§2.3. The deliverable is a Spring application.

**Submission-process risk:** 61 files are uncommitted (including all of `CODE/js/*.js`, `tools/`, `resptest/`); the repo has 2 commits total and `origin` is set but nothing has been pushed. Sessions #10 and #11 exist only on disk.

**Note on numbering:** the four "follow-up" subsections above (`Session #10 loose ends closed`, `Donor side of the lifecycle closed`, `Real-mode transport verified`) cover sessions #10–#11; they are grouped under #10 because they are all the same body of work — proposal v2 + CPJ119 §2.1 compliance.

## Bugs fixed (session #10 — the five real defects these runs caught)

1. **`admin-requests.html` threw a `SyntaxError` and the whole moderation queue died.** A leftover `)` from the session #10 `PATCH` refactor closed the forward-branch patch object as `});` instead of `};`. One bad character took out the ENTIRE inline script: no cards, no pager, no tabs, no item counts — every admin-requests check failed and every later "0 cards" failure (hospital filter, EM badge, unrouted filter) traced back to it. `node --check` on the page's inline script is the fastest way to catch this class of bug.
2. **`requester-profile.html` was a botched edit — it could never have loaded.** Three separate defects: a duplicated early `const FIELDS`, a duplicated `saveProfile()`, and **no `const STORAGE_KEY = 'bb_requester_profile'` declaration at all** (every other profile page declares it), so `saveProfile()` referenced an undeclared binding. Rebuilt the block to match `donor-profile.html`'s order (`API`, `V`, `STORAGE_KEY`, `DEFAULTS`, `loadProfile`, `saveProfile`, `FIELDS`, …). Node flagged the duplicate `FIELDS`; the missing `STORAGE_KEY` was only visible by comparing against the sibling pages.
3. **Donor search was completely dead: `GET /api/donors/search` had no mock route.** The facade called `/api/donors/search` (correct — it is Appendix B's endpoint) but the mock router only handled `GET /api/donors` and `GET /api/donors/{id}`, so the literal segment `search` was parsed as a donor id and every search rejected with `ApiError: Donor not found` — the donor grid rendered 0 cards on every load, in every role. The router now matches `search` explicitly, and **before** the `/{id}` lookup (checking it after would still read the segment as an id). This also silently means "search" was never a valid donor id — the ordering is load-bearing, not stylistic.

4. **`404.html` was not responsive — ~91px of horizontal overflow at 360px.** CPJ119 2.1 makes responsiveness mandatory, and this was the one page a visitor reaches from *any* bad URL. `404.css` had zero media queries and the page deliberately does not load `landingresponsive.css` (it is self-contained with root-absolute paths), so `.footer-inner` stayed a flex ROW from landing.css: at 360px the logo, copy and four footer links ran to x≈451 and made the whole document scroll sideways. Added its own stacking breakpoints to `404.css` (footer column ≤768px, wrapping centred links + tighter card padding ≤480px) rather than adding the shared responsive stylesheet — the page keeps working even if that file is missing, which is the point of a 404. Found by the new `responsive-check.html`, not by eye: the existing manual screenshots only covered landing/login/register, so a hand-check would plausibly have missed this page entirely.

5. **Real mode would have 404'd on every single call.** `bb-api.js` shipped `MOCK = true` with `BASE = '/api'`, while every path passed to `request()` already begins `/api/` — so with `MOCK = false` the browser asked for `/api/api/donors/search` and every endpoint missed. The failure mode is the nastiest kind: the pages render empty with **no JavaScript error**, and mock mode never reaches the concatenation, so the existing 261-check harness could not have caught it. Found by actually running the fetch path against `tools/mock_api_server.py` (the stub's log showed `/api/api/...`); fixed by `BASE = ''` and re-verified at 16/16 over real HTTP. §22 of the harness now guards it, and that guard was negative-tested.

## Bugs fixed (session #9 — walked every role's flow end-to-end, then filled the holes)

1. **Search page filters did nothing** (`requester-search.html`). The 8 hardcoded donors were all B+/B- in Kathmandu, but `#search-btn` only filtered availability — blood group/district were cosmetic, and every donor search for "O- in Pokhara" returned the same Kathmandu B+ list. Added donors across groups/districts (14 total) and made the filter real: blood group + district + availability, with a **widen fallback** (if the group has no donors in the chosen district, show that group in all districts with an explanatory line — an emergency search should never dead-end at "0 results"). The sort select now works too (Availability / Name A–Z).
2. **Requesting an unavailable donor was impossible.** Cards rendered a dead "View profile" button for unavailable donors. Now every card offers Request; unavailable donors get a muted style and a modal note that the request waits until they toggle back.
3. **Request form threw away the chosen hospital** (`requester-request.html` stored `'—'`) and the emergency form stored the raw option label. Both persist the real hospital now, normalized through the new `BloodBuddyStore.normalizeHospital()` (bb-store.js alias table: TUTH/full name/Bir/Patan/Manmohan/NMC → canonical labels) — so requests forwarded or submitted under variant names actually land in the hospital queue's equality filter.
4. **Registered accounts were invisible to the admin.** Register never wrote `bb_db.users`, so the admin's user-management table only ever showed seeds. Registration now inserts a row (POST /api/auth/register later). Also fixed `auth-register.html` loading `bb-store.js`.
5. **Donor dashboard had a dead end + fake UX.** The "Active Requests Nearby" stat card linked to `requester-search.html`, which is requester-only — RBAC bounced the donor straight back to the dashboard (link went nowhere, permanently). Now points to `donor-history.html`. The greeting used a hardcoded "Saksham" until a profile was saved (now falls back to the session first name), and "Respond Now" no longer claims "Check your email" (demo has no donor-response action yet; CPP401 adds it).
6. **Donor/requester profiles prefilled with template names.** A fresh browser showed "Saksham Maharjan" / "Ramesh Shrestha" regardless of who logged in. Profiles now seed from the session name when nothing is saved (hospital/admin stay template-based until register collects a hospital name).
7. **Tracking page hid half the lifecycle** (`requester-tracking.html`): Accepted and Fulfilled requests existed in the store but matched no tab, so they vanished from every filter; cancel was offered even on Fulfilled/Rejected requests; multi-unit cards said "2 units" only by accident (`'4+' > 1` string compare); the hero pill's open-count was hardcoded HTML. Added Accepted/Fulfilled tabs, hidden cancel for terminal statuses, `parseInt` pluralization, and a live open-count pill (open = not Completed/Fulfilled/Cancelled/Rejected).
8. **Hospital dashboard pill contradicted the profile toggle.** The hero always said "Blood bank open" even after `bb_hospital_profile.open = false` (hospital-profile's toggle). Now mirrors it.
9. **Admin dashboard hardcoded "2 blood requests awaiting review"** — now read from the store (`status === 'Pending'`), so the shortcut card stays true as requests flow through.
10. **Admin moderation's hospital filter was dead** — rendered, never wired. It now narrows the queue (status resets to All when used) and gained a "Not routed yet" (`—`) option so the admin can find unrouted requests; the forward modal got a comment pinning its labels to the canonical store labels.
11. **Terms/Privacy were only linked from the register checkbox** — added to the site-wide footer of all 22 pages (root-absolute on 404.html). (This was PROGRESS.md's first remaining polish idea.)
12. QA: 18 new harness checks (section 18) covering each fix; 198 → 216. First run failed 3 — all three were harness bugs (asserted seeds that only exist after other pages run: REQ-498 and the REQ-50x hospital seeds; the checks now seed their own data via `seed:`), which is also a good reminder that page-level seeds are load-order dependent in the demo.

## Bugs fixed (session #8)

1. **Profile chip fell back to the dashboard for requester/hospital/admin** (the last nav polish item from sessions #5–#7). Built the three missing profile editors — `requester-profile.html`, `hospital-profile.html`, `admin-profile.html` — cloned from the `donor-profile.html` pattern (same hero treatment with each role's own image rotation, save bar + discard + toast + beforeunload guard, per-role toggle: urgent-alerts / blood-bank-open; admin gets read-only role/since fields instead of a toggle). Styles live in shared `CSS/profile.css` (donor-dashboard.css's profile block extracted + hero pieces from hospital.css). nav.js: `PROFILE` now points every role at its real page, `ROLE_LINKS` gains a My Profile link per role (chip stays the direct-link pattern), `RBAC_GUARD` covers all four profile pages (donor-profile's separate donor-only guard IIFE folded into the map — same rule), `canAccess` simplified accordingly. QA: render smoke ×3 (right role + logged-out bounce), per-page save flow (edit → save bar → persist → toast), chip destination checks for requester/hospital/admin, wrong-role bounces for hospital/admin profiles. 161 → 198 checks.

## Bugs fixed (session #7)

1. **"Request blood" CTA shown to logged-in donors** (user report: "the request blood option doesn't make sense in the donor portal"). The CTA band on landing/about has two buttons; nav.js only rewired the register button (`ROLE_CTA`), leaving the secondary "Request blood" → `requester-request.html` visible to every role. Since submitting a request is a requester action, nav.js now removes in-page `requester-request` CTAs for non-requester sessions (requesters keep it; logged-out visitors keep it). CTA band for a donor now shows only "Go to Donor Portal". QA: donor-session check flipped to "CTA removed" + band-has-1-button, new requester-session check confirms it's kept.
2. **Portal pages had no access control** (user report: "when I click this I can access the hospital and the donor interface even when I'm just a requester or logged out — a big security concern"). Landing's "Open X portal" cards (and any typed URL) went straight to the dashboards. Fix, all in nav.js so it covers every page: (a) an RBAC guard map (`RBAC_GUARD`) bounces logged-out visitors to `auth-login.html` and wrong-role users to their own dashboard before the page renders; (b) `donor-profile.html` is donor-only (same guard shape); (c) on public pages while logged out, hard-coded links to private pages (the portal cards) are rewired to `auth-login.html` with the original target kept in `data-bb-return`; (d) the login page honors a stashed return URL — `sessionStorage` bounce or `data-bb-return` from the clicked link — but only when the role picked at login may access it (`BloodBuddyNav.canAccess`). Requester pages are member-only too (`RBAC_GUARD` in nav.js) — guests use the public emergency flow instead. QA: private pages smoke-tested under the right session AND logged out (bounce asserted), wrong-role bounce checks (requester→admin, donor→hospital, hospital→donor-profile), login-return (right role returns, wrong role ignored), portal-card rewire for logged-out visitors.


## Bugs fixed (session #6)

1. **requester-tracking.html — rendered 0 cards on fresh load.** Root cause was simple: the inline script defined `render()` but only ever called it from the tab-click and cancel handlers — never on load. Added the initial `render()` call. Also fixed the empty-state logic: it used to unhide `#trk-empty` whenever the *current filter* had no matches (e.g. a filter with 0 results covered the list); now the box only shows when the store itself is empty.
2. **landing.html + logged-in session: no Dashboard link.** Initially fixed by *inserting* a Dashboard link on landing (its logo anchors `#home`, no Home link exists); **superseded later the same session by #5's full `ROLE_LINKS` rebuild**, which handles landing and every other page uniformly — see #5 for the final behavior.
3. **Register Terms/Privacy placeholders** now point to new `terms.html` / `privacy.html` (shared `CSS/legal.css`, same hero treatment + navbar/footer as other pages).
4. **Nice-to-have:** "Reset demo data" button on admin-dashboard users toolbar → `window.confirm` → `BloodBuddyStore.reset()` → reload.
5. **Logged-in navbar loses your portal links on public pages** (user report: "clicking About throws me out even though I'm logged in"). Pages ship a static logged-out navbar, so About/Contact reverted to marketing links (How it works / For hospitals) with no way back to the portal except browser back. Fix: nav.js now rebuilds `.nav-links` from a per-role `ROLE_LINKS` table on every page while logged in, preserving About/Contact links (and their `.active` class). Replaces the old "Home becomes Dashboard" rewrite.
6. **donor-history.html had the wrong navbar** (requester links, "Find Donors" marked active) — copy-paste slip; now matches donor-dashboard (Donor Portal / My Profile / About).
7. **Emergency CTA routed through signup** (user report: "Find blood now is for emergencies but takes you to register"). The search page was already fully usable logged-out (filters, request modal, soft "log in for full results" nudge, `?blood=&district=` prefill), so the wall was pointless: hero "Find blood now" → `requester-search.html` directly; bottom CTA "Request blood" → `requester-request.html` directly. "Register as donor" stays a register link (correct for its audience).
8. **Search page request modal persisted nothing** — it showed "Request sent ✓" and redirected but never loaded `bb-store.js`, so the request vanished from the tracker. Now loads `bb-store.js` and persists a real Pending request (donor's group/district, urgency + hospital + notes from the modal, requester from session or "You") that flows through hospital/admin/tracking like any other.
9. **Profile chip was dead on click** (user report: "clicking my profile icon should take me to my profile"). Chip is now an `<a>` → role's profile page (`PROFILE` map in nav.js: donor → `donor-profile.html`; requester/hospital/admin → their dashboard until dedicated pages exist — **superseded in session #8**, which built those pages and pointed `PROFILE` at them). Hover ring + lift added in `landing.css`.
10. **Logged-in users still saw "Register as donor" CTAs** (user report: logged-in donor on about.html sees a donor-register button). nav.js now swaps every `auth-register` CTA outside the navbar to the role's destination (`ROLE_CTA`): donor → "Go to Donor Portal", requester → "Find compatible blood" (search), hospital/admin → their portal. Logged-out pages keep the marketing CTAs. Also: landing quick-search's "log in or register for full results" nudge is now session-aware (skipped when logged in), and about.html's "Request blood" CTA got the same logged-out signup-wall fix landing already had (→ `requester-request.html`).

## Bugs fixed (session #5)

1. **requester-search.html — wrong donor in request modal after filtering.** `renderDonors(list)` wrote `data-i` = index in the *filtered* list but `openModal` looked up `DONORS[i]` in the *unfiltered* array. Fixed: buttons carry `data-name`, modal resolves by name. Harness check: filter B+ + available, click card #3 (Priya) → modal must show Priya.
2. **hospital-requests.html — blank queue on fresh browser.** Seed `BB-8F3K2M` (status `Matched`, Bir Hospital) hit `STATUS_META[r.status]` → undefined → throw mid-render. Fixed with fallback `{ cls:'st-pending', label:r.status }`. Also: `fmtDate` now guards empty `neededBy`, units pluralize via `parseInt` (handles `'4+'`).
3. **admin-requests.html — same `Matched` crash.** Same fallback fix + same fmtDate guard.
4. **requester-tracking.html — units pluralization** in detail modal (`'4+' > 1` string compare) — fixed with `parseInt`. (The page's bigger bug is still open, below.)
5. **Dead links wired:** all footers (12 files) + any navbar `#` About links now point to `about.html` / `contact.html` (verified none left except Terms/Privacy placeholders in register).

## Also delivered in session #7 (feature notes)

- **Session #7c — public emergency page + requester portal locked.** New `CODE/HTML/emergency-request.html` (public): guest urgent-request form — blood group + units + needed-by + district + hospital + contact + notes, Appendix-A compat chips, red hero with a "call 102" reminder pill, and a success state that shows the request ID (guests have no tracker; the "My Requests" link on it points to login, or to requester-tracking for logged-in requesters). Submissions persist to the shared store as `EM-*`, requester "Guest (emergency)", urgency Urgent, so they flow through admin/hospital queues like any request. Landing's guest CTAs now point here — hero "Find blood now" (data-em="find") and CTA band "Request blood" — and about.html's "Request blood" follows; nav.js swaps them per session: requester → search/form, other roles → portal ("find") or removed ("request blood"). The requester pages (search/request/tracking) moved into `RBAC_GUARD` — guests are bounced to login (landing's "Open requester portal" card auto-rewires via the logged-out rewire), other roles to their dashboard; the emergency page's login/register nudge (`#em-nudge`) is removed for members. The quick-search nudge text on landing still points guests at register for "full results" — acceptable: search results need an account, emergency doesn't.
- **Session #7d — guest status lookup + Emergency badges.** The emergency page grew a public "Already submitted? Check its status" card: enter the EM-… ID → status pill (same `st-*` styles as the tracker), blood/units/district/hospital, and the full timeline from the store; unknown IDs get a friendly not-found error, and **member request IDs (BB-*) are rejected** so the guest lookup can't be used to read members' requests. The success screen's "check its status here" link now jumps to the lookup with the ID pre-filled. Hospital + admin queues tag guest requests with a red **⏺ Emergency** pill (`.em-tag` in hospital.css/admin.css, shown when the id starts with `EM-`); member cards are untagged.

## Next steps

**The frontend is DONE. Everything below is backend (CPP401) and is listed in dependency order.**

0. ~~**IMMEDIATE (session #16 hand-off):** press ▶ once to confirm the fixed seeder completes.~~ **DONE in session #17** — the boot was run from the terminal (logged in `backend/target/bb-boot.log`): seeder completed, app stayed up, landing served 200, and the `workspace.xml.bak-*` backup is deleted. The app serves at **http://localhost:8081/…** (start it however you like — IntelliJ ▶ or the maven recipe in "Session #17").
0b. ~~**B2 — Service layer + DTOs.**~~ **DONE in session #17** (DTOs + 7 services, 25/25 smoke checks against TiDB, 2 real bugs fixed).
0c. ~~**B3 — REST controllers + error mapping.**~~ **DONE in session #17** (7 controllers, `@ControllerAdvice` 404/409/400, `ActorContext` resolver, 56/56 HTTP checks; §2.5's BLOB photo endpoints landed here too, so B5 is effectively done).
0d. ~~**B6 — Spring Security: role rules + session login.**~~ **DONE in session #18** (unauthorized 401 / forbidden 403 as `ApiError` JSON, `JSESSIONID` session login + logout, the dev `X-BB-*` header removed, `@PreAuthorize` on the admin/hospital surfaces, 74/74 HTTP checks on a cookie jar and the B2 smoke run still 25/25 with security on).
0e. ~~**B4 — SMTP delivery on the `NotificationService` hooks.**~~ **DONE in session #19** `NotificationMailer` sends and the audit row is stamped SENT/FAILED with the body stored; 28/28 smoke checks with the catcher up, 26/28 with it stopped (exactly the two mail-arrival checks) so "an SMTP outage is recorded, never fatal" is verified rather than assumed. Dev catcher = Mailpit in gitignored `.tools/`.
0f. ~~**B7 — flip `MOCK = false`.**~~ **DONE in session #20** — real transport by default (with a `bb_force_mock` override that keeps the mock suite alive), session reconciled from `GET /api/auth/me`, real logout, register sends `password`; **268/268 mock + 25/25 real**, and a registration landed a row in TiDB and a mail in Mailpit. Remaining gap (deliberately small): a NEW hospital registration still has no `staffedHospital` link.
0h. ~~**The two criteria gaps: the M:N relationship (§2.3) + the staffed-hospital link (BR-5).**~~ **DONE in session #22** — `Donor.affiliatedHospitals` (`@ManyToMany`, join table `donor_hospital_affiliation`) with a same-district seeded rule, read from both sides (`GET /api/donors/{id}/hospitals` + the hospital-side queries), and `HospitalResolver` links a hospital-staff registration from the NAME the form collects. **31/31 smoke, 81/81 real HTTP.** What remains from it: **re-run the mock harness (268/268 — the frontend edits are newer than its last run)**, extend `real-backend-check.html`, update `backend/README.md` / `docs/DEMO_RUNBOOK.md` / the report's relationship row, and clear the DB footprint the last `api_check.py` run left (`.tools/api_check.log` prints the SQL; BB-5MN8VX is currently routed, so the `?unrouted=true` check will fail until it is run).
0g. ~~**B8 — the Postman collection (§2.6).**~~ **DONE in session #21** — 48 requests, cookie-based login capture, static validation against the controller routes. **Every mandatory CPJ119 item is now built.** What is left is submission polish: fill the report's 9 screenshot placeholders, and the **demo runbook** (already written: `docs/DEMO_RUNBOOK.md`).

1. ~~**MANDATORY — Spring MVC backend (§2.2).**~~ **DONE (B0–B3, B6):** three layers, and the logic that used to live in the browser (Appendix A compat, the donor-match direction, the status transitions, the RBAC split) is in the backend — the frontend facade now only presents. Initialise the project, then the three layers: Controller (thin, HTTP only), **Service (this is where `bb-compat.js`, the donor-match direction and the status transitions must end up** — §2.2 says *all* critical logic belongs in the backend and the frontend only presents), Repository/DAO (Spring Data JPA).
2. ~~**MANDATORY — MySQL + Hibernate (§2.3).**~~ **DONE (B1):** six entities live in TiDB with the 1:1/1:N/M:N relationships and `ddl-auto=update`. The six entities from the proposal ER diagram (USER, DONOR, BLOOD_REQUEST, HOSPITAL, BLOOD_INVENTORY, EMAIL_NOTIFICATION) with JPA annotations and the 1:1 / 1:N / M:N relationships, plus CRUD. `bb-store.js`'s `requests` / `donors` / `users` / `inventory` collections map onto them one-to-one.
3. ~~**MANDATORY — registration confirmation email via SMTP (§2.4).**~~ **DONE in session #19.** The only mandatory notification (donor urgent-request alerts are proposal-level extras, delivered too). Every credential comes from env vars, never hardcoded, and the provider is a config swap.
   - **Still open, purely cosmetic for the demo:** switching from the local Mailpit catcher to real Gmail delivery — one 4-variable env change, blocked only by Google's new-phone 2SV delay for `bloodbuddy1org@gmail.com`.
4. ~~**MANDATORY — images stored in the database (§2.5).**~~ **Effectively DONE in session #17 (B3):** `POST /api/donors/{id}/photo` stores the Base64 photo into a real `MEDIUMBLOB` column and `GET …/photo` returns the bytes with a sniffed content type — explicitly not a file path.
5. ~~**MANDATORY — Postman collection exported as JSON (§2.6).**~~ **DONE in session #21, extended in session #22 (50 requests)** — `docs/postman/BloodBuddy.postman_collection.json`, cookie-based login capture, statically validated against every controller route.  `bb-api.js`'s endpoint list and `tools/mock_api_server.py`'s stub were the checklist.
6. **MANDATORY — the written technical report (§5)** per the Teams documentation guidelines.
7. Remaining polish ideas (optional):
   - A matching Password Help link on the login page footer
   - A chip dropdown (Profile / Log out) instead of a direct profile link
   - More request lifecycle statuses in the demo timeline
   - ~~A donor-side view of requests (donors still can't accept/fulfil anything)~~ — **done** in the session #10 follow-up: `bb-compat.js` + Accept/Decline on `donor-dashboard`, and `Matched` is now a labelled status in the tracker, the hospital queue and admin moderation.
8. ~~When backend starts (CPP401): **flip `MOCK = false` in bb-api.js**~~ — **DONE in session #20** (see the session #20 section; the mock path survives behind `localStorage.bb_force_mock = '1'`). Kept for the reasoning: (that was the whole swap — the store-as-shim design from bb-store.js's old header comment is superseded; bb-api.js's header documents every endpoint). The fetch path is **proven, not just written**: run `python tools/mock_api_server.py -p 8322` and open `http://localhost:8322/resptest/real-mode-check.html` (see the tooling section). Two things to keep intact — `BASE` must stay `''` or an absolute origin (never a path prefix, since every endpoint already carries `/api/`), and the demo's `bb-*.js` files must keep loading before the inline scripts. The session model (`bb_session`) maps to real auth then; `ROLE_LINKS`/`ROLE_CTA`/`PROFILE` in nav.js stay, just driven by the server-side role. Profile pages swap `profile.get/save` mock for `GET /api/auth/me` + role-scoped PUT (already routed through bb-api — no page edits expected).
