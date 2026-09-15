/* =============================================
   BLOODBUDY — bb-api.js
   REST API layer — the ONLY place pages talk to
   the backend (proposal §1.4: "AJAX/Fetch API
   calls to communicate with backend REST
   endpoints"; CPJ119 §2.6).

   Endpoints mirror proposal Appendix B:
     POST   /api/auth/register
     POST   /api/auth/login
     GET    /api/donors/search
     GET    /api/donors/{id}
     GET    /api/requests            (?requester=&donor=&hospital=&guest=)
     POST   /api/requests
     GET    /api/requests/{id}
     PATCH  /api/requests/{id}       (status / moderation / fulfilment)
     GET    /api/hospitals/inventory
     PUT    /api/hospitals/inventory/{bloodGroup}
     GET    /api/admin/users
     PATCH  /api/admin/users/{id}
     GET/PUT /api/profile/{role}     (GET /api/auth/me + PUT later)

   ── MOCK MODE (current) ─────────────────────────
   MOCK = true serves every call from the shared
   localStorage demo store (BloodBuddyStore) with
   simulated latency, so the whole UI already runs
   on the promise/fetch flow. Store mutations land
   SYNCHRONOUSLY (only the response resolution is
   delayed) — demo data stays consistent for
   direct localStorage inspection.

   ── REAL MODE (CPP401) ──────────────────────────
   Set MOCK = false and every call goes through
   fetch() to the Spring endpoints above. Pages
   keep working unchanged — that is the point of
   this layer.

   BASE is the origin prefix only; the paths above
   already carry /api/, so BASE stays '' and the
   browser resolves them against the page's own
   origin. Verify with tools/mock_api_server.py
   (serves the pages AND the Appendix B stub):
     python tools/mock_api_server.py -p 8322
     open http://localhost:8322/resptest/real-mode-check.html
   ============================================= */

