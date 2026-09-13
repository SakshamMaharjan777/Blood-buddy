/* =============================================
   BLOODBUDY — nav.js
   Role-aware navigation for the demo (no backend).
   Loaded by every page BEFORE main.js.

   Session model (localStorage key: bb_session):
     { role: 'donor'|'requester'|'hospital'|'admin', name?: string }
   Absent key = logged out.

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
    dashboardFor: function (role) { return DASHBOARDS[role] || 'landing.html'; }
  };

  function roleLabel(role) {
    return { donor: 'Donor', requester: 'Requester', hospital: 'Hospital', admin: 'Admin' }[role] || '';
  }
  function initials(name) {
    var parts = String(name || '').trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return 'U';
    return parts.slice(0, 2).map(function (w) { return w[0].toUpperCase(); }).join('');
  }

  document.addEventListener('DOMContentLoaded', function () {
    var session = getSession();
    var home = document.querySelector('.nav-links a[href$="landing.html"], .nav-links a[href^="landing.html#"]');

    /* --- 1. HOME → role dashboard when logged in --- */
    if (session && session.role && DASHBOARDS[session.role] && home) {
      home.textContent = 'Dashboard';
      home.setAttribute('href', DASHBOARDS[session.role]);
    }

    /* --- 2. Nav auth buttons → user chip + Log out when logged in --- */
    var actions = document.getElementById('nav-actions');
    if (session && session.role && actions) {
      var name = session.name || roleLabel(session.role);
      var chip = document.createElement('span');
      chip.className = 'nav-user-chip';
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

    /* --- 3. Esc key: back to your dashboard (only while logged in) ---
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
