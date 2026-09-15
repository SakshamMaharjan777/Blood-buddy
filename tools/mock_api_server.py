#!/usr/bin/env python3
"""
BloodBuddy — local mock REST API + static file server.

WHY THIS EXISTS
---------------
bb-api.js has always had a real fetch() transport (`realRequest`), but with
`MOCK = true` nothing ever hit the network: the demo ran entirely off
localStorage and DevTools stayed empty. CPP119 requires "basic AJAX/Fetch API
integration to communicate with backend APIs", so the fetch path must be
demonstrably working — and it cannot be while there is no server to talk to.

This is that server. It is a DEV STUB for the Spring backend in CPP401:

  * serves the static site (same as `python -m http.server`) AND
  * implements the proposal's Appendix B endpoints over an in-memory store.

USE IT TO SHOW REAL AJAX TRAFFIC
--------------------------------
  1. start it:      python tools/mock_api_server.py            (port 8321)
  2. flip the flag: in CODE/js/bb-api.js set  MOCK = false
  3. open           http://localhost:8321/CODE/HTML/landing.html
  Every request/login/search/accept now appears in DevTools > Network as a
  real XHR against /api/... with a JSON body, and this process logs each call.

Leave `MOCK = true` (the default) to run the demo with no server at all — that
is how resptest/qa-harness.html works, and why it reads localStorage directly.

WHAT THIS IS NOT
----------------
No auth, no persistence (state is in memory and resets on restart), no
validation, no security. It exists to exercise the frontend's fetch layer and
to let the API be poked from Postman before Spring exists. The authoritative
logic belongs in the CPP401 service layer.

Seed data is duplicated from CODE/js/bb-store.js (which stays the source of
truth for mock mode). Keep the two in step while both exist.
"""

import argparse
import json
import re
import time
from datetime import datetime, timezone
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent

# --------------------------------------------------------------------------
# Seed data (mirror of CODE/js/bb-store.js)
# --------------------------------------------------------------------------

HOSPITAL_ALIASES = [
    ("Tribhuvan University Teaching Hospital", "TUTH, Maharajgunj"),
    ("TUTH", "TUTH, Maharajgunj"),
    ("Bir Hospital", "Bir Hospital, Kathmandu"),
    ("Patan Hospital", "Patan Hospital, Lalitpur"),
    ("Manmohan", "Manmohan Memorial, Kathmandu"),
    ("Nepal Medical College", "Nepal Medical College, Kathmandu"),
]


def normalize_hospital(name):
    """Map a raw hospital label onto the canonical one the queues filter by."""
    n = str(name or "").strip()
    if not n or n in ("—", "Other"):
        return n
    for prefix, canonical in HOSPITAL_ALIASES:
        if n.startswith(prefix):
            return canonical
    return n


