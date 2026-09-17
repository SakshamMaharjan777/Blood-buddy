"""B3/B6 HTTP exercise — runs against the booted backend (default http://localhost:8081).

Start the app first (see backend/README.md), then:

    python tools/api_check.py                 # 94 checks over real HTTP
    python tools/api_check.py http://host:8081

It authenticates like a browser does — POST /api/auth/login, then the JSESSIONID
cookie is carried by every later call — and asserts the STATUS CODES and the
business-rule MESSAGES, including the refusals. Since B6 it also asserts the RBAC
boundary itself: anonymous → 401, wrong role → 403, and logout really ends the
session. That is the part a Postman collection will not re-check for you.

Known footprint: it deliberately mutates demo data (creates and cancels a guest
request, creates a member request, admin-forwards BB-5MN8VX, registers three accounts
— a donor and two hospital staff, one of them deliberately unlinkable). Nothing can
undo those over the API, so the run PRINTS the SQL that does — run it afterwards if
you pointed this at the demo schema. Every account it creates is named
'api-check+…@example.com', so one LIKE pattern cleans up all of them.
"""
import http.cookiejar
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

# The check names use § and →; Windows consoles default to cp1252 and would
# crash printing them.
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081"
DEMO_PASSWORD = "BloodBuddy#2026"
NOTE = "HTTP check"          # the marker the cleanup SQL keys on
passed, failed, created = [], [], {}


def opener_for(email=None, password=DEMO_PASSWORD, role=None):
    """A cookie-jar session; logs in first when credentials are given."""
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    if email:
        status, body = call(opener, "POST", "/api/auth/login",
                            {"email": email, "password": password, "role": role})
        if status != 200:
            raise SystemExit("could not sign in as %s: %s %s" % (email, status, body))
    return opener


