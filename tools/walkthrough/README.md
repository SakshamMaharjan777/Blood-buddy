# `tools/walkthrough` — the demo runbook, driven by a browser

`docs/DEMO_RUNBOOK.md` §1 is a click script for the CPP401 defence. This tool
**runs that script** against the live app and **screenshots every checkpoint**, so
the rehearsal is evidence rather than a promise — and so a page that breaks
between two sessions is caught before the evaluator sees it.

It is not a substitute for the operator's own rehearsal. It is the safety net:
the two things a human has to *look* at (the hospital queue showing a TUTH
session its own rows, and one welcome mail per registration) are both in here
with their numbers attached.

## What it does

- logs in through the **real login form** for all four roles — a broken login
  screen fails the run, which a planted cookie would hide;
- **clicks** the real controls: the register wizard, the donor-search filters,
  the request form, the donor's Accept, the stock-adjust modal, the hospital's
  *Mark fulfilled*, the admin's *Suspend* and *Forward*;
- **asserts** what each checkpoint claims (an inline validation error with no
  `alert()` dialog, exactly one welcome mail, the widen fallback's sentence, the
  O− request *not* offered to a B+ donor, TUTH's own queue with no foreign
  hospital on it, the suspend's reason at the next login, 401/403, and the three
  suites) and writes PASS/FAIL for each;
- **screenshots** each step and **self-checks that every shot is a distinct
  picture** — a screenshot that is the previous frame, or that photographs the
  wrong part of the page, looks like evidence without being any.

## Before you run it

| Need | Why |
|---|---|
| the app on `localhost:8081` | every page and API call goes to it |
| Mailpit on `127.0.0.1:8025` | the §2.4 mail check reads its inbox over its API |
| a pristine demo database | the checkpoints assert stock `12 → 13 → 11` and one unrouted request |
| Chrome | `BB_CHROME` overrides the default Windows path |

```bash
.tools/mailpit/mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
# ... and the app, per runbook §0A
```

## Run it

```bash
cd tools/walkthrough
npm install                                    # once: puppeteer-core

node walkthrough.js                            # every checkpoint (~5 min)
node walkthrough.js --first 26                 # the click-through half only
node walkthrough.js 28 29                      # just those steps (see the numbering below)
node gallery.js                                # build the browsable index.html
```

The three slow self-driving suites (27–29: `real-backend-check`, `qa-harness`,
`responsive-check`) take most of the time; `--first 26` skips them.

Overridable with environment variables: `BB_BASE`, `BB_MAILPIT`, `BB_CHROME`,
`BB_WALK_OUT` (where the shots go — default `.tools/wt/walkthrough/`, gitignored),
and `BB_WALK_EMAIL`, which lets one step be re-run against the account an earlier
pass registered:

```bash
BB_WALK_EMAIL=walk-1789665456209@example.com node walkthrough.js 21
```

## Where the results land

Gitignored, next to the other dev scratch: **`.tools/wt/walkthrough/`**

| File | What it is |
|---|---|
| `index.html` | the gallery — open this, every checkpoint with its caption |
| `walkthrough.txt` | the machine-readable record: steps, every assertion, the shots, and the distinctness self-check |
| `01_…` … `29_…png` | the screenshots, numbered in runbook order |

**It writes to the demo database** — that is the point, it is not a simulation.
Afterwards put the data back with runbook §0G / §5 and empty Mailpit before the
next rehearsal.

## The steps

| # | Checkpoint | # | Checkpoint |
|---|---|---|---|
| 01–03 | landing (1440px, then 360px), the public emergency page | 17a/b | stock adjust (modal, then B+ 12 → 13) |
| 04–07 | register: role picker, inline validation, `201`, the welcome mail | 18–19 | TUTH's own queue, then *Mark fulfilled* (B+ → 11) |
| 08–09 | donor search, then the widen fallback | 20–21b | admin dashboard, suspend modal, the suspended row |
| 10–11 | the request form and its `201` | 22–23 | forward the unrouted request; the suspended login is refused |
| 12–13 | the tracker, then the donor alert mails | 24–26 | the RBAC page guard (both halves), 403, 401 |
| 14–15 | the donor's board, then Accept | 27–29 | `real-backend-check` 42/42, `qa-harness` 268/268, `responsive-check` 92/92 |
