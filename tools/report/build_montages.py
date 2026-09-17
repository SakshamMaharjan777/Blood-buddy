# -*- coding: utf-8 -*-
"""
Composes the report's grouped screenshot figures from the individual captures in
tools/report/assets/shots/, and reports every source image's size and "ink"
coverage so a blank or error page cannot slip in unnoticed.

A figure in the report carries ONE image, but several placeholders ask for more
than one screen (two request forms, four admin panels, three harness reports).
Those are stacked/gridded here instead of restructured into new figure numbers.

Run:  python tools/report/build_montages.py
Captures come from the running app (see the session-#26 note in PROGRESS.md):
  node .tools/shots/capture.js
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "assets", "shots")
GAP = 14
BG = (255, 255, 255)


def load(name):
    p = os.path.join(SRC, name)
    if not os.path.exists(p):
        raise SystemExit("missing capture: " + p)
    return Image.open(p).convert("RGB")


def ink(im):
    """Fraction of pixels that are not (near) white - a blank page reads ~0."""
    g = im.convert("L").resize((im.width // 4 or 1, im.height // 4 or 1))
    total = g.width * g.height
    hist = g.histogram()
    return (total - sum(hist[240:])) / float(total)


def stack(images, out, crop_to=None):
    if crop_to:
        images = [im.crop((0, 0, im.width, min(crop_to, im.height))) for im in images]
    w = max(im.width for im in images)
    h = sum(im.height for im in images) + GAP * (len(images) - 1)
    canvas = Image.new("RGB", (w, h), BG)
    y = 0
    for im in images:
        canvas.paste(im, (0, y))
        y += im.height + GAP
    canvas.save(os.path.join(SRC, out), "PNG")
    return canvas


def grid(images, cols, out, cell_w=None):
    if cell_w:
        images = [im.resize((cell_w, int(im.height * cell_w / im.width)), Image.LANCZOS)
                  for im in images]
    rows = (len(images) + cols - 1) // cols
    cw = max(im.width for im in images)
    ch = max(im.height for im in images)
    canvas = Image.new("RGB", (cols * cw + GAP * (cols - 1), rows * ch + GAP * (rows - 1)), BG)
    for i, im in enumerate(images):
        r, c = divmod(i, cols)
        canvas.paste(im, (c * (cw + GAP), r * (ch + GAP)))
    canvas.save(os.path.join(SRC, out), "PNG")
    return canvas


def row(images, out, cell_w):
    scaled = [im.resize((cell_w, int(im.height * cell_w / im.width)), Image.LANCZOS) for im in images]
    h = max(im.height for im in scaled)
    canvas = Image.new("RGB", (cell_w * len(scaled) + GAP * (len(scaled) - 1), h), BG)
    x = 0
    for im in scaled:
        canvas.paste(im, (x, 0))
        x += im.width + GAP
    canvas.save(os.path.join(SRC, out), "PNG")
    return canvas


def main():
    print("%-28s %-11s %6s" % ("capture", "size", "ink"))
    for n in sorted(os.listdir(SRC)):
        if n.endswith(".png") and not n.startswith("fig4_3_request") \
                and not n.startswith("fig4_5_admin_panels") and not n.startswith("fig5_1_harness") \
                and not n.startswith("appF_auth_pair") and not n.startswith("appF_legal_row"):
            im = load(n)
            print("%-28s %-11s %5.1f%%" % (n, "%dx%d" % im.size, ink(im) * 100))

    # Figure 4-3: the two request forms (member form + public emergency form)
    stack([load("fig4_3a_request_form.png"), load("fig4_3b_emergency.png")], "fig4_3_request_forms.png")
    # Figure 4-5: the four administrative screens
    grid([load("fig4_5a_admin_dashboard.png"), load("fig4_5b_admin_requests.png"),
          load("fig4_5c_hospital_dashboard.png"), load("fig4_5d_requester_tracking.png")],
         2, "fig4_5_admin_panels.png", cell_w=1440)
    # Figure 5-1: three self-driving harness reports. Each capture was scrolled so
    # the verdict line sits ~220px down, so a 640px band holds it.
    stack([load("fig5_1a_qa_harness.png"), load("fig5_1b_responsive.png"),
           load("fig5_1c_real_backend.png")], "fig5_1_harness.png", crop_to=900)
    # Appendix F: a pair and a trio, side by side at a readable width
    row([load("appF_auth_login.png"), load("appF_auth_register.png")], "appF_auth_pair.png", 720)
    row([load("appF_terms.png"), load("appF_privacy.png"), load("appF_404.png")], "appF_legal_row.png", 480)

    print("\ncomposed:")
    for n in ("fig4_3_request_forms.png", "fig4_5_admin_panels.png", "fig5_1_harness.png",
              "appF_auth_pair.png", "appF_legal_row.png"):
        im = load(n)
        print("  %-28s %-11s %5.1f%%  (at 15cm wide -> %.1fcm tall)" %
              (n, "%dx%d" % im.size, ink(im) * 100, 15.0 * im.height / im.width))


if __name__ == "__main__":
    main()
