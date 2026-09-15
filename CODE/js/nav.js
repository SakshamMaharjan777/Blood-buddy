/* =============================================
   BLOODBUDY — nav.js
   Role-aware navigation for the demo (no backend).
   Loaded by every page BEFORE main.js.

   Session model (localStorage key: bb_session):
     { role: 'donor'|'requester'|'hospital'|'admin', name?: string }
   Absent key = logged out.

   RBAC GUARD: role-private pages (role portals, moderation, and each
   role's profile page) redirect to the login page when opened logged-out,
   and to the visitor's own dashboard when opened under the wrong role —
   typing a portal URL directly must not bypass the role split. The
   requester pages are member-only too (guests use the public emergency
   flow on emergency-request.html instead).
   (Demo-level control: enforced in this shared script, not the server;
   the real gate is Spring Security per role in CPP401.)

   Logged in, the .nav-links of EVERY page is rebuilt from the role's
   link set (portal pages + whatever About/Contact links the page
   already had), so your portal links follow you onto public pages
   like about.html instead of swapping back to marketing links.

   Demo credentials: any email + password (6+ chars).
   The login page lets you pick a role; register saves
   the role you registered as.
   ============================================= */

(function () {
  'use strict';

  var DASHBOARDS = {
    donor:     'donor-dashboard.html',
    requester: 'requester-search.html',
    hospital:  'hospital-dashboard.html',
    admin:     'admin-dashboard.html'
  };

  /* Role-private pages → allowed roles (see RBAC GUARD in the header).
     Guests needing blood use the public emergency-request.html flow.
     Every role's profile page is role-private too (donor-profile.html
     used to be guarded separately; it now lives in this map as well). */
  var RBAC_GUARD = {
    'donor-dashboard.html':     ['donor'],
    'donor-history.html':       ['donor'],
    'donor-profile.html':       ['donor'],
    'hospital-dashboard.html':  ['hospital'],
    'hospital-requests.html':   ['hospital'],
    'hospital-profile.html':    ['hospital'],
    'admin-dashboard.html':     ['admin'],
    'admin-requests.html':      ['admin'],
    'admin-profile.html':       ['admin'],
    'requester-search.html':    ['requester'],
    'requester-request.html':   ['requester'],
    'requester-tracking.html':  ['requester'],
    'requester-profile.html':   ['requester']
  };
  var RETURN_KEY = 'bb_return_to'; /* sessionStorage — where a bounced visitor came from */

  function getSession() {
    try { return JSON.parse(localStorage.getItem('bb_session') || 'null'); }
    catch (e) { return null; }
  }
  function setSession(session) {
    if (session) localStorage.setItem('bb_session', JSON.stringify(session));
    else localStorage.removeItem('bb_session');
  }

  /* Expose a tiny API so login/register pages can set the session */
  window.BloodBuddyNav = {
    getSession: getSession,
    setSession: setSession,
    dashboardFor: function (role) { return DASHBOARDS[role] || 'landing.html'; },
    canAccess: function (role, pageFile) {
      var file = String(pageFile || '').toLowerCase();
      var allowed = RBAC_GUARD[file];
      if (!allowed) return true; /* public page */
      return !!role && allowed.indexOf(role) > -1;
    }
  };

  /* --- RBAC GUARD for role-private pages (see file header) ---
     Must run before the DOMContentLoaded work below so a forbidden
     visitor is bounced before nav links render. */
  (function () {
    var here = (window.location.pathname.split('/').pop() || '').toLowerCase();
    var allowed = RBAC_GUARD[here];
    if (!allowed) return; /* public page */
    var session = getSession();
    if (session && session.role && allowed.indexOf(session.role) > -1) return; /* correct role */
    try {
      sessionStorage.setItem(RETURN_KEY, window.location.href); /* login returns here */
    } catch (e) {}
    window.location.replace(
      session && session.role
        ? (DASHBOARDS[session.role] || 'landing.html') /* wrong role → your dashboard */
        : 'auth-login.html'                            /* logged out → login */
    );
  })();

  function roleLabel(role) {
    return { donor: 'Donor', requester: 'Requester', hospital: 'Hospital', admin: 'Admin' }[role] || '';
  }
  function initials(name) {
    var parts = String(name || '').trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return 'U';
    return parts.slice(0, 2).map(function (w) { return w[0].toUpperCase(); }).join('');
  }

  /* Per-role profile page — every role now has a dedicated profile
     editor (donor-profile.html is the original template). */
  var PROFILE = {
    donor:     'donor-profile.html',
    requester: 'requester-profile.html',
    hospital:  'hospital-profile.html',
    admin:     'admin-profile.html'
  };

  /* Where in-page marketing CTAs ("Register as donor", …) point once
     the user is logged in — a donor should never see a register button. */
  var ROLE_CTA = {
    donor:     { label: 'Go to Donor Portal',     href: 'donor-dashboard.html' },
    requester: { label: 'Find compatible blood',  href: 'requester-search.html' },
    hospital:  { label: 'Go to Hospital Portal',  href: 'hospital-dashboard.html' },
    admin:     { label: 'Go to Admin Portal',     href: 'admin-dashboard.html' }
  };

  /* Per-role nav links (labels/targets mirror the static navbars of
     each role's own pages). The first link is that role's dashboard. */
  var ROLE_LINKS = {
    donor:     [['Donor Portal', 'donor-dashboard.html'], ['My Profile', 'donor-profile.html']],
    requester: [['Find Donors', 'requester-search.html'], ['My Requests', 'requester-tracking.html'], ['My Profile', 'requester-profile.html']],
    hospital:  [['Hospital Portal', 'hospital-dashboard.html'], ['Requests', 'hospital-requests.html'], ['My Profile', 'hospital-profile.html']],
    admin:     [['Admin Portal', 'admin-dashboard.html'], ['Moderation', 'admin-requests.html'], ['My Profile', 'admin-profile.html']]
  };

  /* (donor-profile.html's separate donor-only guard was folded into
     RBAC_GUARD above — same rule, one table for every private page.) */

  /* --- Logged-out hardening: portal links can't sit in plain HTML where
     anyone (or an editor) can point them straight at a portal. Rebind
     every PUBLIC page's href to a private page (portal cards, footers,
     CTA fallbacks) to the login page, keeping the original target in
     data-bb-return so post-login we can carry on to it. Guest blood
     CTAs ("Find blood now", "Request blood" → emergency-request.html)
     are LEFT alone — that flow is public on purpose; the DOMContentLoaded
     swap below sends members to their own pages instead. Runs before the
     DOMContentLoaded block so the swap beats any early click. --- */
  (function () {
    if (getSession()) return; /* logged in → nav links already role-gated */
    var PRIVATE_RE = /^(donor|hospital|admin|requester)-[a-z-]+\.html$/;
    Array.prototype.slice.call(document.querySelectorAll('a[href]')).forEach(function (a) {
      var href = (a.getAttribute('href') || '').trim();
      var file = href.split('?')[0].split('#')[0].split('/').pop().toLowerCase();
      if (!PRIVATE_RE.test(file)) return;
      a.setAttribute('data-bb-return', href);
      a.setAttribute('href', 'auth-login.html');
    });
  })();

  document.addEventListener('DOMContentLoaded', function () {
    var session = getSession();
    var links = document.querySelector('.nav-links');

    /* --- 1. Role-aware nav links when logged in ---
       Pages ship a static nav for logged-out visitors (Home + marketing
       links). With a session active we rebuild .nav-links everywhere so
       the user's portal links persist on public pages (about, contact,
       terms, privacy, landing) too — no more "nav throws me out of my
       portal". Links to about.html/contact.html the page already had are
       kept (preserving their .active state); everything else is replaced. */
    if (session && session.role && ROLE_LINKS[session.role] && links) {
      var current = (window.location.pathname.split('/').pop() || 'landing.html').toLowerCase();
      var kept = [];
      Array.prototype.slice.call(links.querySelectorAll('a')).forEach(function (a) {
        var h = (a.getAttribute('href') || '').toLowerCase();
        if (h.indexOf('about.html') === 0 || h.indexOf('contact.html') === 0) {
          kept.push(a);
        } else {
          a.parentNode.removeChild(a);
        }
      });
      ROLE_LINKS[session.role].forEach(function (def) {
        var a = document.createElement('a');
        a.textContent = def[0];
        a.setAttribute('href', def[1]);
        if (current === def[1]) a.className = 'active';
        links.insertBefore(a, kept.length ? kept[0] : null);
      });
    }

    /* --- 2. In-page CTAs → role actions when logged in ---
       Pages keep marketing CTAs ("Register as donor", "Request blood" →
       register) for logged-out visitors. With a session active:
       - swap every auth-register CTA OUTSIDE the navbar (the navbar
         Register button is already replaced by the user chip below)
         for the role's destination;
       - drop "Request blood" CTAs for every role except requester —
         submitting a request is a requester action, so showing it to a
         logged-in donor/hospital/admin makes no sense. Requesters keep
         it (it is their form); logged-out visitors keep it too. */
    if (session && session.role && ROLE_CTA[session.role]) {
      var cta = ROLE_CTA[session.role];
      var inNavbar = function (el) {
        var p = el.parentNode;
        while (p && p !== document.body) {
          if (p.id === 'nav-actions') return true;
          p = p.parentNode;
        }
        return false;
      };
      Array.prototype.slice.call(document.querySelectorAll('a[href^="auth-register"]')).forEach(function (a) {
        if (inNavbar(a)) return;
        a.setAttribute('href', cta.href);
        a.textContent = cta.label;
      });
      if (session.role !== 'requester') {
        Array.prototype.slice.call(document.querySelectorAll('a[href^="requester-request"]')).forEach(function (a) {
          if (inNavbar(a)) return;
          a.parentNode.removeChild(a);
        });
      }
      /* Guest blood CTAs (emergency-request.html) become member actions
         once logged in. "Find" CTAs (data-em="find", search intent) go to
         the requester's donor search / everyone else's portal; plain
         "Request blood" CTAs go to the requester's form and are removed
         for other roles (it stays a requester-only action). */
      Array.prototype.slice.call(document.querySelectorAll('a[href^="emergency-request"]')).forEach(function (a) {
        if (inNavbar(a)) return;
        if (session.role === 'requester') {
          a.setAttribute('href', a.getAttribute('data-em') === 'find' ? 'requester-search.html' : 'requester-request.html');
        } else if (a.getAttribute('data-em') === 'find') {
          a.setAttribute('href', cta.href);
        } else {
          a.parentNode.removeChild(a);
        }
      });

      /* Emergency page's login/register nudge is pointless for members */
      var nudge = document.getElementById('em-nudge');
      if (nudge) nudge.parentNode.removeChild(nudge);
    }

    /* --- 3. Nav auth buttons → user chip + Log out when logged in --- */
    var actions = document.getElementById('nav-actions');
    if (session && session.role && actions) {
      var name = session.name || roleLabel(session.role);
      var chip = document.createElement('a');
      chip.className = 'nav-user-chip';
      chip.setAttribute('href', PROFILE[session.role] || DASHBOARDS[session.role] || 'landing.html');
      chip.setAttribute('title', 'My profile');
      chip.setAttribute('aria-label', 'Open my profile');
      var avatar = document.createElement('span');
      avatar.className = 'nav-user-avatar';
      avatar.textContent = initials(name);
      var label = document.createElement('span');
      label.className = 'nav-user-label';
      label.textContent = name;
      chip.appendChild(avatar);
      chip.appendChild(label);

      var logout = document.createElement('a');
      logout.className = 'btn-outline-nav';
      logout.setAttribute('href', '#');
      logout.textContent = 'Log out';

      actions.innerHTML = '';
      actions.appendChild(chip);
      actions.appendChild(logout);

      logout.addEventListener('click', function (e) {
        e.preventDefault();
        setSession(null);
        window.location.href = 'landing.html';
      });
    }

    /* --- 4. Esc key: back to your dashboard (only while logged in) ---
       Skipped when a modal is open — pages use Esc to close modals. --- */
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && !e.defaultPrevented && getSession() &&
          !document.querySelector('.modal-overlay.open') &&
          !/^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName) &&
          !document.activeElement.isContentEditable) {
        window.location.href = DASHBOARDS[getSession().role] || 'landing.html';
        e.preventDefault();
      }
    });
  });
})();
