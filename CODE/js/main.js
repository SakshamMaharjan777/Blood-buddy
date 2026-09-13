/* =============================================
   BLOODBUDY — main.js
   Button ripples + card tilt. Reusable on
   every page (selectors are class-based).
   ============================================= */

const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/* --- 1. Button ripple on click --- */
const buttons = document.querySelectorAll(
  '.btn-primary, .btn-ghost, .btn-search, .btn-outline-nav, ' +
  '.btn-filled-nav, .cta-btn-white, .cta-btn-outline'
);

buttons.forEach((btn) => {
  btn.addEventListener('click', (e) => {
    if (reduceMotion) return;

    // center the ripple for keyboard (Enter) presses
    const fromKeyboard = e.clientX === 0 && e.clientY === 0;
    const rect = btn.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const x = fromKeyboard ? rect.width / 2 : e.clientX - rect.left;
    const y = fromKeyboard ? rect.height / 2 : e.clientY - rect.top;

    const ripple = document.createElement('span');
    ripple.className = 'ripple';
    ripple.style.width = ripple.style.height = size + 'px';
    ripple.style.left = x - size / 2 + 'px';
    ripple.style.top = y - size / 2 + 'px';

    btn.appendChild(ripple);
    ripple.addEventListener('animationend', () => ripple.remove());
  });
});

/* --- 2. Subtle 3D tilt on cards --- */
const cards = document.querySelectorAll('.step-card, .role-card, .compat-card, .stat-card');

if (!reduceMotion && window.matchMedia('(hover: hover)').matches) {
  cards.forEach((card) => {
    card.addEventListener('mousemove', (e) => {
      const rect = card.getBoundingClientRect();
      const px = (e.clientX - rect.left) / rect.width - 0.5;   // -0.5 .. 0.5
      const py = (e.clientY - rect.top) / rect.height - 0.5;
      card.style.transform =
        `translateY(-2px) perspective(600px) rotateX(${(-py * 4).toFixed(2)}deg) rotateY(${(px * 4).toFixed(2)}deg)`;
    });
    card.addEventListener('mouseleave', () => { card.style.transform = ''; });
  });
}