def call(opener, method, path, body=None):
    req = urllib.request.Request(BASE + path,
                                 data=json.dumps(body).encode() if body is not None else None,
                                 method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    def parse(raw, content_type):
        if content_type == "application/json":
            try:
                return json.loads(raw) if raw.strip() else None
            except ValueError:
                return raw.decode("utf-8", "replace")
        # HTML (static pages) and image bytes are valid answers too — never decode a
        # PNG as JSON, and keep the report readable if it is large.
        text = raw.decode("utf-8", "replace")
        return text if len(text) < 400 else "<%d bytes of %s>" % (len(raw), content_type)

    try:
        with opener.open(req) as res:
            return res.status, parse(res.read(), res.headers.get_content_type())
    except urllib.error.HTTPError as e:
        return e.code, parse(e.read(), e.headers.get_content_type())


def check(name, ok, detail=""):
    (passed if ok else failed).append(name)
    print(("  [PASS] " if ok else "  [FAIL] ") + name + ("" if ok else "  ->  " + str(detail)[:300]))


anon = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

# ---------------- B6: the public surface, and who gets in ----------------
s, landing = call(anon, "GET", "/CODE/HTML/landing.html")
check("the static frontend is public", s == 200, s)
s, guest_req = call(anon, "POST", "/api/requests", {
    "emergency": True, "blood": "O-", "units": 1, "urgency": "Urgent", "neededBy": "2026-10-01",
    "hospital": "Patan Hospital", "district": "Lalitpur", "notes": NOTE, "contactPhone": "9801234567"})
check("the guest emergency form works with nobody signed in (201, EM- code)",
      s == 201 and str(guest_req.get("id", "")).startswith("EM-"), (s, guest_req))
check("guest row labelled the demo's way", guest_req.get("requester") == "Guest (emergency)"), guest_req
created["guest"] = guest_req.get("id")
s, lookup = call(anon, "GET", "/api/requests/lookup/" + str(created["guest"]))
check("BR-8 public status lookup works anonymously", s == 200 and lookup["id"] == created["guest"], (s, lookup))
s, leaked = call(anon, "GET", "/api/requests/lookup/BB-8F3K2M")
check("BR-8: the lookup answers 404 for a member's BB- code", s == 404, (s, leaked))
s, contact = call(anon, "POST", "/api/contact", {"name": "QA", "email": "qa@example.com", "message": "HTTP check"})
check("POST /api/contact is public", s == 200 and contact.get("ok") is True, (s, contact))
s, empty_contact = call(anon, "POST", "/api/contact", {"name": "QA", "email": "qa@example.com", "message": ""})
check("empty contact message -> 400 validation-failed", s == 400, (s, empty_contact))

s, anon_me = call(anon, "GET", "/api/auth/me")
check("GET /api/auth/me anonymous -> 401 unauthorized", s == 401 and anon_me.get("error") == "unauthorized", (s, anon_me))
s, anon_search = call(anon, "GET", "/api/donors/search?blood=B%2B")
check("donor search anonymous -> 401 (no public donor directory)", s == 401, (s, anon_search))
s, anon_admin = call(anon, "GET", "/api/admin/users")
check("admin users anonymous -> 401", s == 401, (s, anon_admin))
s, anon_inv = call(anon, "PUT", "/api/hospitals/inventory/B%2B", {"units": 1})
check("stock write anonymous -> 401", s == 401, (s, anon_inv))
s, anon_patch = call(anon, "PATCH", "/api/requests/" + str(created["guest"]), {"status": "Cancelled"})
check("cancelling an EM- request anonymously -> 401 (not even a rule check)", s == 401, (s, anon_patch))

# login itself (the session is what everything below rides on)
s, login = call(anon, "POST", "/api/auth/login",
                {"email": "sita.g@example.com", "password": DEMO_PASSWORD, "role": "requester"})
check("POST /api/auth/login -> 200 + role", s == 200 and login.get("user", {}).get("role") == "Requester", (s, login))
check("login response carries no password hash", "passwordHash" not in json.dumps(login))
s, bad = call(anon, "POST", "/api/auth/login", {"email": "sita.g@example.com", "password": "nope", "role": "requester"})
check("wrong password -> 409 with the rule's message", s == 409 and "Invalid email or password" in bad.get("message", ""), (s, bad))
s, wrong_portal = call(anon, "POST", "/api/auth/login",
                       {"email": "sita.g@example.com", "password": DEMO_PASSWORD, "role": "admin"})
check("wrong portal picked -> 409", s == 409 and "portal" in wrong_portal.get("message", ""), (s, wrong_portal))
s, invalid = call(anon, "POST", "/api/auth/register",
                  {"name": "X", "email": "not-an-email", "password": "longenough1", "role": "donor", "blood": "B+"})
check("register with a bad email -> 400 validation-failed", s == 400 and invalid.get("error") == "validation-failed", (s, invalid))
s, admin_self = call(anon, "POST", "/api/auth/register",
                     {"name": "Sneak", "email": "sneak@example.com", "password": "longenough1", "role": "admin"})
check("self-registering an admin -> 409 (provisioned, not signed up for)", s == 409 and "provisioned" in admin_self.get("message", ""), (s, admin_self))

requester = opener_for("sita.g@example.com", role="requester")
donor = opener_for("arun.s@example.com", role="donor")
hospital = opener_for("admin@birhospital.org.np", role="hospital")
admin = opener_for("saksham@bloodbuddy.np", role="admin")

s, me = call(requester, "GET", "/api/auth/me")
check("the login cookie carries the session (GET /api/auth/me -> 200)", s == 200 and me.get("role") == "Requester", (s, me))
s, admin_as_requester = call(requester, "GET", "/api/admin/users")
check("requester on /api/admin/users -> 403 forbidden", s == 403 and admin_as_requester.get("error") == "forbidden", (s, admin_as_requester))
s, donor_on_board = call(donor, "GET", "/api/hospitals/inventory")
check("donor on the inventory board -> 403", s == 403, (s, donor_on_board))
s, hospital_on_admin = call(hospital, "GET", "/api/admin/users")
check("hospital staff on /api/admin/users -> 403", s == 403, (s, hospital_on_admin))
s, hospital_on_other_board = call(hospital, "GET", "/api/hospitals/%s/inventory" % "1")
check("admin-only scoped board -> 403 for hospital staff", s == 403, (s, hospital_on_other_board))

# ---------------- donors ----------------
s, cards = call(requester, "GET", "/api/donors/search?blood=B%2B&district=Kathmandu&available=true")
check("GET /api/donors/search -> 200 + array of donor cards", s == 200 and isinstance(cards, list) and len(cards) > 0, (s, cards))
check("cards carry id/name/blood/area/avail/compat/last",
      all(k in cards[0] for k in ("id", "name", "blood", "area", "avail", "compat", "last")), cards[0] if cards else None)
check("available-only search returns available donors", all(c["avail"] for c in cards))
check("B+ recipient flags B+ donors compatible (BR-1)", all(c["compat"] for c in cards))
s, meta = call(requester, "GET", "/api/donors/search?blood=A%2B&district=Chitwan&available=true&withMeta=true")
check("widen fallback: A+ in Chitwan widens and explains itself",
      s == 200 and meta.get("widened") is True and bool(meta.get("message")), (s, meta))
s, registry = call(requester, "GET", "/api/donors/search")
check("no filters -> the whole registry (14 donors before the run's own registration)",
      s == 200 and len(registry) >= 14, (s, len(registry) if isinstance(registry, list) else registry))
donor_id = registry[0]["id"]
s, one = call(requester, "GET", "/api/donors/" + str(donor_id) + "?blood=B%2B")
check("GET /api/donors/{id} -> one donor card", s == 200 and str(one["id"]) == str(donor_id), (s, one))
s, comp = call(requester, "GET", "/api/donors/compatible?blood=B%2B")
check("GET /api/donors/compatible -> the BR-1 alert set", s == 200 and len(comp) >= 8, (s, len(comp) if isinstance(comp, list) else comp))

# ---------------- requests ----------------
s, tracker = call(requester, "GET", "/api/requests?requester=" + urllib.parse.quote("Sita Gurung"))
check("?requester=<name> (the demo's tracker contract) -> that requester's rows",
      s == 200 and isinstance(tracker, list) and len(tracker) >= 1 and all(r["requester"] == "Sita Gurung" for r in tracker), (s, tracker))
s, tuth = call(requester, "GET", "/api/requests?hospital=" + urllib.parse.quote("TUTH, Maharajgunj"))
check("?hospital=<short label> resolves to the TUTH row (BR-5 queue)",
      s == 200 and len(tuth) >= 1 and all("Tribhuvan" in r["hospital"] for r in tuth), (s, tuth))
s, unrouted = call(requester, "GET", "/api/requests?unrouted=true")
check("?unrouted=true -> the admin's not-routed bucket",
      s == 200 and all(r["hospital"] == "—" for r in unrouted), (s, unrouted))
s, allreq = call(admin, "GET", "/api/requests")
check("no filter as an admin -> every request", s == 200 and len(allreq) >= 4, (s, len(allreq) if isinstance(allreq, list) else allreq))
s, donor_board = call(donor, "GET", "/api/requests")
# Arun is B+: his board may only contain requests a B+ donor can serve (BR-1) —
# B+ accepts B+/B-/O+/O- — so an A+ or AB+ request appearing here would be a bug.
BPLUS_SERVABLE = {"B+", "B-", "O+", "O-"}
check("no filter as a DONOR -> their own BR-1-filtered board (never the firehose)",
      s == 200 and isinstance(donor_board, list) and len(donor_board) >= 1
      and all(r["blood"] in BPLUS_SERVABLE for r in donor_board), (s, donor_board))
s, onereq = call(requester, "GET", "/api/requests/BB-8F3K2M")
check("GET /api/requests/{code} -> Matched + 2-line timeline",
      s == 200 and onereq["status"] == "Matched" and len(onereq["timeline"]) == 2, (s, onereq))
s, missing = call(requester, "GET", "/api/requests/BB-ZZZZZZ")
check("unknown request code -> 404 not-found", s == 404 and missing.get("error") == "not-found", (s, missing))

s, member_req = call(requester, "POST", "/api/requests", {
    "blood": "B+", "units": 2, "urgency": "Normal", "neededBy": "2026-10-05", "hospital": "Bir Hospital",
    "district": "Kathmandu", "notes": NOTE, "requester": "Someone Else"})
check("a member's request takes a BB- code and its requester from the SESSION",
      s == 201 and str(member_req.get("id", "")).startswith("BB-") and member_req.get("requester") == "Sita Gurung",
      (s, member_req))
check("the client-supplied requester/compatOk/timeline are ignored",
      member_req.get("requester") != "Someone Else" and member_req.get("timeline", [{}])[0].get("what", "").endswith("notified"), member_req)
created["member"] = member_req.get("id")

s, nodate = call(requester, "POST", "/api/requests", {"blood": "B+", "units": 1, "neededBy": "", "district": "Kathmandu"})
check("blank needed-by -> 409 with the rule's message", s == 409 and "needed-by" in nodate.get("message", ""), (s, nodate))
s, past = call(requester, "POST", "/api/requests", {"blood": "B+", "units": 1, "neededBy": "2020-01-01", "district": "Kathmandu"})
check("past needed-by -> 409", s == 409 and "today or later" in past.get("message", ""), (s, past))
s, jump = call(admin, "PATCH", "/api/requests/" + str(created["guest"]), {"status": "Fulfilled"})
check("BR-2: Pending cannot jump straight to Fulfilled (409)", s == 409 and "cannot become" in jump.get("message", ""), (s, jump))
s, again = call(donor, "PATCH", "/api/requests/BB-8F3K2M", {"status": "Matched", "donor": "Someone Else"})
check("BR-3: an already-matched request refuses a second donor (409)", s == 409 and "already been accepted" in again.get("message", ""), (s, again))
s, incompat = call(opener_for("dipesh.l@example.com", role="donor"), "PATCH", "/api/requests/BB-2QX9P1", {"status": "Matched"})
check("BR-1 over HTTP: an A+ donor cannot take a B+ request (409)", s == 409 and "cannot donate to" in incompat.get("message", ""), (s, incompat))
s, forward = call(requester, "PATCH", "/api/requests/BB-5MN8VX", {"status": "Accepted", "hospital": "NMC"})
check("a requester cannot route a request for the admin (409)", s == 409 and "administrator" in forward.get("message", ""), (s, forward))
s, forwarded = call(admin, "PATCH", "/api/requests/BB-5MN8VX", {"status": "Accepted", "hospital": "NMC"})
check("admin forward -> 200, hospital assigned, still Pending (routing != matching)",
      s == 200 and "Nepal Medical College" in forwarded.get("hospital", "") and forwarded.get("status") == "Pending", (s, forwarded))
s, cancelled = call(admin, "PATCH", "/api/requests/" + str(created["guest"]), {"status": "Cancelled"})
check("PATCH {status: Cancelled} as admin -> 200 Cancelled", s == 200 and cancelled["status"] == "Cancelled", (s, cancelled))
s, cancel_donor = call(donor, "PATCH", "/api/requests/" + str(created["member"]), {"status": "Cancelled"})
check("a donor cannot cancel someone else's request (409)", s == 409 and "requester" in cancel_donor.get("message", ""), (s, cancel_donor))
s, cancel_owner = call(requester, "PATCH", "/api/requests/" + str(created["member"]), {"status": "Cancelled"})
check("the owning requester CAN cancel their own request", s == 200 and cancel_owner["status"] == "Cancelled", (s, cancel_owner))

# ---------------- hospitals / inventory (BR-5) ----------------
s, bir_me = call(hospital, "GET", "/api/auth/me")
bir_id = bir_me.get("hospitalId")
check("a hospital account resolves to its staffed hospital (BR-5 link)", s == 200 and bir_id is not None, (s, bir_me))
s, board = call(hospital, "GET", "/api/hospitals/inventory")
check("GET /api/hospitals/inventory (hospital session) -> 8-group map", s == 200 and isinstance(board, dict) and len(board) == 8, (s, board))
check("board keyed by group label (the page's Object.keys contract)", isinstance(board, dict) and "B+" in board and "AB-" in board, list(board)[:4] if isinstance(board, dict) else board)
s, cross = call(hospital, "PUT", "/api/hospitals/inventory/B%2B?hospitalId=" + str(bir_id), {"units": 99})
check("the session's own hospital is used (write succeeds for the staff account)", s == 200 and cross.get("units") == 99, (s, cross))
s, restored = call(hospital, "PUT", "/api/hospitals/inventory/B%2B", {"units": 12})
check("stock restored to the seeded 12", s == 200 and restored.get("units") == 12, (s, restored))
s, admin_board = call(admin, "GET", "/api/hospitals/%s/inventory" % bir_id)
check("an admin CAN read a named hospital's board (Appendix B scoped form)", s == 200 and len(admin_board) == 8, (s, admin_board))
s, badgroup = call(hospital, "PUT", "/api/hospitals/inventory/XX", {"units": 1})
check("bad blood-group label -> 400 with the parser's message", s == 400, (s, badgroup))

# ---------------- admin / profile / photo ----------------
s, admin_users = call(admin, "GET", "/api/admin/users?role=Donor&q=Arun")
check("GET /api/admin/users?role=&q= -> one row with a STRING id",
      s == 200 and len(admin_users) == 1 and isinstance(admin_users[0]["id"], str), (s, admin_users))
s, suspended = call(admin, "PATCH", "/api/admin/users/" + str(admin_users[0]["id"]), {"status": "Suspended"})
check("PATCH /api/admin/users/{id} -> Suspended", s == 200 and suspended.get("status") == "Suspended", (s, suspended))
s, blocked_login = call(anon, "POST", "/api/auth/login", {"email": "arun.s@example.com", "password": DEMO_PASSWORD, "role": "donor"})
check("a suspended account cannot start a NEW session (409)", s == 409 and "suspended" in blocked_login.get("message", "").lower(), (s, blocked_login))
s, still_in = call(donor, "GET", "/api/auth/me")
check("...but an already-open session keeps working until it logs out (known limitation)",
      s == 200, (s, still_in))
call(admin, "PATCH", "/api/admin/users/" + str(admin_users[0]["id"]), {"status": "Active"})
s, admin_me = call(admin, "GET", "/api/auth/me")
s, self_suspend = call(admin, "PATCH", "/api/admin/users/" + str(admin_me.get("id")), {"status": "Suspended"})
check("an admin cannot suspend their own account (409)", s == 409 and "your own account" in self_suspend.get("message", ""), (s, self_suspend))
s, prof = call(donor, "GET", "/api/profile/donor")
check("GET /api/profile/donor -> the signed-in donor's editor", s == 200 and prof.get("fname") == "Arun" and "photo" in prof, (s, prof))
s, wrong_role_profile = call(requester, "GET", "/api/profile/donor")
check("the profile path role must match the account (409, not a half-filled donor form)",
      s == 409 and "account" in wrong_role_profile.get("message", ""), (s, wrong_role_profile))
s, photo = call(donor, "GET", "/api/donors/" + str(donor_id) + "/photo")
check("donor photo endpoint answers image bytes or a clean 404", s in (200, 404), s)
s, notfound = call(anon, "GET", "/api/nope")
check("unknown path -> 404 (not a 500)", s == 404, s)

# ---------------- register → log in (the B7 flow) ----------------
email = "api-check+%d@example.com" % __import__("time").time_ns()
s, reg = call(anon, "POST", "/api/auth/register", {"name": "API Check", "email": email, "password": "ApiCheck#2026",
                                                   "role": "donor", "phone": "9800000000", "district": "Kathmandu",
                                                   "blood": "B+", "area": "Baneshwor, Kathmandu"})
check("register a donor -> 201 with a donor id", s == 201 and reg.get("donorId") is not None, (s, reg))
s, dup = call(anon, "POST", "/api/auth/register", {"name": "API Check", "email": email, "password": "ApiCheck#2026",
                                                   "role": "donor", "blood": "B+"})
check("registering that email again -> 409", s == 409, (s, dup))
fresh = opener_for(email, password="ApiCheck#2026", role="donor")
s, fresh_me = call(fresh, "GET", "/api/auth/me")
check("the freshly registered account can sign in and use the API", s == 200 and fresh_me.get("email") == email, (s, fresh_me))
# §2.5 on the run's OWN donor, so the seeded donors are never given a photo:
s, upload = call(fresh, "POST", "/api/donors/%s/photo" % reg.get("donorId"),
                 {"dataUrl": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/AF+7VwAAAAASUVORK5CYII="})
check("§2.5 photo upload to the DB (Base64 → BLOB) -> 200", s == 200, (s, upload))
s, back = call(fresh, "GET", "/api/donors/%s/photo" % reg.get("donorId"))
check("§2.5 photo retrieval returns the stored bytes", s == 200 and isinstance(back, str), (s, back))
no_photo_probe = [d for d in registry if str(d["id"]) != str(reg.get("donorId")) and not d.get("hasPhoto")]
if no_photo_probe:
    s, no_photo = call(fresh, "GET", "/api/donors/%s/photo" % no_photo_probe[0]["id"])
    check("a donor without a photo -> 404, not an empty image", s == 404, (s, no_photo))
s, logged_out = call(fresh, "POST", "/api/auth/logout")
check("POST /api/auth/logout -> 200 ack", s == 200 and logged_out.get("ok") is True, (s, logged_out))
s, after_logout = call(fresh, "GET", "/api/auth/me")
check("after logout the session is really gone (401)", s == 401, (s, after_logout))
created["email"] = email

# ------------- the Donor ↔ Hospital M:N and the staffed-hospital link -------------
# A SEEDED donor (the one the login captured) has affiliations; a donor registered
# a second ago deliberately has none, because the seeded rule links a donor to the
# partner hospitals in THEIR district and the seeder only runs at boot. Checking
# both is what says the relationship was populated by a rule rather than by
# "everybody is affiliated with everything".
# A named SEEDED donor (Arun, Kathmandu) rather than `cards[0]`/`registry[0]`: the
# registry includes donors this script itself registers, and those deliberately have
# no affiliation — and a leftover `api-check` donor from an unclean previous run can
# sort ahead of the seeded ones (its name starts with "API"). Naming the fixture is
# what makes the check about the RULE rather than about row order.
seeded_donor_id = next((d["id"] for d in registry if d.get("name") == "Arun Shrestha"), cards[0]["id"])
s, near = call(anon, "GET", "/api/donors/%s/hospitals" % seeded_donor_id)
check("GET /api/donors/{id}/hospitals -> the M:N affiliation list (200)",
      s == 200 and isinstance(near, list) and len(near) > 0, (s, near))
check("every affiliation carries the id and the labels the UI needs",
      all(isinstance(h.get("id"), str) and h.get("label") and h.get("shortName") and h.get("district")
          for h in (near or [])), near)
check("the seeded rule holds — a donor is registered only with hospitals in one district",
      len({h.get("district") for h in (near or [])}) == 1, near)
check("the donor is affiliated with the partner hospitals of their OWN district",
      {h.get("district") for h in (near or [])} == {"Kathmandu"}, near)
s, no_near = call(anon, "GET", "/api/donors/%s/hospitals" % reg.get("donorId"))
check("a donor registered after boot legitimately has no affiliation (200 + empty, not 404)",
      s == 200 and no_near == [], (s, no_near))

nurse_email = "api-check+nurse%d@example.com" % __import__("time").time_ns()
s, nurse = call(anon, "POST", "/api/auth/register",
                {"name": "API Check Nurse", "email": nurse_email, "password": "ApiCheck#2026",
                 "role": "hospital", "phone": "9800000001", "district": "Kathmandu", "hospital": "TUTH"})
check("hospital staff register with a NAME -> staffedHospital resolved (BR-5 is active for it)",
      s == 201 and nurse.get("hospitalId") is not None and "Tribhuvan" in str(nurse.get("hospitalLabel")), (s, nurse))
s, unknown_hospital = call(anon, "POST", "/api/auth/register",
                           {"name": "API Check Nurse 2", "email": "api-check+nurse2%d@example.com" % __import__("time").time_ns(),
                            "password": "ApiCheck#2026", "role": "hospital", "district": "Kathmandu",
                            "hospital": "Nowhere General Hospital"})
check("a hospital name matching no partner hospital still registers, unlinked (warned, not refused)",
      s == 201 and unknown_hospital.get("hospitalId") is None, (s, unknown_hospital))
created["nurse"] = nurse_email

# ------------- proposal Appendix B: the hospital-scoped "local API" routes -------------
# The five-hospital paths the proposal names. Each is a thin delegate onto a service
# already exercised above, so what is worth asserting here is that the PATH exists,
# that BR-5 travels with a hospital id taken from the URL, and that an administrator
# may still name any hospital.
s, nearby = call(hospital, "GET", f"/api/hospitals/{bir_id}/donors/nearby?blood=B%2B")
check("GET /api/hospitals/{id}/donors/nearby -> donors in that hospital's district",
      s == 200 and isinstance(nearby, list) and len(nearby) >= 1, (s, nearby))
s, other_guest_status = call(hospital, "GET", f"/api/hospitals/{bir_id}/requests/{created['guest']}/status")
check("a request routed to ANOTHER hospital -> 404 on Bir's status route (BR-5)",
      s == 404, (s, other_guest_status))   # created[guest] was submitted to Patan
s, bir_status = call(hospital, "GET", f"/api/hospitals/{bir_id}/requests/BB-8F3K2M/status")
check("the seeded Bir request reads through its own hospital's status route",
      s == 200 and bir_status.get("id") == "BB-8F3K2M", (s, bir_status))
s, other_status = call(hospital, "GET", f"/api/hospitals/{bir_id}/requests/BB-7HD4LN/status")
check("...but a request in ANOTHER hospital's queue -> 404 (BR-5)", s == 404, (s, other_status))
s, scoped_put = call(hospital, "PUT", f"/api/hospitals/{bir_id}/inventory/B%2B", {"units": 12})
check("PUT /api/hospitals/{id}/inventory/{group} (Appendix B) -> 200", s == 200 and scoped_put.get("units") == 12, (s, scoped_put))
# A second, DIFFERENT hospital id taken from the seeded affiliations — never a
# hardcoded row id (the B3 lesson: TiDB's identity ids have gaps).
other_hospital_id = next((h["id"] for h in near if str(h["id"]) != str(bir_id)), None)
if other_hospital_id:
    s, cross_put = call(hospital, "PUT", f"/api/hospitals/{other_hospital_id}/inventory/B%2B", {"units": 1})
    check("...but a staff account naming a DIFFERENT hospital -> 409 (BR-5)",
          s == 409 and "own hospital" in cross_put.get("message", ""), (s, cross_put))
    s, admin_any = call(admin, "GET", f"/api/hospitals/{other_hospital_id}/inventory")
    check("an administrator may still address any hospital in the path (Appendix B)", s == 200, (s, admin_any))
s, post_inv = call(hospital, "POST", "/api/hospitals/inventory?bloodGroup=B%2B", {"units": 12})
check("POST /api/hospitals/inventory (Appendix B update shape) -> 200", s == 200 and post_inv.get("units") == 12, (s, post_inv))
s, deactivated = call(admin, "POST", f"/api/admin/users/{admin_users[0]['id']}/deactivate")
check("POST /api/admin/users/{id}/deactivate (Appendix B) -> Suspended",
      s == 200 and deactivated.get("status") == "Suspended", (s, deactivated))
call(admin, "PATCH", f"/api/admin/users/{admin_users[0]['id']}", {"status": "Active"})

# ------------- server-side pagination (CPJ119 §3 / proposal §1.4) -------------
# Additive: asking for a page returns an envelope, and NOT asking for one returns the
# same array every existing page already consumes — the second half of each pair is
# the regression guard for the first.
s, users_paged = call(admin, "GET", "/api/admin/users?page=0&size=3")
check("GET /api/admin/users?page=&size= -> a paged envelope (items/total/totalPages/hasNext)",
      s == 200 and isinstance(users_paged, dict) and len(users_paged["items"]) == 3
      and users_paged["size"] == 3 and users_paged["hasNext"] is True, (s, users_paged))
s, users_all = call(admin, "GET", "/api/admin/users")
check("...and WITHOUT page the array shape is unchanged (additive, no breakage)",
      s == 200 and isinstance(users_all, list) and len(users_all) == users_paged["total"],
      (s, len(users_all) if isinstance(users_all, list) else users_all))
s, donors_paged = call(requester, "GET", "/api/donors/search?page=0&size=5")
check("GET /api/donors/search?page=&size= -> paged envelope over the search results",
      s == 200 and isinstance(donors_paged, dict) and len(donors_paged["items"]) == 5
      and donors_paged["total"] >= 14, (s, donors_paged))
s, reqs_paged = call(admin, "GET", "/api/requests?page=0&size=2")
check("GET /api/requests?page=&size= -> paged envelope",
      s == 200 and isinstance(reqs_paged, dict) and len(reqs_paged["items"]) == 2, (s, reqs_paged))

print()
print("HTTP exercise: %d passed, %d failed" % (len(passed), len(failed)))
for f in failed:
    print("  FAILED:", f)

print()
# Run these ONE statement at a time, or with the client's `--force`: a mysql client
# stops at the first error in a multi-statement -e, which silently skips the rest.
# The unrouted label is written as UNHEX('E28094') because a literal em dash in a
# Windows console argument is mangled to '?' before the client ever sees it — and
# because CONVERT(CHAR(0x2014) USING utf8mb4) is NOT the em dash: TiDB takes only
# the low byte (0x2014 & 0xFF = 0x14), so that spelling silently stores the wrong
# character and the "not routed yet" filter stops matching the row.
print("cleanup SQL (run against the same schema, one statement at a time):")
print("  DELETE FROM email_notifications WHERE request_id IN (SELECT request_id FROM blood_requests WHERE notes='%s');" % NOTE)
print("  DELETE FROM request_timeline   WHERE request_id IN (SELECT request_id FROM blood_requests WHERE notes='%s');" % NOTE)
print("  DELETE FROM blood_requests     WHERE notes='%s';" % NOTE)
print("  DELETE FROM request_timeline   WHERE request_id = (SELECT request_id FROM blood_requests WHERE public_code='BB-5MN8VX') AND seq > 0;")
print("  UPDATE blood_requests SET hospital_id=NULL, hospital_label=CONVERT(UNHEX('E28094') USING utf8mb4) WHERE public_code='BB-5MN8VX';")
print("  UPDATE users SET status='ACTIVE' WHERE email='arun.s@example.com';")
print("  UPDATE donors SET photo=NULL WHERE photo IS NOT NULL AND user_id NOT IN (SELECT user_id FROM users WHERE email LIKE 'api-check+%%');  -- seeded donors stay photo-less")
print("  DELETE FROM email_notifications WHERE recipient_user_id IN (SELECT user_id FROM users WHERE email LIKE 'api-check+%%');")
print("  DELETE FROM donors WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE 'api-check+%%');")
print("  DELETE FROM users WHERE email LIKE 'api-check+%%';")
print("created during the run: guest", created.get("guest"), "| member", created.get("member"),
      "| accounts", created.get("email"), ("+ " + str(created.get("nurse")) if created.get("nurse") else ""))