SEED_REQUESTS = [
    {
        "id": "BB-8F3K2M", "blood": "O+", "units": "2", "urgency": "Urgent",
        "hospital": "Bir Hospital, Kathmandu", "district": "Kathmandu",
        "neededBy": "2026-09-15", "compatOk": True, "status": "Matched",
        "requester": "Sita Gurung", "created": "2026-09-12T09:24:00",
        "donor": "Arun Shrestha (B+ → O+ compatible)", "donorAt": "2026-09-12T11:02:00",
        "timeline": [
            {"when": "2026-09-12 09:24", "what": "Request submitted — 14 compatible donors notified"},
            {"when": "2026-09-12 11:02", "what": "Arun Shrestha accepted the request"},
        ],
    },
    {
        "id": "BB-2QX9P1", "blood": "B+", "units": "1", "urgency": "Normal",
        "hospital": "Patan Hospital, Lalitpur", "district": "Lalitpur",
        "neededBy": "2026-09-20", "compatOk": False, "status": "Pending",
        "requester": "Ramesh Shrestha", "created": "2026-09-13T08:10:00",
        "timeline": [{"when": "2026-09-13 08:10", "what": "Request submitted — 22 compatible donors notified"}],
    },
    {
        "id": "BB-7HD4LN", "blood": "AB-", "units": "1", "urgency": "Normal",
        "hospital": "Tribhuvan University Teaching Hospital, Maharajgunj", "district": "Kathmandu",
        "neededBy": "2026-08-30", "compatOk": True, "status": "Completed",
        "requester": "Anita Rai", "created": "2026-08-28T14:45:00",
        "donor": "Priya Karki (AB- exact match)", "donorAt": "2026-08-28T15:30:00",
        "timeline": [
            {"when": "2026-08-28 14:45", "what": "Request submitted — 6 compatible donors notified"},
            {"when": "2026-08-28 15:30", "what": "Priya Karki accepted the request"},
            {"when": "2026-08-30 10:15", "what": "Donation completed & verified"},
        ],
    },
    {
        "id": "BB-5MN8VX", "blood": "O-", "units": "2", "urgency": "Urgent",
        "hospital": "—", "district": "Kathmandu",
        "neededBy": "2026-09-16", "compatOk": False, "status": "Pending",
        "requester": "Kiran Adhikari", "created": "2026-09-13T10:05:00",
        "timeline": [{"when": "2026-09-13 10:05", "what": "Request submitted — 9 compatible donors notified"}],
    },
]

SEED_INVENTORY = {"O+": 24, "O-": 3, "A+": 18, "A-": 6, "B+": 12, "B-": 2, "AB+": 8, "AB-": 5}

SEED_USERS = [
    {"id": "USR-1042", "name": "Arun Shrestha", "email": "arun.s@example.com", "role": "Donor", "blood": "B+", "district": "Kathmandu", "joined": "2025-11-02", "status": "Active"},
    {"id": "USR-1039", "name": "Sunita Maharjan", "email": "sunita.m@example.com", "role": "Donor", "blood": "B+", "district": "Lalitpur", "joined": "2025-10-18", "status": "Active"},
    {"id": "USR-1036", "name": "Bikash Tamang", "email": "bikash.t@example.com", "role": "Donor", "blood": "B-", "district": "Kathmandu", "joined": "2025-10-09", "status": "Suspended"},
    {"id": "USR-1033", "name": "Sita Gurung", "email": "sita.g@example.com", "role": "Requester", "blood": "—", "district": "Kathmandu", "joined": "2025-12-01", "status": "Active"},
    {"id": "USR-1030", "name": "Ramesh Shrestha", "email": "ramesh.s@example.com", "role": "Requester", "blood": "—", "district": "Bhaktapur", "joined": "2026-01-14", "status": "Active"},
    {"id": "USR-1028", "name": "TUTH Blood Bank", "email": "bloodbank@tuth.edu.np", "role": "Hospital", "blood": "46 u", "district": "Kathmandu", "joined": "2025-08-20", "status": "Active"},
    {"id": "USR-1027", "name": "Bir Hospital", "email": "admin@birhospital.org.np", "role": "Hospital", "blood": "38 u", "district": "Kathmandu", "joined": "2025-08-20", "status": "Active"},
    {"id": "USR-1026", "name": "Patan Hospital", "email": "bb@patanhospital.org.np", "role": "Hospital", "blood": "51 u", "district": "Lalitpur", "joined": "2025-08-21", "status": "Active"},
    {"id": "USR-1021", "name": "Anita Rai", "email": "anita.r@example.com", "role": "Donor", "blood": "O-", "district": "Pokhara", "joined": "2026-02-03", "status": "Active"},
    {"id": "USR-1015", "name": "Admin · Saksham", "email": "saksham@bloodbuddy.np", "role": "Admin", "blood": "—", "district": "Lalitpur", "joined": "2025-08-01", "status": "Active"},
]

