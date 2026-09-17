/*
 * Builds .tools/wt/walkthrough/index.html from walkthrough.txt — one page to page
 * through during the rehearsal, instead of opening 31 PNGs by hand.
 *
 *   node tools/walkthrough/gallery.js
 */
const fs = require('fs');
const path = require('path');

const DIR = process.env.BB_WALK_OUT || path.resolve(__dirname, '..', '..', '.tools', 'wt', 'walkthrough');
const txt = fs.readFileSync(path.join(DIR, 'walkthrough.txt'), 'utf8').split('\n');

const checks = [];
const shots = [];
let mode = null;
for (const line of txt) {
  if (/^ASSERTIONS \(/.test(line)) { mode = 'check'; checks.push({ header: line.trim() }); continue; }
  if (/^SHOTS \(/.test(line)) { mode = 'shot'; shots.push({ header: line.trim() }); continue; }
  if (/^STEPS/.test(line)) { mode = null; continue; }
  if (mode === 'check' && /^  (PASS|FAIL) {2}/.test(line)) {
    const m = /^  (PASS|FAIL) {2}(.*)$/.exec(line);
    const [label, detail] = m[2].split(' — ');
    checks.push({ ok: m[1] === 'PASS', label, detail: detail || '' });
  } else if (mode === 'shot' && /^  [0-9A-Za-z_]+\.png/.test(line)) {
    const m = /^  ([0-9A-Za-z_]+)\.png {2}(.*)$/.exec(line);
    if (m) shots.push({ name: m[1], note: m[2].replace(/ {3}\[BYTE-IDENTICAL.*$/, '') });
  }
}

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const passed = checks.filter(c => c.ok).length;
const failed = checks.filter(c => c.ok === false).length;
const real = shots.filter(s => s.name);

const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<title>BloodBuddy — demo runbook walkthrough (${real.length} checkpoints)</title>
<style>
  body { font: 15px/1.55 "Segoe UI", system-ui, sans-serif; background: #F7F6F4; color: #1c1c1e;
         margin: 0; padding: 32px 24px 80px; }
  .wrap { max-width: 1180px; margin: 0 auto; }
  h1 { font-size: 27px; margin: 0 0 4px; }
  .sub { color: #6b6b70; margin-bottom: 22px; }
  .card { background: #fff; border-radius: 12px; padding: 18px 20px; margin: 0 0 22px;
          box-shadow: inset 0 0 0 1px rgba(0,0,0,.07); }
  .tally { font-size: 17px; font-weight: 600; }
  .tally b { color: #C0202A; }
  table { border-collapse: collapse; width: 100%; font-size: 14px; }
  td { padding: 5px 8px; border-bottom: 1px solid #eceae7; vertical-align: top; }
  td.ok { color: #0a7c42; font-weight: 600; white-space: nowrap; }
  td.bad { color: #C0202A; font-weight: 700; }
  figure { margin: 0 0 26px; background: #fff; border-radius: 12px; overflow: hidden;
           box-shadow: inset 0 0 0 1px rgba(0,0,0,.07); }
  figure img { display: block; width: 100%; height: auto; border-bottom: 1px solid #eceae7; }
  figcaption { padding: 11px 14px; font-size: 14px; }
  figcaption b { font-family: ui-monospace, Consolas, monospace; font-size: 13px; color: #C0202A; }
  ol { margin: 0; padding-left: 0; list-style: none; }
  li { margin-bottom: 26px; }
  .lbl { display: block; font-size: 12px; letter-spacing: .06em; text-transform: uppercase;
         color: #8a8a90; margin: 0 0 6px; }
  .hint { background: #fff; border-left: 3px solid #C0202A; padding: 12px 16px; border-radius: 8px;
          margin-bottom: 22px; font-size: 14px; }
</style></head><body><div class="wrap">
<h1>BloodBuddy — the demo runbook, walked in the browser</h1>
<div class="sub">Generated from <b>walkthrough.txt</b> by <b>.tools/shots/walkthrough.js</b>.
Every checkpoint below is a screenshot of the running app at <code>localhost:8081</code>, driven
through the same controls a human clicks — the real login form, not a planted cookie.</div>

<div class="hint"><b>The two things an API-level run cannot see</b>, both checked here:
the hospital queue shows <b>TUTH's own rows for a TUTH session</b> (18), and the Mailpit inbox
gains <b>exactly one</b> mail per registration (07).</div>

<div class="card">
  <div class="tally">Assertions: <b>${passed} passed</b>${failed ? `, <b>${failed} FAILED</b>` : ', 0 failed'}</div>
  <table><tbody>
  ${checks.filter(c => c.label).map(c =>
    `<tr><td class="${c.ok ? 'ok' : 'bad'}">${c.ok ? 'PASS' : 'FAIL'}</td><td>${esc(c.label)}` +
    (c.detail ? `<br><span style="color:#6b6b70">${esc(c.detail)}</span>` : '') + '</td></tr>').join('\n  ')}
  </tbody></table>
</div>

<ol>
${shots.filter(s => s.name).map(s => `<li><span class="lbl">${esc(s.note.split(' — ')[0])}</span>
  <figure><img src="${s.name}.png" alt="${esc(s.note)}">
  <figcaption><b>${s.name}.png</b> — ${esc(s.note)}</figcaption></figure></li>`).join('\n')}
</ol>
<div class="sub">Full machine-readable record: <a href="walkthrough.txt">walkthrough.txt</a></div>
</div></body></html>
`;

fs.writeFileSync(path.join(DIR, 'index.html'), html);
console.log('wrote ' + path.join(DIR, 'index.html') + ' — ' + real.length + ' shots, ' +
  passed + ' passing assertions' + (failed ? ', ' + failed + ' FAILED' : ''));
