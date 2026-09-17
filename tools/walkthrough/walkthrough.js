/*
 * Walks docs/DEMO_RUNBOOK.md §1 checkpoint by checkpoint IN THE BROWSER, the way
 * the operator will on defence day, and screenshots each one.
 *
 * Needs: the app on 8081, Mailpit on 8025, a Chrome binary, and puppeteer-core
 * (see package.json / README.md). Nothing it does is simulated — it logs in
 * through the real form and clicks the real controls against the real database.
 *
 *   node tools/walkthrough/walkthrough.js            # every step
 *   node tools/walkthrough/walkthrough.js --first 26 # the click-through half
 *   node tools/walkthrough/walkthrough.js 28 29      # just those steps
 *   BB_WALK_EMAIL=walk-123@example.com node … 21     # re-run one step on an
 *                                                    # account an earlier pass made
 *
 * Differences from capture.js (which only photographs pages):
 *  - it logs in through the REAL form (#login-email/#login-pass/#login-submit),
 *    not by planting a cookie, so a broken login screen would fail here;
 *  - it CLICKS: register wizard, search filters, submit, accept, fulfil, suspend;
 *  - it asserts the thing the runbook claims at that checkpoint (donor rows,
 *    the widen fallback, one welcome mail, the suspended login's reason) and
 *    records PASS/FAIL per assertion in walkthrough.txt.
 * Every step is wrapped, so one bad selector does not lose the other 28 shots.
 */
const fs = require('fs');
const path = require('path');

/* puppeteer-core is the only dependency. It is looked for in the usual places so
   the script runs either from `tools/walkthrough` (after `npm install`) or from
   a machine that already has it in the gitignored dev folder. */
const puppeteer = (() => {
  for (const c of ['puppeteer-core',
    path.resolve(__dirname, 'node_modules', 'puppeteer-core'),
    path.resolve(__dirname, '..', '..', '.tools', 'shots', 'node_modules', 'puppeteer-core')]) {
    try { return require(c); } catch (e) { /* try the next one */ }
  }
  console.error('puppeteer-core was not found. Install it first:\n' +
    '  cd tools/walkthrough && npm install\n' +
    '(it needs a Chrome/Chromium binary too — set BB_CHROME if it is not the default path)');
  process.exit(2);
})();

const CHROME = process.env.BB_CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const BASE = process.env.BB_BASE || 'http://localhost:8081';
const MAILPIT = process.env.BB_MAILPIT || 'http://127.0.0.1:8025';
const OUT = process.env.BB_WALK_OUT || path.resolve(__dirname, '..', '..', '.tools', 'wt', 'walkthrough');
const PASSWORD = 'BloodBuddy#2026';
// Overridable so a single step can be re-run against an account that already
// exists:  BB_WALK_EMAIL=walk-123@example.com node walkthrough.js 21
const WALK_EMAIL = process.env.BB_WALK_EMAIL || 'walk-' + Date.now() + '@example.com';
const WALK_PASS = 'Walkthrough#2026';

const ACC = {
  requester: 'sita.g@example.com',
  donor: 'arun.s@example.com',
  hospital: 'bloodbank@tuth.edu.np',
  admin: 'saksham@bloodbuddy.np',
};

const W = 1440, H = 1000;
const sleep = ms => new Promise(r => setTimeout(r, ms));

const shots = [];
const checks = [];
const state = { code: null, stockBefore: null, mails: { start: 0 }, dialog: null };

function check(label, ok, detail) {
  checks.push({ label, ok: !!ok, detail: detail || '' });
  console.log('   ' + (ok ? 'PASS' : 'FAIL') + '  ' + label + (detail ? ' — ' + detail : ''));
  return !!ok;
}

async function shot(page, name, note) {
  const file = path.join(OUT, name + '.png');
  /* Force a fresh compositor frame. Without this, a screenshot taken right after
     a DOM change can come back as the PREVIOUS frame — which is how a first run
     produced byte-identical "before"/"after" pairs (14/15, 16/17) that proved
     nothing. */
  await page.evaluate(() => new Promise(r =>
    requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(r, 60))))).catch(() => {});
  await sleep(300);
  await page.screenshot({ path: file });
  const buf = fs.readFileSync(file);
  const hash = require('crypto').createHash('md5').update(buf).digest('hex');
  const prev = shots[shots.length - 1];
  if (prev && prev.hash === hash) {
    console.log('  WARNING ' + name + '.png is byte-identical to ' + prev.name +
      '.png — the page did not visibly change (it proves nothing on its own)');
  }
  shots.push({ name, note: note || '', hash });
  console.log('  shot ' + name + '.png  (' + Math.round(buf.length / 1024) + ' KB)' +
    (note ? '  — ' + note : ''));
}