SEED_DONORS = [
    {"id": "DNR-1001", "name": "Arun Shrestha", "blood": "B+", "area": "Baneshwor, Kathmandu", "avail": True, "compat": True, "last": "4 months ago"},
    {"id": "DNR-1002", "name": "Sunita Maharjan", "blood": "B+", "area": "Thamel, Kathmandu", "avail": True, "compat": False, "last": "6 months ago"},
    {"id": "DNR-1003", "name": "Bikash Tamang", "blood": "B-", "area": "Koteshwor, Kathmandu", "avail": False, "compat": False, "last": "2 months ago"},
    {"id": "DNR-1004", "name": "Priya Karki", "blood": "B+", "area": "Baluwatar, Kathmandu", "avail": True, "compat": True, "last": "5 months ago"},
    {"id": "DNR-1005", "name": "Rajan Thapa", "blood": "B+", "area": "Chabahil, Kathmandu", "avail": True, "compat": False, "last": "8 months ago"},
    {"id": "DNR-1006", "name": "Deepa Rana", "blood": "B+", "area": "Lazimpat, Kathmandu", "avail": True, "compat": False, "last": "1 year ago"},
    {"id": "DNR-1007", "name": "Kiran Gurung", "blood": "B-", "area": "Gongabu, Kathmandu", "avail": False, "compat": False, "last": "3 months ago"},
    {"id": "DNR-1008", "name": "Mina Shrestha", "blood": "B+", "area": "Maharajgunj, Kathmandu", "avail": True, "compat": False, "last": "7 months ago"},
    {"id": "DNR-1009", "name": "Dipesh Limbu", "blood": "A+", "area": "Sorakhutte, Kathmandu", "avail": True, "compat": False, "last": "1 month ago"},
    {"id": "DNR-1010", "name": "Anjali Karki", "blood": "AB+", "area": "Swayambhu, Kathmandu", "avail": False, "compat": False, "last": "7 months ago"},
    {"id": "DNR-1011", "name": "Kabita Gurung", "blood": "B-", "area": "Kupondole, Lalitpur", "avail": True, "compat": False, "last": "3 months ago"},
    {"id": "DNR-1012", "name": "Nabin Chaudhary", "blood": "O+", "area": "Pulchowk, Lalitpur", "avail": True, "compat": False, "last": "2 months ago"},
    {"id": "DNR-1013", "name": "Sarita Thapa", "blood": "O-", "area": "Lakeside, Pokhara", "avail": True, "compat": False, "last": "5 months ago"},
    {"id": "DNR-1014", "name": "Roshan Bhattarai", "blood": "O+", "area": "Zero KM, Chitwan", "avail": True, "compat": False, "last": "4 months ago"},
]


# --------------------------------------------------------------------------
# In-memory store
# --------------------------------------------------------------------------

class Store:
    """In-memory mirror of what bb-store.js keeps in localStorage.

    Deliberately not persisted: restarting the server is the equivalent of the
    demo's "Reset demo data" control.
    """

    def __init__(self):
        self.requests = [dict(r) for r in SEED_REQUESTS]
        self.inventory = dict(SEED_INVENTORY)
        self.users = [dict(u) for u in SEED_USERS]
        self.donors = [dict(d) for d in SEED_DONORS]
        self.profiles = {}          # role -> profile object

    # -- helpers ---------------------------------------------------------
    def find_request(self, rid):
        for r in self.requests:
            if r.get("id") == rid:
                return r
        return None

    def find_user(self, uid):
        for u in self.users:
            if u.get("id") == uid:
                return u
        return None


def base36(value):
    """Same short-id shape the frontend generates (Date.now() in base 36)."""
    digits = "0123456789abcdefghijklmnopqrstuvwxyz"
    if value == 0:
        return "0"
    out = ""
    while value:
        value, rem = divmod(value, 36)
        out = digits[rem] + out
    return out