(function () {
  'use strict';

  var MOCK = true;          /* false → every call goes through fetch() */
  var BASE = '';            /* prefix for the real backend; every path below
                               already starts with /api/, so this stays ''
                               (point it at http://host:port for a remote API) */
  var LATENCY = 60;         /* simulated network delay, ms (mock mode) */

  function ApiError(message, status) {
    this.name = 'ApiError';
    this.message = message || 'Request failed';
    this.status = status || 0;
  }
  ApiError.prototype = Object.create(Error.prototype);

  function qs(params) {
    if (!params) return '';
    var pairs = Object.keys(params).filter(function (k) {
      return params[k] !== undefined && params[k] !== null && params[k] !== '';
    }).map(function (k) {
      return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
    });
    return pairs.length ? '?' + pairs.join('&') : '';
  }

  /* ---- Real fetch transport (CPP401) ---- */
  function realRequest(method, path, body) {
    return fetch(BASE + path, {
      method: method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    }).then(function (res) {
      if (!res.ok) {
        return res.json().catch(function () { return {}; }).then(function (err) {
          throw new ApiError(err.message || res.statusText, res.status);
        });
      }
      return res.status === 204 ? null : res.json();
    });
  }

  /* ---- Mock transport (demo store) ---- */
  function mockRequest(method, path, body) {
    var result;
    try {
      result = mockRoute(method, path, body); /* store mutations happen NOW (synchronously) */
    } catch (e) {
      return Promise.reject(e instanceof ApiError ? e : new ApiError(String(e && e.message || e), 500));
    }
    /* ...only the resolution is delayed, so pages exercise the async flow */
    return new Promise(function (resolve) {
      setTimeout(function () { resolve(result); }, LATENCY);
    });
  }

  function request(method, path, body) {
    return MOCK ? mockRequest(method, path, body) : realRequest(method, path, body);
  }

  function clone(v) { return v == null ? v : JSON.parse(JSON.stringify(v)); }

  /* ---- Mock routing table (mirrors Appendix B) ---- */
  function mockRoute(method, path, body) {
    var Store = window.BloodBuddyStore;

    var hash = path.indexOf('?');
    var query = {};
    if (hash > -1) {
      new URLSearchParams(path.slice(hash + 1)).forEach(function (v, k) { query[k] = v; });
    }
    var seg = path.slice(0, hash > -1 ? hash : undefined).split('/').filter(Boolean);
    var r = seg[0] === 'api' ? seg.slice(1) : seg;

    /* --- auth --- */
    if (r[0] === 'auth') {
      if (r[1] === 'register' && method === 'POST') return mockRegister(Store, body);
      if (r[1] === 'login' && method === 'POST') {
        return { ok: true, token: 'demo-' + Date.now().toString(36) };
      }
      if (r[1] === 'forgot' && method === 'POST') {
        return { ok: true, message: 'Password reset link sent (demo — no real email).' };
      }
    }

    /* --- contact (site form; not an Appendix B endpoint) --- */
    if (r[0] === 'contact' && method === 'POST') {
      return { ok: true, message: 'Message received (demo — no real send).' };
    }

    /* --- donors --- */
    if (r[0] === 'donors') {
      /* GET /api/donors/search — the donor search endpoint (Appendix B).
         Must be checked BEFORE the /{id} lookup, or "search" is read as an id. */
      if (method === 'GET' && (r[1] === 'search' || !r[1])) return clone(searchDonors(Store, query));
      if (method === 'GET' && r[1]) {
        var one = Store.donors.all().filter(function (d) { return d.id === decodeURIComponent(r[1]); })[0];
        if (!one) throw new ApiError('Donor not found', 404);
        return clone(one);
      }
    }

    /* --- requests --- */
    if (r[0] === 'requests') {
      if (method === 'GET' && r[1]) {
        var req = Store.requests.find(decodeURIComponent(r[1]));
        if (!req) throw new ApiError('Request not found', 404);
        return clone(req);
      }
      if (method === 'GET') return clone(filterRequests(Store, query));
      if (method === 'POST') return clone(createRequest(Store, body));
      if (method === 'PATCH' && r[1]) {
        var updated = Store.requests.update(decodeURIComponent(r[1]), body || {});
        if (!updated) throw new ApiError('Request not found', 404);
        return clone(updated);
      }
    }

    /* --- hospital inventory --- */
    if (r[0] === 'hospitals' && r[1] === 'inventory') {
      if (method === 'GET' && !r[2]) return clone(Store.inventory.all());
      if (method === 'PUT' && r[2]) {
        var group = decodeURIComponent(r[2]);
        var units = Store.inventory.setUnits(group, Number(body && body.units) || 0);
        return { bloodGroup: group, units: units };
      }
    }

    /* --- admin users --- */
    if (r[0] === 'admin' && r[1] === 'users') {
      if (method === 'GET' && !r[2]) return clone(Store.users.all());
      if (method === 'PATCH' && r[2]) {
        var u = Store.users.setStatus(decodeURIComponent(r[2]), (body && body.status) || '');
        if (!u) throw new ApiError('User not found', 404);
        return clone(u);
      }
    }

    /* --- profile (demo-level stand-in for GET /api/auth/me + PUT) --- */
    if (r[0] === 'profile' && r[1]) {
      var key = 'bb_' + r[1] + '_profile';
      if (method === 'GET') {
        try { return JSON.parse(localStorage.getItem(key) || 'null'); } catch (e) { return null; }
      }
      if (method === 'PUT') {
        localStorage.setItem(key, JSON.stringify(body || {}));
        return clone(body);
      }
    }

    throw new ApiError('No mock route for ' + method + ' ' + path, 501);
  }

  /* ---- Mock handlers ---- */

  function mockRegister(Store, body) {
    var roleMap = { donor: 'Donor', requester: 'Requester', hospital: 'Hospital', admin: 'Admin' };
    var row = {
      id: 'USR-' + Date.now().toString(36).toUpperCase().slice(-4),
      name: (body && body.name) || 'New user',
      email: (body && body.email) || '',
      role: roleMap[(body && body.role) || 'donor'] || 'Donor',
      blood: (body && body.blood) || '—',
      district: (body && body.district) || '—',
      joined: new Date().toISOString().slice(0, 10),
      status: 'Active'
    };
    var users = Store.users.all();
    users.push(row);
    Store.users.save(users);
    return clone(row);
  }

  function searchDonors(Store, q) {
    var list = Store.donors.all();
    if (q.blood) list = list.filter(function (d) { return d.blood === q.blood; });
    if (q.district) {
      var needle = q.district.toLowerCase();
      list = list.filter(function (d) { return (d.area || '').toLowerCase().indexOf(needle) > -1; });
    }
    if (q.available === 'true') list = list.filter(function (d) { return d.avail === true; });
    return list;
  }

  function filterRequests(Store, q) {
    var list = Store.requests.all();
    if (q.requester) list = list.filter(function (r) { return r.requester === q.requester; });
    if (q.hospital) list = list.filter(function (r) { return r.hospital === q.hospital; });
    if (q.guest === 'true') list = list.filter(function (r) { return (r.requester || '').indexOf('Guest') === 0; });
    return list;
  }

  function createRequest(Store, body) {
    var b = body || {};
    var now = new Date();
    var req = {
      id: b.id || (b.emergency ? 'EM-' : 'BB-') + now.getTime().toString(36).toUpperCase(),
      blood: b.blood || '',
      units: String(b.units || '1'),
      urgency: b.urgency || 'Normal',
      neededBy: b.neededBy || '',
      hospital: Store.normalizeHospital(b.hospital || '—'),
      district: b.district || '',
      notes: b.notes || '',
      compatOk: !!b.compatOk,
      status: 'Pending',
      requester: b.requester || 'You',
      created: now.toISOString(),
      timeline: b.timeline && b.timeline.length ? b.timeline : [
        { when: now.toISOString().slice(0, 16).replace('T', ' '), what: 'Request submitted — compatible donors notified' }
      ]
    };
    Store.requests.add(req);
    return clone(req);
  }

  /* ---- Public facade (all calls return Promises) ---- */

  window.BloodBuddyAPI = {
    ApiError: ApiError,
    isMock: function () { return MOCK; },

    auth: {
      register: function (body) { return request('POST', '/api/auth/register', body); },
      login: function (body) { return request('POST', '/api/auth/login', body); },
      forgot: function (body) { return request('POST', '/api/auth/forgot', body); }
    },

    contact: function (body) { return request('POST', '/api/contact', body); },

    donors: {
      /* params: { blood, district, available: 'true'|'false' } */
      search: function (params) { return request('GET', '/api/donors/search' + qs(params)); },
      get: function (id) { return request('GET', '/api/donors/' + encodeURIComponent(id)); }
    },

    requests: {
      /* params: { requester, hospital, guest: 'true' } */
      list: function (params) { return request('GET', '/api/requests' + qs(params)); },
      get: function (id) { return request('GET', '/api/requests/' + encodeURIComponent(id)); },
      create: function (body) { return request('POST', '/api/requests', body); },
      /* patch: { status, hospital, donor, donorAt, timeline, notes... } */
      update: function (id, patch) { return request('PATCH', '/api/requests/' + encodeURIComponent(id), patch); }
    },

    inventory: {
      get: function () { return request('GET', '/api/hospitals/inventory'); },
      update: function (group, units) {
        return request('PUT', '/api/hospitals/inventory/' + encodeURIComponent(group), { units: units });
      }
    },

    users: {
      list: function () { return request('GET', '/api/admin/users'); },
      setStatus: function (id, status) {
        return request('PATCH', '/api/admin/users/' + encodeURIComponent(id), { status: status });
      }
    },

    profile: {
      get: function (role) { return request('GET', '/api/profile/' + encodeURIComponent(role)); },
      save: function (role, data) { return request('PUT', '/api/profile/' + encodeURIComponent(role), data); }
    }
  };
})();