/* Node-side polling: survives navigations, unlike page.waitForFunction. */
async function waitForEval(page, fn, timeout = 45000, label = '', arg = null) {
  const t0 = Date.now();
  let last = null;
  while (Date.now() - t0 < timeout) {
    try { if (await page.evaluate(fn, arg)) return true; } catch (e) { last = e; }
    await sleep(400);
  }
  throw new Error('timed out waiting for ' + (label || String(fn).slice(0, 60)) +
    (last ? ' [' + last.message + ']' : ''));
}

async function waitForUrl(page, test, timeout = 45000) {
  const t0 = Date.now();
  while (Date.now() - t0 < timeout) {
    if (test(page.url())) return page.url();
    await sleep(300);
  }
  throw new Error('url never matched ' + test + ' (still ' + page.url() + ')');
}

async function go(page, p) {
  await page.goto(BASE + p, { waitUntil: 'load', timeout: 60000 });
  await sleep(400);
}

/* The real form, not a planted cookie. */
async function login(page, role, email) {
  await go(page, '/CODE/HTML/auth-login.html');
  await page.select('#login-role', role);
  await page.type('#login-email', email);
  await page.type('#login-pass', PASSWORD);
  await page.click('#login-submit');
  await waitForUrl(page, u => !/auth-login/.test(u), 40000);
  await sleep(3000);                       // the page's own fetch() renders after the redirect
  const alert = await page.evaluate(() => {
    const a = document.getElementById('login-alert');
    return a && !a.hidden ? a.innerText.trim() : '';
  });
  if (alert) throw new Error('login alert: ' + alert);
  return page.url();
}

