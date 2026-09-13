/* =============================================
   BLOODBUDY — bb-store.js
   Shared demo "database" in localStorage.
   Loaded by every page that reads/writes demo
   data, BEFORE the page's inline script.

   All 4 roles read/write the SAME store, so the
   whole request lifecycle persists across pages:

     requester-request → creates request (Pending)
     admin-requests    → forward → Accepted (+hospital)
                         flag    → Rejected
     hospital-requests → accept pending (already
                         forwarded) → fulfilled
     requester-tracking→ shows live status

   Backend swap (CPP401): replace the load/save
   internals of BloodBuddyStore with fetch() calls
   to the Appendix B endpoints — pages don't change.

   Storage keys (one JSON document per collection):
     bb_db.requests    — all blood requests
     bb_db.inventory   — hospital stock levels
     bb_db.users       — platform users (admin)
   ============================================= */

(function () {
  'use strict';

  var PREFIX = 'bb_db.';

  function readKey(key, fallback) {
    try {
      var raw = localStorage.getItem(PREFIX + key);
      return raw === null ? fallback : JSON.parse(raw);
    } catch (e) {
      return fallback;
    }
  }

  function writeKey(key, value) {
    try {
      localStorage.setItem(PREFIX + key, JSON.stringify(value));
    } catch (e) { /* storage full/blocked — demo keeps working in-memory */ }
  }

  /* ---------------- SEEDS (first visit only) ---------------- */

  var SEED_REQUESTS = [
    {
      id: 'BB-8F3K2M', blood: 'O+', units: '2', urgency: 'Urgent',
      hospital: 'Bir Hospital, Kathmandu', district: 'Kathmandu',
      neededBy: '2026-09-15', compatOk: true, status: 'Matched',
      requester: 'Sita Gurung',
      created: '2026-09-12T09:24:00',
      donor: 'Arun Shrestha (B+ → O+ compatible)', donorAt: '2026-09-12T11:02:00',
      timeline: [
        { when: '2026-09-12 09:24', what: 'Request submitted — 14 compatible donors notified' },
        { when: '2026-09-12 11:02', what: 'Arun Shrestha accepted the request' }
      ]
    },
    {
      id: 'BB-2QX9P1', blood: 'B+', units: '1', urgency: 'Normal',
      hospital: 'Patan Hospital, Lalitpur', district: 'Lalitpur',
      neededBy: '2026-09-20', compatOk: false, status: 'Pending',
      requester: 'Ramesh Shrestha',
      created: '2026-09-13T08:10:00',
      timeline: [{ when: '2026-09-13 08:10', what: 'Request submitted — 22 compatible donors notified' }]
    },
    {
      id: 'BB-7HD4LN', blood: 'AB-', units: '1', urgency: 'Normal',
      hospital: 'Tribhuvan University Teaching Hospital, Maharajgunj', district: 'Kathmandu',
      neededBy: '2026-08-30', compatOk: true, status: 'Completed',
      requester: 'Anita Rai',
      created: '2026-08-28T14:45:00',
      donor: 'Priya Karki (AB- exact match)', donorAt: '2026-08-28T15:30:00',
      timeline: [
        { when: '2026-08-28 14:45', what: 'Request submitted — 6 compatible donors notified' },
        { when: '2026-08-28 15:30', what: 'Priya Karki accepted the request' },
        { when: '2026-08-30 10:15', what: 'Donation completed & verified' }
      ]
    },
    /* A platform-level pending request the admin can forward in the demo */
    {
      id: 'BB-5MN8VX', blood: 'O-', units: '2', urgency: 'Urgent',
      hospital: '—', district: 'Kathmandu',
      neededBy: '2026-09-16', compatOk: false, status: 'Pending',
      requester: 'Kiran Adhikari',
      created: '2026-09-13T10:05:00',
      timeline: [{ when: '2026-09-13 10:05', what: 'Request submitted — 9 compatible donors notified' }]
    }
  ];

  var SEED_INVENTORY = {
    'O+': 24, 'O-': 3, 'A+': 18, 'A-': 6,
    'B+': 12, 'B-': 2, 'AB+': 8, 'AB-': 5
  };

  var SEED_USERS = [
    { id: 'USR-1042', name: 'Arun Shrestha',   email: 'arun.s@example.com',       role: 'Donor',     blood: 'B+',   district: 'Kathmandu', joined: '2025-11-02', status: 'Active' },
    { id: 'USR-1039', name: 'Sunita Maharjan', email: 'sunita.m@example.com',     role: 'Donor',     blood: 'B+',   district: 'Lalitpur',  joined: '2025-10-18', status: 'Active' },
    { id: 'USR-1036', name: 'Bikash Tamang',   email: 'bikash.t@example.com',     role: 'Donor',     blood: 'B-',   district: 'Kathmandu', joined: '2025-10-09', status: 'Suspended' },
    { id: 'USR-1033', name: 'Sita Gurung',     email: 'sita.g@example.com',       role: 'Requester', blood: '—',    district: 'Kathmandu', joined: '2025-12-01', status: 'Active' },
    { id: 'USR-1030', name: 'Ramesh Shrestha', email: 'ramesh.s@example.com',     role: 'Requester', blood: '—',    district: 'Bhaktapur', joined: '2026-01-14', status: 'Active' },
    { id: 'USR-1028', name: 'TUTH Blood Bank', email: 'bloodbank@tuth.edu.np',    role: 'Hospital',  blood: '46 u', district: 'Kathmandu', joined: '2025-08-20', status: 'Active' },
    { id: 'USR-1027', name: 'Bir Hospital',    email: 'admin@birhospital.org.np', role: 'Hospital',  blood: '38 u', district: 'Kathmandu', joined: '2025-08-20', status: 'Active' },
    { id: 'USR-1026', name: 'Patan Hospital',  email: 'bb@patanhospital.org.np',  role: 'Hospital',  blood: '51 u', district: 'Lalitpur',  joined: '2025-08-21', status: 'Active' },
    { id: 'USR-1021', name: 'Anita Rai',       email: 'anita.r@example.com',      role: 'Donor',     blood: 'O-',   district: 'Pokhara',   joined: '2026-02-03', status: 'Active' },
    { id: 'USR-1015', name: 'Admin · Saksham', email: 'saksham@bloodbuddy.np',    role: 'Admin',     blood: '—',    district: 'Lalitpur',  joined: '2025-08-01', status: 'Active' }
  ];

  /* Migrate: seeds used per-page before bb-store existed */
  function migrate() {
    var oldRequests = readKey('bb_requests_migrated', null);
    if (oldRequests === null) {
      try {
        var legacy = JSON.parse(localStorage.getItem('bb_requests') || '[]');
        if (legacy.length) {
          var reqs = readKey('requests', SEED_REQUESTS.slice());
          legacy.forEach(function (r) {
            if (!r.requester) r.requester = 'You';
            if (r.status === 'Cancelled') return; // keep terminal legacy states out
            reqs.unshift(r);
          });
          writeKey('requests', reqs);
        }
      } catch (e) { /* ignore corrupt legacy data */ }
      writeKey('bb_requests_migrated', true);
    }
  }

  function ensureSeeded() {
    if (readKey('requests', null) === null) writeKey('requests', SEED_REQUESTS.slice());
    if (readKey('inventory', null) === null) writeKey('inventory', SEED_INVENTORY);
    if (readKey('users', null) === null) writeKey('users', SEED_USERS.slice());
    migrate();
  }

  /* ---------------- PUBLIC API ---------------- */

  var Store = {
    /* Returns the array/object for a collection (a fresh copy). */
    get: function (key) { return readKey(key, null); },

    /* Overwrites the whole collection. */
    set: function (key, value) { writeKey(key, value); },

    /* ---- Requests helpers ---- */
    requests: {
      all: function () { return readKey('requests', []); },
      save: function (list) { writeKey('requests', list); },
      find: function (id) {
        return this.all().filter(function (r) { return r.id === id; })[0] || null;
      },
      add: function (req) {
        var list = this.all();
        list.unshift(req);
        this.save(list);
        return req;
      },
      /* Update a request by id; `patch` fields are merged in. Returns the updated request. */
      update: function (id, patch) {
        var list = this.all();
        for (var i = 0; i < list.length; i++) {
          if (list[i].id === id) {
            Object.keys(patch).forEach(function (k) { list[i][k] = patch[k]; });
            this.save(list);
            return list[i];
          }
        }
        return null;
      }
    },

    /* ---- Inventory helpers ---- */
    inventory: {
      all: function () { return readKey('inventory', {}); },
      save: function (inv) { writeKey('inventory', inv); },
      setUnits: function (group, units) {
        var inv = this.all();
        inv[group] = units;
        this.save(inv);
        return inv[group];
      }
    },

    /* ---- Users helpers (admin) ---- */
    users: {
      all: function () { return readKey('users', []); },
      save: function (list) { writeKey('users', list); },
      setStatus: function (id, status) {
        var list = this.all();
        for (var i = 0; i < list.length; i++) {
          if (list[i].id === id) { list[i].status = status; this.save(list); return list[i]; }
        }
        return null;
      }
    },

    /* ---- Demo reset (used by a "reset demo data" control if you add one) ---- */
    reset: function () {
      ['requests', 'inventory', 'users', 'bb_requests_migrated'].forEach(function (k) {
        localStorage.removeItem(PREFIX + k);
      });
      localStorage.removeItem('bb_requests'); // legacy key
      ensureSeeded();
    }
  };

  ensureSeeded();
  window.BloodBuddyStore = Store;
})();