def create_request(store, body):
    now = datetime.now(timezone.utc)
    stamp = now.isoformat().replace("+00:00", "")
    prefix = "EM-" if body.get("emergency") else "BB-"
    req = {
        "id": body.get("id") or prefix + base36(int(time.time() * 1000)).upper(),
        "blood": body.get("blood", ""),
        "units": str(body.get("units", "1")),
        "urgency": body.get("urgency", "Normal"),
        "neededBy": body.get("neededBy", ""),
        "hospital": normalize_hospital(body.get("hospital", "—")),
        "district": body.get("district", ""),
        "notes": body.get("notes", ""),
        "compatOk": bool(body.get("compatOk")),
        "status": "Pending",
        "requester": body.get("requester", "You"),
        "created": stamp,
        "timeline": body.get("timeline") or [
            {"when": stamp[:16].replace("T", " "),
             "what": "Request submitted — compatible donors notified"}
        ],
    }
    store.requests.insert(0, req)
    return req


def search_donors(store, query):
    blood = query.get("blood", [""])[0]
    district = query.get("district", [""])[0]
    available = query.get("available", [""])[0]
    out = store.donors
    if blood:
        out = [d for d in out if d.get("blood") == blood]
    if district:
        needle = district.lower()
        out = [d for d in out if needle in (d.get("area") or "").lower()]
    if available == "true":
        out = [d for d in out if d.get("avail") is True]
    return out


def filter_requests(store, query):
    out = store.requests
    requester = query.get("requester", [""])[0]
    hospital = query.get("hospital", [""])[0]
    guest = query.get("guest", [""])[0]
    if requester:
        out = [r for r in out if r.get("requester") == requester]
    if hospital:
        out = [r for r in out if r.get("hospital") == hospital]
    if guest == "true":
        out = [r for r in out if str(r.get("requester", "")).startswith("Guest")]
    return out


# --------------------------------------------------------------------------
# HTTP handler
# --------------------------------------------------------------------------