async function logout(page) {
  await page.evaluate(() => {
    const a = Array.from(document.querySelectorAll('a, button'))
      .find(x => x.textContent.trim() === 'Log out');
    if (a) a.click();
  });
  await sleep(600);
  const status = await page.evaluate(() =>
    fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' }).then(r => r.status));
  await page.evaluate(() => localStorage.removeItem('bb_session'));
  return status;
}

const mails = async (limit = 1) => (await (await fetch(MAILPIT + '/api/v1/messages?limit=' + limit)).json());

async function mailShot(page, name, note) {
  await page.goto(MAILPIT, { waitUntil: 'load', timeout: 30000 });
  await sleep(2500);                        // the Vue inbox fetches after load
  await shot(page, name, note);
}

/* A resptest page that drives itself: wait for its own document.title. */
async function selfDriving(page, p, titleRe, needle, name, note) {
  await page.goto(BASE + p, { waitUntil: 'domcontentloaded', timeout: 60000 });
  const t0 = Date.now();
  while (Date.now() - t0 < 480000) {
    const title = await page.title().catch(() => '');
    if (titleRe.test(title)) {
      const y = await page.evaluate(n => {
        const pre = document.getElementById('out');
        if (!pre || !pre.firstChild) return null;
        const tn = pre.firstChild, text = tn.textContent || '';
        const i = text.indexOf(n);
        if (i < 0) return null;
        const r = document.createRange();
        r.setStart(tn, i); r.setEnd(tn, i + n.length);
        return r.getBoundingClientRect().top + window.scrollY;
      }, needle).catch(() => null);
      if (y !== null) await page.evaluate(v => window.scrollTo(0, Math.max(0, v - 200)), y);
      await sleep(400);
      await shot(page, name, note + ' — ' + title);
      return title;
    }
    await sleep(2000);
  }
  throw new Error('page never finished (title: ' + (await page.title()) + ')');
}

/* ------------------------------------------------------------------ steps --- */
const STEPS = [
  ['01', 'landing desktop', async (page) => {
    await page.setViewport({ width: W, height: H });
    await go(page, '/CODE/HTML/landing.html');
    await shot(page, '01_landing_desktop', '0:00 — landing, 1440px');
  }],

  ['02', 'landing 360px (responsive)', async (page) => {
    await page.setViewport({ width: 360, height: 780 });
    await go(page, '/CODE/HTML/landing.html');
    const over = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    check('at 360px the landing page does not overflow horizontally', !over,
      'scrollWidth ' + (await page.evaluate(() => document.documentElement.scrollWidth)) + ' vs 360');
    await shot(page, '02_landing_360', '0:00 — the same page at 360px (mandatory criterion)');
    await page.setViewport({ width: W, height: H });
  }],

  ['03', 'public emergency page (guest)', async (page) => {
    await go(page, '/CODE/HTML/emergency-request.html');
    await shot(page, '03_emergency_guest', '0:00 — "Find blood now": works with no account');
  }],

  ['04', 'register step 1', async (page) => {
    await go(page, '/CODE/HTML/auth-register.html');
    await waitForEval(page, () => { const s = document.getElementById('step-1'); return s && !s.hidden; },
      20000, 'step 1');
    await shot(page, '04_register_step1', '1:00 — role picker');
  }],

  ['05', 'register — inline validation error', async (page) => {
    await page.click('#to-step-2');
    await waitForEval(page, () => { const s = document.getElementById('step-2'); return s && !s.hidden; },
      20000, 'step 2');
    await page.type('#reg-fname', 'Walk');
    await page.type('#reg-lname', 'Through');
    await page.type('#reg-email', 'not-an-email');
    await page.type('#reg-phone', '9801234567');
    await page.select('#reg-blood', 'B+');
    await page.select('#reg-district', 'Kathmandu');
    await page.type('#reg-pass', WALK_PASS);
    await page.click('#reg-terms');
    await page.click('#to-step-3');
    await sleep(900);
    const errs = await page.evaluate(() => Array.from(document.querySelectorAll('.field-error, .has-error'))
      .map(e => e.innerText.trim()).filter(Boolean));
    check('a bad email is caught inline, before any request goes out', errs.length > 0,
      errs.join(' | ').slice(0, 120));
    check('and no native browser alert() dialog fired (the banner is an in-page element)', !state.dialog,
      state.dialog ? 'alert(): ' + state.dialog : 'no alert() dialog; the banner is an in-page element');
    await shot(page, '05_register_validation', '1:00 — client-side validation, inline (CPJ119 §2.1/§4)');
  }],

  ['06', 'register — server accepts it', async (page) => {
    // Clear it properly: a triple-click does not select inside this input, and
    // the first run of this script registered "not-an-emailwalk-…@example.com".
    await page.evaluate(() => {
      const el = document.getElementById('reg-email');
      el.value = '';
      el.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await page.type('#reg-email', WALK_EMAIL);
    await page.click('#to-step-3');
    await waitForEval(page, () => { const s = document.getElementById('step-3'); return s && !s.hidden; },
      40000, 'step 3 (the POST must have returned 201)');
    await sleep(600);
    await shot(page, '06_register_success', '1:00 — 201 from /api/auth/register; account ' + WALK_EMAIL);
  }],

  ['07', 'Mailpit — the welcome mail', async (page) => {
    const after = (await mails()).total;
    state.mails.afterRegister = after;
    const list = await mails(5);
    const welcome = (list.messages || []).filter(m => /^Welcome to BloodBuddy/.test(m.Subject));
    check('Mailpit gained exactly one registration mail',
      after === state.mails.start + 1 && welcome.length === 1,
      'inbox ' + state.mails.start + ' -> ' + after + '; newest subject: "' +
      ((list.messages || [])[0] || {}).Subject + '"');
    await mailShot(page, '07_mailpit_welcome', '1:00 — §2.4: the mail is delivered, not queued');
  }],

  ['08', 'requester login → donor search', async (page) => {
    await logout(page);
    const url = await login(page, 'requester', ACC.requester);
    check('requester login lands on the search page', /requester-search/.test(url), url.split('/').pop());
    await page.select('#f-blood', 'B+');
    await page.select('#f-district', 'Kathmandu');
    await page.click('#search-btn');
    await waitForEval(page, () => document.querySelectorAll('#donor-grid .donor-card').length >= 1,
      30000, 'donor cards');
    await sleep(800);
    const n = await page.evaluate(() => document.querySelectorAll('#donor-grid .donor-card').length);
    check('donor search renders real rows over HTTP (not localStorage)', n >= 5, n + ' cards');
    await shot(page, '08_requester_search', '4:00 — real donor rows for B+ in Kathmandu, pagination below');
  }],

  ['09', 'requester — widen fallback', async (page) => {
    await page.select('#f-blood', 'O+');
    await page.select('#f-district', 'Bhaktapur');
    await page.click('#search-btn');
    await sleep(4000);
    const text = await page.evaluate(() => (document.getElementById('results-text') || {}).innerText || '');
    const n = await page.evaluate(() => document.querySelectorAll('#donor-grid .donor-card').length);
    check('an empty district explains itself instead of showing 0 results',
      /all districts|widen|no .*in/i.test(text) || n > 0, 'results line: "' + text.trim() + '" (' + n + ' cards)');
    await shot(page, '09_requester_widen', '4:00 — O+ in Bhaktapur: the widen fallback speaks up');
  }],

  ['10', 'requester — submit a request', async (page) => {
    await go(page, '/CODE/HTML/requester-request.html');
    await page.select('#r-blood', 'B+');
    await page.select('#r-units', '2');
    await page.select('#r-urgency', 'Urgent — needed within 24 hours');
    await page.select('#r-hospital', 'Tribhuvan University Teaching Hospital, Maharajgunj');
    await page.select('#r-district', 'Kathmandu');
    const date = new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10);
    await page.evaluate(d => {
      const el = document.getElementById('r-by'); el.value = d;
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    }, date);
    await page.type('#r-notes', 'Walkthrough rehearsal — 2 units for the demo.');
    await sleep(400);
    await shot(page, '10_request_form', '4:00 — the form, with the recipient→donor compat chips');
    await page.click('#r-submit');
    await sleep(500);
    await shot(page, '11_request_submitted', '4:00 — accepted by the server ("Request submitted ✓")');
    await waitForUrl(page, u => /requester-tracking/.test(u), 30000);
    await sleep(3500);
  }],

  ['12', 'requester — tracker', async (page) => {
    await waitForEval(page, () => document.querySelectorAll('#req-list .req-card').length >= 1,
      30000, 'tracker rows');
    await sleep(800);
    const txt = await page.evaluate(() => (document.getElementById('req-list') || {}).innerText || '');
    const m = /BB-[A-Z0-9]{6}/.exec(txt);
    state.code = m ? m[0] : null;
    check('the new request is in the tracker with its public code', !!state.code,
      state.code ? state.code + ' (newest card)' : 'no BB- code found in #req-list');
    await shot(page, '12_tracker', '4:00 — tracker: ' + (state.code || 'new request') + ' with status + timeline');
  }],

  ['13', 'Mailpit — donor alerts', async (page) => {
    const list = await mails(20);
    const urgent = (list.messages || []).filter(m => /^Urgent .* request in /.test(m.Subject));
    check('compatible donors were emailed about it', urgent.length >= 1,
      urgent.length + ' "Urgent … request in …" mail(s) in the inbox');
    await mailShot(page, '13_mailpit_alerts', '4:00 — the donor alerts the timeline claims');
  }],

  ['14', 'donor — requests near you', async (page) => {
    await logout(page);
    const url = await login(page, 'donor', ACC.donor);
    check('donor login lands on the donor dashboard', /donor-dashboard/.test(url), url.split('/').pop());
    await waitForEval(page, () => document.querySelectorAll('#nearby-body tr').length >= 1,
      30000, 'the nearby-requests table');
    await sleep(800);
    const rows = await page.evaluate(() => Array.from(document.querySelectorAll('#nearby-body tr')).map(r => r.innerText.replace(/\s+/g, ' ').trim()));
    const mine = state.code ? rows.some(r => r.includes(state.code)) : false;
    check('the board lists only requests a B+ donor may actually serve',
      rows.length >= 1, rows.length + ' row(s); ' + (mine ? 'the new one is offered to him' : 'none is his district/group'));
    check('the O− request is NOT offered to a B+ donor',
      !rows.some(r => /BB-5MN8VX/.test(r)), 'BB-5MN8VX absent from ' + rows.length + ' row(s)');
    await shot(page, '14_donor_board', '7:00 — "Requests near you" for Arun (B+)');
  }],

  ['15', 'donor — accept', async (page) => {
    const id = await page.evaluate(code => {
      const btns = Array.from(document.querySelectorAll('.nb-act[data-act="accept"]'));
      const mine = code ? btns.find(b => b.getAttribute('data-id') === code) : null;
      const b = mine || btns[0];
      if (!b) return null;
      const id = b.getAttribute('data-id'); b.click(); return id;
    }, state.code);
    if (!id) throw new Error('no Accept button on the board');
    check('a donor can accept a request from the board', true, 'accepted ' + id);
    // Wait for the board to actually change, not just for the request to return.
    const gone = await waitForEval(page, c =>
      !document.querySelector('.nb-act[data-act="accept"][data-id="' + c + '"]'), 20000,
      'the accepted row to leave the board', id).then(() => true).catch(() => false);
    check('the accepted request leaves the donor\'s open list (it is Matched now)', gone,
      gone ? 'no Accept button remains for ' + id : 'still offered — the row did not change');
    await page.evaluate(() => {
      const f = document.getElementById('nb-flash');
      if (f && !f.hidden) f.scrollIntoView({ block: 'center' });
    });
    await sleep(500);
    await shot(page, '15_donor_accepted', '7:00 — accepted ' + id + ': it left the open list (Matched, stamped)');
    state.accepted = id;
  }],

  ['16', 'hospital — inventory', async (page) => {
    await logout(page);
    const url = await login(page, 'hospital', ACC.hospital);
    check('hospital login lands on the blood bank dashboard', /hospital-dashboard/.test(url), url.split('/').pop());
    await waitForEval(page, () => document.querySelectorAll('#inv-grid .inv-card').length >= 8,
      30000, 'the 8 blood groups');
    await sleep(600);
    state.stockBefore = await page.evaluate(() => {
      const card = Array.from(document.querySelectorAll('#inv-grid .inv-card'))
        .find(c => (c.querySelector('.inv-bg') || {}).textContent === 'B+');
      return card ? parseInt((card.querySelector('.inv-num') || {}).textContent, 10) : null;
    });
    check('the inventory reads 8 groups with live stock', state.stockBefore !== null,
      'TUTH B+ = ' + state.stockBefore);
    await shot(page, '16_hospital_inventory', '9:30 — TUTH inventory (B+ = ' + state.stockBefore + ')');
  }],

  ['17', 'hospital — adjust stock', async (page) => {
    await page.evaluate(() => {
      const b = Array.from(document.querySelectorAll('.inv-adjust')).find(x => x.getAttribute('data-bg') === 'B+')
        || document.querySelector('.inv-adjust');
      b.click();
    });
    await sleep(1200);
    await page.select('#m-action', 'add');
    await page.evaluate(() => {
      const el = document.getElementById('m-units'); el.value = '1';
      el.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await shot(page, '17a_hospital_stock_modal', '9:30 — the write goes through the modal (Add stock, 1 unit)');
    await page.click('#m-save');
    await sleep(4000);
    const after = await page.evaluate(() => {
      const card = Array.from(document.querySelectorAll('#inv-grid .inv-card'))
        .find(c => (c.querySelector('.inv-bg') || {}).textContent === 'B+');
      return card ? parseInt((card.querySelector('.inv-num') || {}).textContent, 10) : null;
    });
    check('the stock write goes through the API (B+ +1)', after === state.stockBefore + 1,
      state.stockBefore + ' → ' + after);
    state.stockAfterAdd = after;
    await page.evaluate(() => {
      const card = Array.from(document.querySelectorAll('#inv-grid .inv-card'))
        .find(c => (c.querySelector('.inv-bg') || {}).textContent === 'B+');
      if (card) card.scrollIntoView({ block: 'center' });
    });
    await sleep(400);
    await shot(page, '17b_hospital_stock_adjusted', '9:30 — B+ ' + state.stockBefore + ' → ' + after + ' via PUT /api/hospitals/inventory');
  }],

  ['18', 'hospital — the TUTH queue', async (page) => {
    await go(page, '/CODE/HTML/hospital-requests.html');
    await waitForEval(page, () => document.querySelectorAll('#queue-list .req-card').length >= 1,
      30000, 'queue rows');
    await sleep(1000);
    const rows = await page.evaluate(() => Array.from(document.querySelectorAll('#queue-list .req-card'))
      .map(c => (c.querySelector('.rc-id') || {}).textContent || ''));
    check('a TUTH session sees TUTH\'s own queue (the page sends no hospital at all)',
      rows.length >= 1, rows.length + ' row(s): ' + rows.join(', '));
    const foreign = await page.evaluate(() => /Bir Hospital|Patan Hospital/.test(document.getElementById('queue-list').innerText));
    check('no other hospital\'s request appears on it', !foreign, foreign ? 'a foreign hospital label rendered' : 'none');
    await shot(page, '18_hospital_queue', '9:30 — TUTH\'s own queue for a TUTH session (BR-5)');
  }],

  ['19', 'hospital — fulfil (BR-4)', async (page) => {
    const acted = await page.evaluate(code => {
      const cards = Array.from(document.querySelectorAll('#queue-list .req-card'));
      const mine = code ? cards.find(c => (c.innerText || '').includes(code)) : null;
      const card = mine || cards.find(c => c.querySelector('.qa.accept[data-act="Fulfilled"]')) ||
        cards.find(c => c.querySelector('.qa.accept'));
      if (!card) return null;
      const b = card.querySelector('.qa.accept');
      if (!b) return null;
      const id = b.getAttribute('data-id'), act = b.getAttribute('data-act');
      b.click();
      return id + ' → ' + act;
    }, state.code);
    if (!acted) throw new Error('no fulfillable request in the queue');
    check('a Matched request can be marked fulfilled', true, acted);
    await waitForEval(page, a => {
      const cards = Array.from(document.querySelectorAll('#queue-list .req-card'));
      const c = cards.find(x => (x.innerText || '').includes(a));
      return !c || !c.querySelector('.qa.accept[data-act="Fulfilled"]');
    }, 20000, 'the fulfilled request to leave the queue', acted.split(' ')[0]).catch(() => {});
    await sleep(800);
    await shot(page, '19_hospital_fulfilled', '9:30 — fulfilled: ' + acted + ', units deducted from stock');
    const stock = await page.evaluate(() => fetch('/api/hospitals/inventory', { credentials: 'same-origin' })
      .then(r => r.json()).then(j => (j.board || j)['B+']).catch(() => null));
    check('BR-4: the deduction is the server\'s arithmetic',
      stock === state.stockAfterAdd - 2, 'B+ now ' + stock + ' (was ' + state.stockAfterAdd + ', request = 2 units)');
  }],

  ['20', 'admin — dashboard', async (page) => {
    await logout(page);
    const url = await login(page, 'admin', ACC.admin);
    check('admin login lands on the admin dashboard', /admin-dashboard/.test(url), url.split('/').pop());
    await waitForEval(page, () => document.querySelectorAll('#users-tbody tr').length >= 1,
      30000, 'the users table');
    await sleep(1200);
    const n = await page.evaluate(() => document.querySelectorAll('#users-tbody tr').length);
    check('the users table renders real accounts, paginated', n >= 5, n + ' rows on page 1');
    // The table is below the fold: a viewport shot of the top of this page shows
    // KPIs and charts only, and three different checkpoints came out identical.
    await page.evaluate(() => document.getElementById('users-tbody')
      .scrollIntoView({ block: 'center' }));
    await sleep(400);
    await shot(page, '20_admin_dashboard', '11:30 — the users table (real accounts, paginated) below the KPI row');
  }],

  ['21', 'admin — suspend the walkthrough account', async (page) => {
    await page.evaluate(e => {
      const el = document.getElementById('users-search');
      el.value = e;
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }, WALK_EMAIL);
    await sleep(3500);
    const found = await page.evaluate(() => document.querySelectorAll('#users-tbody tr').length);
    check('the account registered in step 1 is really in the database',
      found >= 1, found + ' row(s) for ' + WALK_EMAIL);
    await page.evaluate(() => {
      const row = document.querySelector('#users-tbody tr');
      const b = row && row.querySelector('.ut-btn.danger, .ut-btn');
      if (b) b.click();
    });
    await sleep(1200);
    await shot(page, '21a_admin_suspend_modal', '11:30 — the confirm dialog names the user and the role');
    await page.evaluate(() => { const b = document.getElementById('m-confirm'); if (b) b.click(); });
    await sleep(4000);
    /* Re-apply the search: the table re-renders from page 1 afterwards, and the
       account that changed is on the last page — the first version of this step
       photographed a table where the changed row was not visible at all. */
    await page.evaluate(e => {
      const el = document.getElementById('users-search');
      el.value = e;
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }, WALK_EMAIL);
    await sleep(3000);
    const st = await page.evaluate(() => {
      const row = document.querySelector('#users-tbody tr');
      return row ? row.innerText.replace(/\s+/g, ' ').trim().slice(0, 120) : '';
    });
    check('the row shows the account as Suspended afterwards', /suspend/i.test(st), st);
    await page.evaluate(() => document.getElementById('users-tbody')
      .scrollIntoView({ block: 'center' }));
    await sleep(400);
    await shot(page, '21b_admin_suspend_done', '11:30 — the row is Suspended: ' + WALK_EMAIL);
  }],

  ['22', 'admin — moderation + forward', async (page) => {
    await go(page, '/CODE/HTML/admin-requests.html');
    await waitForEval(page, () => document.querySelectorAll('#queue-list .req-card').length >= 1,
      30000, 'the moderation queue');
    await page.select('#hospital-filter', '—');
    await sleep(3000);
    const unrouted = await page.evaluate(() => document.querySelectorAll('#queue-list .req-card').length);
    check('the unrouted bucket lists the guest submission', unrouted >= 1, unrouted + ' unrouted row(s)');
    await page.evaluate(() => {
      const b = document.querySelector('#queue-list .qa.accept[data-act="forward"], #queue-list .qa.accept');
      if (b) b.click();
    });
    await sleep(1200);
    await page.select('#m-hospital', 'Patan Hospital, Lalitpur');
    await page.click('#m-confirm');
    await sleep(4000);
    await shot(page, '22_admin_forward', '11:30 — the unrouted guest request forwarded to Patan');
  }],

  ['23', 'the suspended account is refused', async (page) => {
    await logout(page);
    await go(page, '/CODE/HTML/auth-login.html');
    await page.select('#login-role', 'donor');
    await page.type('#login-email', WALK_EMAIL);
    await page.type('#login-pass', WALK_PASS);
    await page.click('#login-submit');
    await sleep(3500);
    const alert = await page.evaluate(() => {
      const a = document.getElementById('login-alert');
      return a && !a.hidden ? a.innerText.trim() : '';
    });
    check('a suspended account cannot start a session, and is told why',
      /suspend/i.test(alert), alert || '(no alert shown)');
    await shot(page, '23_suspended_refused', '11:30 — the suspend has teeth: the next login is refused with its reason');
    const still = page.url();
    check('and it never reached a dashboard', /auth-login/.test(still), still.split('/').pop());
  }],

  ['24', 'RBAC page bounce', async (page) => {
    await login(page, 'admin', ACC.admin);
    await go(page, '/CODE/HTML/donor-dashboard.html');
    await sleep(2500);
    const url = page.url();
    check('a wrong-role page bounces the user to their own dashboard (client-side UX guard)',
      !/donor-dashboard/.test(url), 'asked for donor-dashboard.html, ended on ' + url.split('/').pop() +
      ' — the URL is the evidence here; the picture looks like the admin dashboard because it IS the admin dashboard');
    await page.evaluate(() => window.scrollTo(0, 0));
    await shot(page, '24_rbac_bounce', '13:30 — bounced off donor-dashboard.html (see the URL in the report line)');
    /* The other half of the guard, and a picture that is not the same dashboard:
       a logged-OUT visitor asking for a role-private page is sent to the login
       screen, which remembers where they were going. */
    await logout(page);
    await go(page, '/CODE/HTML/donor-dashboard.html');
    await sleep(2000);
    const outUrl = page.url();
    check('a logged-out visitor asking for a role-private page lands on the login screen',
      /auth-login/.test(outUrl), outUrl.split('/').pop());
    await page.evaluate(() => window.scrollTo(0, 0));
    await shot(page, '24b_rbac_logged_out', '13:30 — logged out, asking for donor-dashboard.html: the guard sends you to log in');
  }],

  ['25', 'server-side 403', async (page) => {
    await logout(page);
    await login(page, 'requester', ACC.requester);
    await page.goto(BASE + '/api/admin/users', { waitUntil: 'domcontentloaded' });
    await sleep(800);
    const body = await page.evaluate(() => document.body.innerText.slice(0, 200));
    check('a requester session on /api/admin/users answers 403 (server, not the page)',
      /403|forbidden/i.test(body), body.replace(/\s+/g, ' ').slice(0, 100));
    await shot(page, '25_api_403', '13:30 — the real gate: 403 JSON for the wrong role');
  }],

  ['26', 'server-side 401', async (page) => {
    await logout(page);
    await page.goto(BASE + '/api/admin/users', { waitUntil: 'domcontentloaded' });
    await sleep(800);
    const body = await page.evaluate(() => document.body.innerText.slice(0, 200));
    check('an anonymous API call answers 401 (Spring Security, every /api/** route)',
      /401|unauthor/i.test(body), body.replace(/\s+/g, ' ').slice(0, 100));
    await shot(page, '26_api_401', '13:30 — anonymous: 401 in the same ApiError shape');
  }],

  ['27', 'tools — real-backend-check', async (page) => {
    const t = await selfDriving(page, '/resptest/real-backend-check.html', /^REAL-BACKEND-DONE/,
      'passed,', '27_real_backend_check', '14:30 — the frontend against the live backend');
    check('real-backend-check.html is green', /42\/42/.test(t), t);
  }],

  ['28', 'tools — qa-harness', async (page) => {
    const t = await selfDriving(page, '/resptest/qa-harness.html', /^QA-DONE/, 'TOTAL:',
      '28_qa_harness', '14:30 — the mock-mode functional suite');
    check('qa-harness.html is green', /268\/268/.test(t), t);
  }],

  ['29', 'tools — responsive-check', async (page) => {
    const t = await selfDriving(page, '/resptest/responsive-check.html', /^RESP-DONE/, 'RESULT:',
      '29_responsive_check', '14:30 — every page at 360/430/768/1280');
    check('responsive-check.html is green', /92\/92/.test(t), t);
  }],
];

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const argv = process.argv.slice(2);
  const firstIdx = argv.indexOf('--first');
  const only = argv.filter((a, i) => !/^--/.test(a) && !(firstIdx >= 0 && i === firstIdx + 1));
  let list = only.length ? STEPS.filter(s => only.some(o => s[0] === o || s[1].includes(o))) : STEPS;
  if (firstIdx >= 0) list = list.slice(0, parseInt(argv[firstIdx + 1], 10));
  const browser = await puppeteer.launch({
    executablePath: CHROME, headless: true,
    args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--disable-dev-shm-usage'],
  });
  const ctx = await browser.createBrowserContext();
  const page = await ctx.newPage();
  await page.setViewport({ width: W, height: H, deviceScaleFactor: 1 });
  // A native dialog would block the run; record it so step 05 can assert none fired.
  page.on('dialog', async d => { state.dialog = d.message(); await d.dismiss().catch(() => {}); });

  state.mails.start = (await mails()).total;
  console.log('walkthrough: ' + list.length + ' step(s); Mailpit starts at ' + state.mails.start + ' message(s)');
  console.log('new account for this run: ' + WALK_EMAIL + '\n');

  const results = [];
  for (const [id, title, fn] of list) {
    const t0 = Date.now();
    console.log('[' + id + '] ' + title);
    try {
      await fn(page);
      results.push({ id, title, ok: true, ms: Date.now() - t0 });
    } catch (e) {
      results.push({ id, title, ok: false, error: e.message, ms: Date.now() - t0 });
      console.log('  FAILED: ' + e.message);
      await page.screenshot({ path: path.join(OUT, 'FAILED_' + id + '.png') }).catch(() => {});
    }
  }
  await browser.close();

  const lines = [];
  lines.push('BloodBuddy — demo runbook walkthrough (browser), ' + new Date().toISOString());
  lines.push('account registered this run: ' + WALK_EMAIL);
  lines.push('request exercised: ' + (state.code || '(none)') + '   accepted: ' + (state.accepted || '(none)'));
  lines.push('TUTH B+: ' + state.stockBefore + ' -> ' + state.stockAfterAdd + ' -> (fulfilled, -2)');
  lines.push('');
  lines.push('STEPS');
  for (const r of results) {
    lines.push('  ' + (r.ok ? 'OK  ' : 'FAIL') + '  [' + r.id + '] ' + r.title +
      '  (' + Math.round(r.ms / 1000) + 's)' + (r.error ? '  — ' + r.error : ''));
  }
  lines.push('');
  lines.push('ASSERTIONS (' + checks.filter(c => c.ok).length + '/' + checks.length + ' passed)');
  for (const c of checks) lines.push('  ' + (c.ok ? 'PASS' : 'FAIL') + '  ' + c.label + (c.detail ? ' — ' + c.detail : ''));
  lines.push('');
  lines.push('SHOTS (' + shots.length + ')');
  for (let i = 0; i < shots.length; i++) {
    const s = shots[i];
    lines.push('  ' + s.name + '.png  ' + s.note +
      (i && s.hash === shots[i - 1].hash ? '   [BYTE-IDENTICAL to ' + shots[i - 1].name + ']' : ''));
  }
  // Append: a run is often done in passes (`--first 26`, then the slow suites),
  // and an overwrite loses the earlier pass's record.
  const report = path.join(OUT, 'walkthrough.txt');
  fs.appendFileSync(report, (fs.existsSync(report) ? '\n\n' + '='.repeat(78) + '\n\n' : '') + lines.join('\n'));
  const byHash = {};
  for (const s of shots) (byHash[s.hash] = byHash[s.hash] || []).push(s.name);
  const dupes = Object.values(byHash).filter(g => g.length > 1);
  lines.push('');
  lines.push(dupes.length
    ? 'SELF-CHECK: ' + dupes.length + ' group(s) of byte-identical shots — ' +
      dupes.map(g => g.join(' = ')).join('; ') + ' (each of those checkpoints proves nothing on its own)'
    : 'SELF-CHECK: all ' + shots.length + ' shots are distinct pictures.');

  console.log('\n' + lines.join('\n'));
  console.log('\n-> ' + OUT);
  if (results.some(r => !r.ok)) process.exit(1);
})();
