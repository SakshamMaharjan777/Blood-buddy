/* =============================================
   BLOODBUDDY — bb-validate.js
   Shared client-side form validation.

   Loaded BEFORE a page's inline script. Every
   form (auth, request, emergency, contact,
   profiles) validates through this module so the
   rules live in ONE place:

     login/register/forgot .  email, password
     requester-request    .  blood group, units, needed-by
     emergency-request    .  blood group, units, needed-by, phone
     profile pages        .  email, phone, names
     contact              .  name, email, message

   Errors render INLINE under the field (a
   <small.field-error> inside the field's
   .field-group) instead of only in alert boxes.

   NOTE (CPJ119 2.2): the frontend only does
   *basic* input validation — the real gate is
   server-side (Spring validation in CPP401).
   ============================================= */

(function () {
  'use strict';

  /* ---- Format rules (proposal §1.4: required fields, blood group format, email format) ---- */
  var RX = {
    email: /^\S+@\S+\.\S+$/,
    phone: /^\d{10}$/,
    /* Blood group format: A/B/AB/O with a +/− Rh factor, e.g. O+, AB- */
    blood: /^(A|B|AB|O)[+-]$/,
    password: /.{6,}/
  };

  var MESSAGES = {
    required: 'This field is required.',
    email: 'Please enter a valid email address.',
    phone: 'Please enter a 10-digit phone number.',
    blood: 'Select a valid blood group (e.g. O+).',
    password: 'Password must be at least 6 characters.',
    future: 'Choose today or a future date.',
    minUnits: 'At least 1 unit is required.',
    terms: 'You must accept the Terms to continue.'
  };

  /* ---- Inline error UI -------------------------------
     Field wrappers vary by page: .field-group (auth),
     .form-field (request/emergency), .pf-field (profiles),
     .ct-field (contact). The error <small> lives inside
     whichever wrapper the field has. */

  var GROUP_SEL = '.field-group, .form-field, .pf-field, .ct-field';

  function groupOf(input) {
    return input.closest ? input.closest(GROUP_SEL) : null;
  }

  /* Ensure a <small class="field-error"> exists inside the field group */
  function ensureErrorEl(input) {
    var group = groupOf(input);
    if (!group) return null;
    var small = group.querySelector('.field-error');
    if (!small) {
      small = document.createElement('small');
      small.className = 'field-error';
      small.setAttribute('role', 'alert');
      group.appendChild(small);
    }
    return small;
  }

  function setError(input, msg) {
    input.setAttribute('aria-invalid', 'true');
    var group = groupOf(input);
    if (group) {
      group.classList.add('has-error');
      var small = ensureErrorEl(input);
      if (small) small.textContent = msg;
    } else {
      /* No wrapper (rare) — fall back to title tooltip */
      input.title = msg;
    }
  }

  function clearError(input) {
    input.removeAttribute('aria-invalid');
    var group = groupOf(input);
    if (group) group.classList.remove('has-error');
  }

  /* ---- Rule engine ----------------------------------- */

  function todayStr() {
    var d = new Date();
    return d.getFullYear() + '-' +
      String(d.getMonth() + 1).padStart(2, '0') + '-' +
      String(d.getDate()).padStart(2, '0');
  }

  /* Validate ONE input against a rules spec.
     rules: { required, email, phone, blood, password, future, minUnits, terms,
              test: function(value) -> error message or null }
     Returns the error message, or null when valid. Also drives the inline UI. */
  function check(input, rules) {
    rules = rules || {};
    clearError(input);

    var raw = input.type === 'checkbox' ? input.checked : String(input.value == null ? '' : input.value).trim();

    if (rules.required && (raw === '' || raw === false || (input.tagName === 'SELECT' && !input.value))) {
      var m = typeof rules.required === 'string' ? rules.required : MESSAGES.required;
      setError(input, m);
      return m; /* always reported so checkAll() can focus the first failure */
    }
    if (raw === '') return null; /* nothing else to check on an empty, non-required field */

    var msg = null;
    if (rules.email && !RX.email.test(raw)) msg = MESSAGES.email;
    else if (rules.phone && !RX.phone.test(raw)) msg = MESSAGES.phone;
    else if (rules.blood && !RX.blood.test(raw)) msg = MESSAGES.blood;
    else if (rules.password && !RX.password.test(raw)) msg = MESSAGES.password;
    else if (rules.future && raw < todayStr()) msg = MESSAGES.future;
    else if (rules.minUnits && !(parseFloat(raw) >= 1)) msg = MESSAGES.minUnits;
    else if (rules.terms && input.checked !== true) msg = MESSAGES.terms;
    else if (typeof rules.test === 'function') msg = rules.test(raw) || null;

    if (msg) setError(input, msg);
    return msg;
  }

  /* Validate a LIST of { input, rules } pairs.
     Returns the array of failed inputs (empty = all valid) and focuses the first. */
  function checkAll(pairs) {
    var failed = [];
    pairs.forEach(function (p) {
      if (check(p.input, p.rules)) failed.push(p.input);
    });
    if (failed.length && failed[0].focus) failed[0].focus();
    return failed;
  }

  /* Clear a field's error as soon as the user edits it (light-touch live UX) */
  function bindLive(inputs) {
    Array.prototype.forEach.call(inputs, function (input) {
      var handler = function () { clearError(input); };
      input.addEventListener('input', handler);
      input.addEventListener('change', handler);
    });
  }

  window.BloodBuddyValidate = {
    rx: RX,
    messages: MESSAGES,
    check: check,
    checkAll: checkAll,
    setError: setError,
    clearError: clearError,
    bindLive: bindLive
  };
})();