class Handler(SimpleHTTPRequestHandler):
    server_version = "BloodBuddyMockAPI/1.0"
    store = Store()

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    # -- plumbing --------------------------------------------------------
    def log_message(self, fmt, *args):
        # Static files only; API calls are logged with their status in _send().
        pass

    def _send(self, status, payload):
        # One line per API call, with the status, so the traffic is visible in
        # the terminal as well as in DevTools.
        print("  %-6s %-46s -> %d" % (self.command, self.path, status), flush=True)
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        # Permissive CORS so the same stub can be driven from Postman or a page
        # served on another port without extra setup.
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        length = int(self.headers.get("Content-Length") or 0)
        if not length:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            return {}

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    # -- routing ---------------------------------------------------------
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._api("GET", parsed)
        return super().do_GET()

    def do_HEAD(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._api("GET", parsed)
        return super().do_HEAD()

    def do_POST(self):
        self._api("POST", urlparse(self.path))

    def do_PUT(self):
        self._api("PUT", urlparse(self.path))

    def do_PATCH(self):
        self._api("PATCH", urlparse(self.path))

    # -- API -------------------------------------------------------------
    def _api(self, method, parsed):
        seg = [unquote(s) for s in parsed.path.split("/") if s][1:]  # drop "api"
        query = parse_qs(parsed.query)
        body = self._body()
        store = self.store

        try:
            # ---- auth ----
            if seg[:1] == ["auth"]:
                if seg[1:2] == ["register"] and method == "POST":
                    return self._register(body)
                if seg[1:2] == ["login"] and method == "POST":
                    return self._send(200, {"ok": True, "token": "demo-" + base36(int(time.time()))})
                if seg[1:2] == ["forgot"] and method == "POST":
                    return self._send(200, {"ok": True, "message": "Password reset link sent (mock — no real email)."})

            # ---- contact ----
            if seg[:1] == ["contact"] and method == "POST":
                return self._send(200, {"ok": True, "message": "Message received (mock — no real send)."})

            # ---- donors ----
            if seg[:1] == ["donors"]:
                if method == "GET" and (seg[1:2] == ["search"] or len(seg) == 1):
                    return self._send(200, search_donors(store, query))
                if method == "GET" and len(seg) > 1:
                    for d in store.donors:
                        if d["id"] == seg[1]:
                            return self._send(200, d)
                    return self._send(404, {"message": "Donor not found"})

            # ---- requests ----
            if seg[:1] == ["requests"]:
                if method == "GET" and len(seg) > 1:
                    r = store.find_request(seg[1])
                    return self._send(200, r) if r else self._send(404, {"message": "Request not found"})
                if method == "GET":
                    return self._send(200, filter_requests(store, query))
                if method == "POST":
                    return self._send(201, create_request(store, body))
                if method == "PATCH" and len(seg) > 1:
                    r = store.find_request(seg[1])
                    if not r:
                        return self._send(404, {"message": "Request not found"})
                    r.update(body or {})
                    return self._send(200, r)

            # ---- hospital inventory ----
            if seg[:2] == ["hospitals", "inventory"]:
                if method == "GET" and len(seg) == 2:
                    return self._send(200, store.inventory)
                if method == "PUT" and len(seg) > 2:
                    group = seg[2]
                    units = int((body or {}).get("units") or 0)
                    store.inventory[group] = units
                    return self._send(200, {"bloodGroup": group, "units": units})

            # ---- admin users ----
            if seg[:2] == ["admin", "users"]:
                if method == "GET" and len(seg) == 2:
                    return self._send(200, store.users)
                if method == "PATCH" and len(seg) > 2:
                    u = store.find_user(seg[2])
                    if not u:
                        return self._send(404, {"message": "User not found"})
                    u["status"] = (body or {}).get("status", u["status"])
                    return self._send(200, u)

            # ---- profile (stand-in for GET /api/auth/me + a role-scoped PUT) ----
            if seg[:1] == ["profile"] and len(seg) > 1:
                role = seg[1]
                if method == "GET":
                    return self._send(200, store.profiles.get(role))
                if method == "PUT":
                    store.profiles[role] = body or {}
                    return self._send(200, store.profiles[role])

            return self._send(501, {"message": "No mock route for %s /%s" % (method, "/".join(seg))})
        except Exception as exc:  # noqa: BLE001 - dev stub: report, never 500 silently
            return self._send(500, {"message": str(exc)})

    def _register(self, body):
        role_map = {"donor": "Donor", "requester": "Requester", "hospital": "Hospital", "admin": "Admin"}
        row = {
            "id": "USR-" + base36(int(time.time() * 1000)).upper()[-4:],
            "name": body.get("name") or "New user",
            "email": body.get("email", ""),
            "role": role_map.get(body.get("role", "donor"), "Donor"),
            "blood": body.get("blood", "—"),
            "district": body.get("district", "—"),
            "joined": datetime.now().strftime("%Y-%m-%d"),
            "status": "Active",
        }
        self.store.users.append(row)
        return self._send(201, row)


def main():
    parser = argparse.ArgumentParser(description="BloodBuddy mock API + static server (CPP401 stub)")
    parser.add_argument("-p", "--port", type=int, default=8321)
    args = parser.parse_args()

    httpd = ThreadingHTTPServer(("", args.port), Handler)
    print("BloodBuddy mock API + static server")
    print("  static : http://localhost:%d/CODE/HTML/landing.html" % args.port)
    print("  api    : http://localhost:%d/api/...  (see docs/BloodBuddy_Proposal_Revised.docx Appendix B)" % args.port)
    print()
    print("  Real fetch traffic requires MOCK = false in CODE/js/bb-api.js.")
    print("  With MOCK = true (default) this server only serves the files.")
    print("  Ctrl+C to stop.")
    print()
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nstopped")


if __name__ == "__main__":
    main()
