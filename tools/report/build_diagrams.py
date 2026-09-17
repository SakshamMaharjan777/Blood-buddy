# -*- coding: utf-8 -*-
"""
Draws the report's design diagrams and its project-structure figure, so the
hand-in copy carries no bracket placeholder.

Style matches the diagrams already in the report (the ones extracted from the
proposal DOCX): white page, pastel fills, a darker stroke per hue, Arial labels.
Drawing is Pillow-only — no Graphviz, no matplotlib, no browser — so the figures
are reproducible from the repo with the one dependency the report tools already
use (build_montages.py uses the same).

Type sizes are chosen against the printed page, not the pixel grid: each figure
is placed at 16 cm in the report, so a 19 px label on a 1460 px canvas lands at
roughly 6 pt — the same order as the proposal's own diagrams. Anything smaller
would be unreadable in print.

Run:  python tools/report/build_diagrams.py
Each figure prints its size + "ink" coverage, and the script reports any label
that does not fit its box or any two boxes that overlap — a blank, clipped or
collided figure is the failure mode these checks exist to catch.
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "assets")
FDIR = "C:/Windows/Fonts"

BG = (255, 255, 255)
INK = (40, 40, 40)
MUTED = (100, 100, 100)
LINE = (125, 125, 125)

# fill / stroke pairs, sampled from the proposal's diagrams
PAL = {
    "blue":   ("DCEEFA", "8FBBDD"),
    "green":  ("D6F5EC", "77C3AA"),
    "purple": ("EDE9FD", "B4A8EE"),
    "salmon": ("FDEAE4", "E8A38D"),
    "amber":  ("FFF6DA", "DCC06C"),
    "grey":   ("F4F4F4", "B8B8B8"),
}


def _font(size, bold=False, mono=False):
    if mono:
        name = "consolab" if bold else "consola"
    else:
        name = "arialbd" if bold else "arial"
    return ImageFont.truetype(os.path.join(FDIR, name + ".ttf"), size)


class Canvas(object):
    def __init__(self, w, h, size=18):
        self.im = Image.new("RGB", (w, h), BG)
        self.d = ImageDraw.Draw(self.im)
        self.w, self.h = w, h
        self.size = size
        self.rects = []
        self.warn = []
        self._fonts = {}

    # ---------------------------------------------------------------- text
    def font(self, size=None, bold=False, mono=False):
        key = (size or self.size, bold, mono)
        if key not in self._fonts:
            self._fonts[key] = _font(key[0], bold, mono)
        return self._fonts[key]

    def wrap(self, text, f, maxw):
        out = []
        for hard in text.split("\n"):
            words, line = hard.split(), ""
            for word in words:
                trial = (line + " " + word).strip()
                if line and self.d.textlength(trial, font=f) > maxw:
                    out.append(line)
                    line = word
                else:
                    line = trial
            out.append(line)
        return out

    def label(self, xy, text, size=None, bold=False, mono=False, fill=INK,
              anchor="mm", halo=False, maxw=None, spacing=5):
        f = self.font(size, bold, mono)
        lines = self.wrap(text, f, maxw) if maxw else text.split("\n")
        if halo:
            x0, y0, x1, y1 = self.d.multiline_textbbox(
                xy, "\n".join(lines), font=f, anchor=anchor, align="center", spacing=spacing)
            self.d.rectangle((x0 - 5, y0 - 3, x1 + 5, y1 + 3), fill=BG)
        self.d.multiline_text(xy, "\n".join(lines), font=f, fill=fill,
                              anchor=anchor, align="center", spacing=spacing)
        width = max(self.d.textlength(l, font=f) for l in lines)
        if maxw and width > maxw + 1:
            self.warn.append("label wider than its bound: %r" % text[:40])
        return lines

    # --------------------------------------------------------------- shapes
    def box(self, rect, kind="blue", text="", size=None, bold=False, mono=False,
            radius=10, pad=12, name="", fill=None, outline=None):
        x0, y0, x1, y1 = rect
        f = self.font(size, bold, mono)
        if fill is None and outline is None:
            fill, outline = PAL[kind]
        self.d.rounded_rectangle(rect, radius=radius, fill="#"+fill,
                                 outline="#"+(outline or "B8B8B8"), width=2)
        self.rects.append((name or text[:22], rect))
        if text:
            lines = self.wrap(text, f, (x1 - x0) - 2 * pad)
            need = len(lines) * (f.size + 5)
            if need > (y1 - y0) - 2 * pad:
                self.warn.append("%s: %d label lines need %.0fpx, box has %dpx"
                                 % (name or text[:22], len(lines), need, (y1 - y0) - 2 * pad))
            self.label(((x0 + x1) / 2.0, (y0 + y1) / 2.0), text, size, bold, mono,
                       maxw=(x1 - x0) - 2 * pad)
        return rect

    def ellipse(self, cx, cy, rw, rh, kind="blue", text="", size=None, name="", bold=False):
        rect = (cx - rw, cy - rh, cx + rw, cy + rh)
        fill, outline = PAL[kind]
        self.d.ellipse(rect, fill="#" + fill, outline="#" + outline, width=2)
        self.rects.append((name or text[:22], rect))
        if text:
            # an inscribed label must stay well inside the curve: wrap to 80 % of
            # the full width, and only use the middle band vertically
            f = self.font(size)
            lines = self.wrap(text, f, int(1.6 * rw) - 8)
            need = len(lines) * (f.size + 5)
            if need > 2 * rh - 14:
                self.warn.append("%s: %d label lines need %.0fpx, ellipse has %dpx"
                                 % (name or text[:22], len(lines), need, 2 * rh - 14))
            self.label((cx, cy), text, size, bold, maxw=int(1.6 * rw) - 8)
        return rect

    def store(self, rect, tag, text, size=17):
        """Gane-Sarson data store: rectangle with a tag cell on the left."""
        x0, y0, x1, y1 = rect
        self.d.rectangle(rect, fill="#F4F4F4", outline="#8C8C8C", width=2)
        self.d.line((x0 + 54, y0, x0 + 54, y1), fill="#8C8C8C", width=2)
        self.rects.append((text[:22], rect))
        self.label((x0 + 27, (y0 + y1) / 2.0), tag, 16, True)
        self.label((x0 + 54 + (x1 - x0 - 54) / 2.0, (y0 + y1) / 2.0), text, size)

    # --------------------------------------------------------------- arrows
    def _seg(self, a, b, color, width, dash):
        if not dash:
            self.d.line((a, b), fill=color, width=width)
            return
        x0, y0 = a
        x1, y1 = b
        total = ((x1 - x0) ** 2 + (y1 - y0) ** 2) ** 0.5 or 1
        step, t = 11.0, 0.0
        while t < total:
            t2 = min(t + step * 0.6, total)
            self.d.line((x0 + (x1 - x0) * t / total, y0 + (y1 - y0) * t / total,
                         x0 + (x1 - x0) * t2 / total, y0 + (y1 - y0) * t2 / total),
                        fill=color, width=width)
            t += step

    def head(self, tip, frm, color, size=11):
        x0, y0 = tip
        dx, dy = x0 - frm[0], y0 - frm[1]
        n = (dx * dx + dy * dy) ** 0.5 or 1
        ux, uy = dx / n, dy / n
        px, py = -uy, ux
        self.d.polygon([(x0, y0),
                        (x0 - ux * size + px * size * 0.42, y0 - uy * size + py * size * 0.42),
                        (x0 - ux * size - px * size * 0.42, y0 - uy * size - py * size * 0.42)],
                       fill=color)

    def flow(self, pts, color=LINE, width=2, dash=False, heads=1, size=12,
             badge=None, badge_fill=(70, 70, 70)):
        for a, b in zip(pts, pts[1:]):
            self._seg(a, b, color, width, dash)
        if heads >= 1:
            self.head(pts[-1], pts[-2], color, size)
        if heads == 2:
            self.head(pts[0], pts[1], color, size)
        if badge is not None:
            _, a, b = max(((b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2, a, b)
                          for a, b in zip(pts, pts[1:]))
            mx, my = (a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0
            r = 15
            self.d.ellipse((mx - r, my - r, mx + r, my + r), fill=badge_fill)
            self.label((mx, my), badge, 14, True, fill=(255, 255, 255))

    def actor(self, x, y, name):
        r = 15
        self.d.ellipse((x - r, y - 52, x + r, y - 52 + 2 * r), fill=BG, outline=INK, width=2)
        self.d.line((x, y - 22, x, y + 16), fill=INK, width=2)          # body
        self.d.line((x - 25, y - 6, x + 25, y - 6), fill=INK, width=2)  # arms
        self.d.line((x, y + 16, x - 20, y + 48), fill=INK, width=2)     # legs
        self.d.line((x, y + 16, x + 20, y + 48), fill=INK, width=2)
        self.label((x, y + 70), name, 18, True)

    def note(self, xy, text, size=17, maxw=None, fill=MUTED):
        self.label(xy, text, size, fill=fill, maxw=maxw)

    # ----------------------------------------------------------- self-checks
    def check_overlaps(self):
        for i, (na, a) in enumerate(self.rects):
            for nb, b in self.rects[i + 1:]:
                if a[0] < b[2] - 2 and b[0] < a[2] - 2 and a[1] < b[3] - 2 and b[1] < a[3] - 2:
                    self.warn.append("overlap: %r and %r" % (na[:26], nb[:26]))

    def save(self, name):
        self.check_overlaps()
        path = os.path.join(OUT, name)
        self.im.save(path, "PNG")
        for w in self.warn:
            print("  ! %s" % w)
        g = self.im.convert("L")
        hist = g.histogram()
        ink = (g.width * g.height - sum(hist[240:])) / float(g.width * g.height)
        print("  %-26s %5dx%-5d  ink %5.1f%%%s" % (name, self.im.width, self.im.height,
                                                   ink * 100, "   !!" if self.warn else ""))
        return ink


# =============================================================== Figure 1-1
def lifecycle():
    c = Canvas(1240, 800)
    c.box((10, 36, 336, 146), "amber",
          "Guest: public emergency form — no account, EM- code", 17, name="guest")
    c.flow([(180, 146), (180, 172), (288, 172), (288, 208)], dash=True)

    xs = [10, 218, 426, 634, 842, 1050]
    w = 140
    y0, y1 = 208, 328
    cells = [
        ("Requester\nsubmits\n(BB-XXXXXX)", "blue"),
        ("PENDING\nmoderation\nqueue", "blue"),
        ("Admin\nforwards to a\nhospital", "purple"),
        ("Hospital\nqueue\n(BR-5 scoped)", "purple"),
        ("Donor accepts\nMATCHED\n(BR-1)", "green"),
        ("Hospital fulfils\nFULFILLED\nstock −(BR-4)", "green"),
    ]
    for x, (t, k) in zip(xs, cells):
        c.box((x, y0, x + w, y1), k, t, 16, name=t.split("\n")[0])
    for i in range(5):
        c.flow([(xs[i] + w, (y0 + y1) // 2), (xs[i + 1], (y0 + y1) // 2)], badge=str(i + 1))

    c.box((330, 404, 910, 486), "salmon",
          "CANCELLED / REJECTED — terminal: the requester cancels while open, "
          "the admin rejects it in moderation", 16, name="terminal")
    c.flow([(288, y1), (288, 372), (430, 372), (430, 404)], dash=True)
    c.flow([(912, y1), (912, 372), (810, 372), (810, 404)], dash=True)
    c.note((620, 380), "cancel / reject", 15)

    legend = [
        "POST /api/requests → created PENDING, donors alerted",
        "moderation: admin forwards to a hospital, or rejects",
        "it enters that hospital's queue (its own only, BR-5)",
        "compatible donors see it; accept sets the donor (BR-1)",
        "fulfil closes it, stock −units for that group (BR-4)",
        "Cancelled / Rejected: terminal, and no stock change",
    ]
    for i, t in enumerate(legend):
        col, row = divmod(i, 3)
        x = 30 + col * 620
        y = 560 + row * 56
        c.d.ellipse((x, y - 13, x + 26, y + 13), fill=(70, 70, 70))
        c.label((x + 13, y), str(i + 1), 14, True, fill=(255, 255, 255))
        c.label((x + 40, y), t, 16, anchor="lm")
    c.note((620, 748), "Every transition appends a timeline entry (request_timeline), visible to the requester "
                       "in My Requests;\nillegal transitions are refused by the API (BR-2 … BR-8).", 16, maxw=1180)
    return c.save("fig1_1_lifecycle.png")


# =============================================================== Figure 3-2
def use_case():
    c = Canvas(1470, 1370)
    c.d.rounded_rectangle((240, 40, 1140, 1250), radius=16, outline="#9AA7B4", width=2)
    c.label((690, 86), "BloodBuddy", 20, True)
    c.label((690, 116), "Register / log in — every role, one shared use case", 17, fill=MUTED)

    donor = ["Manage own profile", "Toggle availability", "View compatible open requests",
             "Accept or decline a request", "View donation history"]
    requester = ["Search donors (filters, paging)", "Submit a blood request",
                 "Submit an emergency request", "Track my requests", "Cancel an open request"]
    guest = ["Search donors", "Submit an emergency request", "Look up status by request ID"]
    hospital = ["Manage blood inventory", "Review the request queue", "Accept / fulfil a request",
                "Adjust stock after fulfilment", "View affiliated donors"]
    admin = ["Manage users (suspend / activate)", "Moderate & forward requests",
             "View platform analytics", "Reset demo data", "Manage partner hospitals"]

    def block(items, kind, cols, top, xs, step=100):
        pos = []
        for i in range(len(items)):
            r, col = divmod(i, cols)
            pos.append((xs[col], top + r * step))
        for (cx, cy), t in zip(pos, items):
            c.ellipse(cx, cy, 120, 38, kind, t, 18)
        return pos

    p_donor = block(donor, "blue", 2, 210, (420, 700))
    p_req = block(requester, "green", 2, 540, (420, 700))
    p_guest = block(guest, "amber", 2, 920, (420, 700))
    p_hosp = block(hospital, "purple", 1, 220, (980,), step=112)
    p_admin = block(admin, "salmon", 1, 780, (980,), step=100)

    actors = [(100, 320, "Donor", p_donor), (100, 640, "Requester", p_req),
              (100, 980, "Guest", p_guest), (1370, 440, "Hospital Staff", p_hosp),
              (1370, 990, "Administrator", p_admin)]
    for x, y, nm, targets in actors:
        c.actor(x, y, nm)
        edge = x + 30 if x < 200 else x - 30
        for tx, ty in targets:
            c.flow([(edge, y - 8), (tx + (120 if x < 200 else -120), ty)], width=1, size=8)
    c.note((690, 1300), "Every use case is reached through the REST API under Spring Security: an anonymous call "
                        "answers 401 and a wrong role 403, whatever the page guard allows (Section 3.9).", 16,
           maxw=1330)
    return c.save("fig3_2_use_case.png")


# =============================================================== Figure 3-5
def dfd():
    c = Canvas(1520, 1520)
    ents = [("Donor", 150), ("Requester", 330), ("Guest", 510), ("Hospital Staff", 690),
            ("Administrator", 870)]
    procs = ["1  User & Role\nManagement", "2  Donor Registry\n& Search", "3  Blood Request\nManagement",
             "4  Inventory\nManagement", "5  Notification\nManagement", "6  Administration\n& Moderation"]
    pys = [150, 310, 470, 630, 790, 950]
    for nm, y in ents:
        c.box((40, y - 40, 270, y + 40), "grey", nm, 18, name=nm)
    for t, y in zip(procs, pys):
        c.box((600, y - 52, 940, y + 52), "blue", t, 18, name=t.split("\n")[0])
    c.box((1160, 1060, 1440, 1140), "grey", "SMTP mail server", 18, name="smtp")
    stores = [("D1", "users"), ("D2", "donors"), ("D3", "blood_requests\n+ request_timeline"),
              ("D4", "blood_inventory"), ("D5", "email_notifications\n+ request_declines")]
    for (tag, t), y in zip(stores, [150, 310, 470, 630, 790]):
        c.store((1160, y - 42, 1440, y + 42), tag, t, 17)

    c.flow([(270, 150), (600, 150)], badge="1")
    c.flow([(270, 168), (340, 168), (340, 310), (600, 310)], badge="2")
    c.flow([(270, 340), (600, 340)], badge="3", heads=2)
    c.flow([(270, 318), (450, 318), (450, 470), (600, 470)], badge="4")
    c.flow([(270, 510), (600, 510)], badge="5", heads=2)
    c.flow([(270, 690), (520, 690), (520, 440), (600, 440)], badge="6", heads=2)
    c.flow([(270, 712), (560, 712), (560, 625), (600, 625)], badge="7", heads=2)
    c.flow([(270, 870), (600, 870)], badge="8")
    c.flow([(940, 470), (1160, 470)], badge="9")
    c.flow([(940, 630), (1160, 630)], badge="10", heads=2)
    c.flow([(940, 140), (1160, 140)], badge="11", heads=2)
    c.flow([(940, 310), (1160, 310)], badge="12", heads=2)
    c.flow([(940, 790), (1160, 790)], badge="13")
    c.flow([(600, 500), (548, 500), (548, 790), (600, 790)], badge="14")
    c.flow([(600, 920), (496, 920), (496, 530), (680, 530), (680, 522)], badge="15")
    c.flow([(940, 820), (1120, 820), (1120, 1100), (1160, 1100)], badge="16")
    c.flow([(940, 935), (1490, 935), (1490, 150), (1440, 150)], badge="17")
    c.flow([(940, 490), (985, 490), (985, 625), (940, 625)], badge="18")

    flows = [
        "1  Donor → user records: registration",
        "2  Donor → donor profile, availability",
        "3  Requester ⇄ search filters / results",
        "4  Requester → request details (BB-)",
        "5  Guest ⇄ emergency request / status",
        "6  Staff ⇄ accept, fulfil / matched state",
        "7  Staff ⇄ stock adjustment / levels",
        "8  Administrator → moderation actions",
        "9  request row + timeline entries",
        "10 stock levels read and written (BR-4)",
        "11 user rows: login read, register write",
        "12 donor rows, incl. the M:N affiliation",
        "13 delivery outcome: SENT or FAILED",
        "14 notify compatible donors, requester",
        "15 forward to a hospital, or reject",
        "16 email out through SMTP (subject+body)",
        "17 role / status change, hospital records",
        "18 decrement that group's stock (BR-4)",
    ]
    for i, t in enumerate(flows):
        col, row = divmod(i, 6)
        x = 40 + col * 500
        y = 1250 + row * 34
        c.label((x, y), t, 16, anchor="lm")
    c.note((760, 1480), "Level-1 DFD — processes 1-6, five data stores, every flow labelled with the data it "
                        "carries; the numbered badge on the line matches this legend.", 16, maxw=1400)
    return c.save("fig3_5_dfd.png")


# =============================================================== Figure 4-1
TREE = """D:/APJ_PROJECT/
├─ backend/                        Spring Boot 3.5 · Java 17 · Maven
│  ├─ src/main/java/com/bloodbuddy/
│  │  ├─ web/          AuthController  DonorController  HospitalController
│  │  │                 RequestController  InventoryController  AdminController
│  │  │                 ProfileController  ContactController  ApiExceptionHandler
│  │  ├─ service/      DonorService  RequestService  InventoryService  AuthService
│  │  │                 AdminService  ProfileService  HospitalResolver
│  │  │                 NotificationService  NotificationMailer  PhotoCodec
│  │  ├─ repository/   seven Spring Data JPA repositories
│  │  ├─ model/        User  Donor  Hospital  BloodInventory  BloodRequest
│  │  │                 EmailNotification  RequestDecline  + enums
│  │  ├─ dto/          request / response bodies (entities are never exposed)
│  │  └─ config/       SecurityConfig  CryptoConfig  DemoDataSeeder  ServiceSmokeRunner
│  ├─ src/main/resources/
│  │  ├─ application.properties      datasource, SMTP and seed flags from env
│  │  └─ static/                    generated copy of the frontend (sync script)
│  └─ pom.xml
├─ CODE/                           frontend: no framework, no build step
│  ├─ HTML/                        23 pages - public, auth, four role portals
│  └─ js/                          nav.js  bb-api.js  bb-validate.js  bb-paginate.js
│                                  bb-compat.js  bb-store.js  main.js
├─ CSS/                            landing.css  bb-ui.css  profile.css  + per-page
├─ tools/                          api_check.py  mock_api_server.py
│  │                               sync_frontend_to_backend.py
│  └─ report/                      build_report.py  bb_content.py  build_montages.py
│                                  build_diagrams.py  assets/ (figures + shots)
├─ resptest/                       qa-harness.html  responsive-check.html
│                                  real-backend-check.html  visual-forms.html
└─ docs/                           proposal  runbook  final report  postman/""".split("\n")


def structure():
    step = 34
    c = Canvas(1320, 76 + step * len(TREE) + 16)
    c.d.rounded_rectangle((14, 14, 1306, c.h - 14), radius=12, fill="#FFFFFF",
                          outline="#9AA7B4", width=2)
    c.label((40, 48), "D:/APJ_PROJECT — the repository as submitted  "
                      "(backend, frontend, tools, tests, docs)", 18, True, anchor="lm")
    f = c.font(17, mono=True)
    roots = ("backend/", "CODE/", "CSS/", "tools/", "resptest/", "docs/")
    for i, line in enumerate(TREE):
        col = (28, 58, 110) if any(r in line for r in roots) else INK
        c.d.text((40, 86 + i * step), line, font=f, fill=col)
        if c.d.textlength(line, font=f) > 1240:
            c.warn.append("structure line too wide: %r" % line[:50])
    return c.save("fig4_1_structure.png")


if __name__ == "__main__":
    print("drawing report figures...")
    lifecycle()
    use_case()
    dfd()
    structure()
    print("done — assets in tools/report/assets/")
