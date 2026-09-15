/* =============================================
   BLOODBUDDY — bb-compat.js
   Blood group compatibility chart (proposal
   Appendix A) in ONE place.

   Before this module the chart was copy-pasted
   into requester-request.html and
   emergency-request.html; donor matching needed
   a third copy, and three copies of a clinical
   rule is how a demo ends up telling a donor
   they can give blood to the wrong patient.

   DIRECTION MATTERS — Appendix A is written
   "recipient → compatible DONOR groups":
     COMPAT['A+'] = ['A+','A-','O+','O-']
   means a recipient who is A+ may receive from
   A+, A-, O+ or O-. So to decide whether a donor
   may serve a request you ask
   isCompatible(donorGroup, recipientGroup),
   NOT whether recipientGroup is in
   donorsFor(donorGroup).

   NOTE (CPP401): this is a UI/match hint only.
   The authoritative compatibility rule belongs
   in the Spring Service layer — a requester must
   never be able to talk the frontend into an
   unsafe match.
   ============================================= */

(function () {
  'use strict';

  /* Recipient blood group → the donor groups that may give to them (Appendix A) */
  var COMPAT = {
    'A+':  ['A+', 'A-', 'O+', 'O-'],
    'A-':  ['A-', 'O-'],
    'B+':  ['B+', 'B-', 'O+', 'O-'],
    'B-':  ['B-', 'O-'],
    'AB+': ['AB+', 'AB-', 'A+', 'A-', 'B+', 'B-', 'O+', 'O-'],
    'AB-': ['AB-', 'A-', 'B-', 'O-'],
    'O+':  ['O+', 'O-'],
    'O-':  ['O-']
  };

  var ALL = Object.keys(COMPAT);

  window.BloodBuddyCompat = {
    /* The raw chart, for rendering the chips on the request forms. */
    groups: COMPAT,

    /* Donor groups that may give to `recipientGroup` (an Appendix A row). */
    donorsFor: function (recipientGroup) {
      return (COMPAT[recipientGroup] || []).slice();
    },

    /* Recipient groups a `donorGroup` may give to (the inverse lookup). */
    recipientsFor: function (donorGroup) {
      return ALL.filter(function (recipient) {
        return COMPAT[recipient].indexOf(donorGroup) > -1;
      });
    },

    /* May `donorGroup` give blood to a patient who is `recipientGroup`? */
    isCompatible: function (donorGroup, recipientGroup) {
      return (COMPAT[recipientGroup] || []).indexOf(donorGroup) > -1;
    },

    /* True when `group` looks like a real blood group (A/B/AB/O + Rh). */
    isValidGroup: function (group) {
      return Object.prototype.hasOwnProperty.call(COMPAT, group);
    }
  };
})();
