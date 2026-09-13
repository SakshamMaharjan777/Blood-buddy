# BloodBuddy — Project Progress & Context

> Read this file first in any new session. It summarizes the proposal, what's built, and what's next.
> **Update this file at the end of every session** (status, new files, next steps).
> Last updated: 2026-09-13 (frontend session #4, nav pass)

## Project

**BloodBuddy** — web-based blood donor management system for Nepal (academic project, CPP400/CPP401, Asia e University, BICT, Saksham Maharjan).

- **Proposal doc:** `docs/BloodBuddy_Proposal_Revised.docx` (copied into the repo 2026-09-13; original at `C:\Users\saksh\Downloads\BloodBuddy_Proposal_Revised (1).docx`)
- **Backend (planned, CPP401):** Spring MVC + Hibernate ORM + MySQL, SMTP email notifications, BLOB image storage, REST API tested via Postman
- **4 roles (RBAC):** Donor, Requester, Hospital Staff, Administrator
- **5 partner hospitals:** TUTH, Bir Hospital, Patan Hospital, Manmohan Memorial Community Hospital, Nepal Medical College Teaching Hospital
- **Key API endpoints (proposal Appendix B):** `/api/auth/*`, `/api/donors/search`, `/api/requests`, `/api/hospitals/{hospitalId}/inventory`, `/api/hospitals/{hospitalId}/requests`, `/api/admin/users`
- **Blood compatibility chart (Appendix A):** used in requester-request.html JS (`COMPAT` map) — real matching logic belongs in the Spring Service layer later

## Tech / conventions (frontend)

- Pure HTML/CSS/JS, no frameworks, no build step
- Fonts: DM Serif Display (display) + DM Sans (body); brand red `#C0202A`; bg `#F7F6F4`; radius 12 cards; inset-ring shadows
- Shared: `CSS/landing.css` + `CSS/landingresponsive.css` (navbar/footer/buttons) + per-page CSS
- Inline `<script>` at bottom of each page + shared `CODE/js/nav.js` (session-aware nav, loaded first) + `CODE/js/main.js` (ripples/tilt)
- **Session model (demo):** localStorage `bb_session` = `{ role, name }`, set by login/register via `BloodBuddyNav.setSession()`. When set, nav "Home" becomes "Dashboard" → that role's dashboard, auth buttons become a user chip + Log out; Esc returns to the dashboard (skips open modals). Absent key = logged out.
- Hero treatment: photo + red gradient, hover zoom, `.hero-pill` badge — images rotate across pages:
  - `tim-marshall-cAtzHUz7Z8g-unsplash.jpg` → requester-search, hospital-dashboard
  - `hero-donation.jpg` → requester-request, hospital-requests (`.alt-hero` class)
  - `faezeh-eslami-Btja3lr5ldk-unsplash.jpg` → donor-dashboard, donor-history, requester-tracking
  - `adrian-sulyok-sZO8ILzGKcg-unsplash.jpg` → admin-dashboard
- All data is **demo/hardcoded or localStorage** until the backend exists

## Pages built (status)

| Page | Purpose | Status |
|---|---|---|
| `landing.html` | Public marketing page | ✅ pre-existing |
| `auth-login.html` / `auth-register.html` | Auth screens | ✅ pre-existing |
| `donor-dashboard.html` / `donor-history.html` | Donor role | ✅ pre-existing |
| `requester-search.html` | Find donors (filter chips, donor cards, request modal) | ✅ pre-existing, edited: nav link to My Requests, urgent CTA → request form, modal send → tracking |
| `requester-request.html` | Submit blood request form (compat chips from Appendix A, compatible-groups toggle) | ✅ built this session |
| `requester-tracking.html` | My Requests tracker (status tabs, timeline modal, localStorage submissions + seeded demos) | ✅ built this session |
| `hospital-dashboard.html` | Blood bank inventory (8 group cards, Healthy/Low/Critical pills, low-stock alerts, adjust-stock modal) | ✅ built session #2 |
| `hospital-requests.html` | Incoming requests queue (Accept/Reject/Mark fulfilled, status tabs) | ✅ built session #2 |
| `admin-dashboard.html` | Platform admin (KPI cards, donations bar chart, users-by-role donut, users table with role tabs/search/suspend-restore confirm modal) | ✅ built session #3 |
| `admin-requests.html` | Request moderation (platform-wide queue, status tabs, forward-to-hospital modal, flag-as-spam) | ✅ built session #4 |
| `auth-forgot.html` | Password reset request (email form, demo "check your inbox" state) | ✅ built session #4 |
| `donor-profile.html` | Donor profile editor (availability toggle, blood group/contact/district form, unsaved-changes bar, toast; persisted in localStorage key `bb_donor_profile`) | ✅ built session #4 |

CSS added: `requester-request.css`, `requester-tracking.css`, `hospital.css` + `admin.css` (both self-contained: local copies of modal/tab/card styles since these pages don't load requester-*.css).

Auth flow notes (session #4): login has a role picker — demo redirect goes to that role's dashboard (requester → requester-search.html); register's final CTA navigates to the role dashboard; "Forgot password?" links to `auth-forgot.html`.

Donor profile notes (session #4): `donor-profile.html` shares `donor-dashboard.css` (profile form styles appended). Saves to localStorage `bb_donor_profile`; `donor-dashboard.html` reads it to personalize greeting (name, blood group, district) and the availability pill. Later backend: `GET /api/auth/me` + `PUT /api/donors/{donorId}`.

## Next steps

1. Visual QA pass in Chrome of all pages (admin-dashboard + admin-requests verified headless: charts/table/queue render; requester-search `?blood=&district=` params verified)
2. Nice-to-have pages (not mandatory): real `about.html` / `contact.html`, branded `404.html`
3. All 4 role dashboards exist — when backend starts (CPP401): swap localStorage/hardcoded data for `fetch()` calls to Appendix B endpoints (submit → `POST /api/requests`, tracking → `GET /api/requests/{id}`, search → `GET /api/donors/search`, inventory → `GET/PUT /api/hospitals/{hospitalId}/inventory`, users → `GET /api/admin/users`, moderation → `GET/PATCH /api/requests`)
