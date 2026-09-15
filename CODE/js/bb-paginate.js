/* =============================================
   BLOODBUDY — bb-paginate.js
   Reusable pagination widget (proposal §1.4:
   "Pagination for large result sets, including
   donor search results and administrative user
   and request listings").

   Ready to wire to a PAGINATED API later: today
   the page slices the full array client-side via
   pager.slice(items); in CPP401 the onChange
   callback can instead request
   GET /api/...?page=X&size=Y and setTotal()
   feeds the server's total count.

   Usage:
     const pager = BloodBuddyPaginate.create({
       mount: document.getElementById('pager-slot'),
       total: items.length,
       pageSize: 5,                 // default page size
       pageSizes: [5, 10, 20],
       onChange: (page, size) => render()
     });
     ...
     pager.setTotal(filtered.length);   // call when the data changes
     const pageItems = pager.slice(filtered);
   ============================================= */

(function () {
  'use strict';

  function create(opts) {
    opts = opts || {};
    var page = 1;
    var total = opts.total || 0;
    var pageSize = opts.pageSize || 5;
    var sizes = opts.pageSizes || [5, 10, 20];

    var nav = document.createElement('nav');
    nav.className = 'bb-pager';
    nav.setAttribute('aria-label', opts.label || 'Pagination');
    nav.innerHTML =
      '<button type="button" class="bb-pager-btn" data-act="prev">&lsaquo; Prev</button>' +
      '<span class="bb-pager-info">Page <b class="bb-pager-cur">1</b> of <b class="bb-pager-last">1</b></span>' +
      '<button type="button" class="bb-pager-btn" data-act="next">Next &rsaquo;</button>' +
      '<label class="bb-pager-size">Per page ' +
      '<select class="bb-pager-select" aria-label="Results per page">' +
      sizes.map(function (n) {
        return '<option' + (n === pageSize ? ' selected' : '') + '>' + n + '</option>';
      }).join('') +
      '</select></label>';

    (opts.mount || document.body).appendChild(nav);

    var prev = nav.querySelector('[data-act="prev"]');
    var next = nav.querySelector('[data-act="next"]');
    var cur = nav.querySelector('.bb-pager-cur');
    var last = nav.querySelector('.bb-pager-last');
    var sel = nav.querySelector('.bb-pager-select');

    function pageCount() { return Math.max(1, Math.ceil(total / pageSize)); }

    function render() {
      cur.textContent = page;
      last.textContent = pageCount();
      prev.disabled = page <= 1;
      next.disabled = page >= pageCount();
      nav.hidden = total === 0; /* nothing to page through — hide the bar entirely */
    }

    function emit() {
      render();
      if (typeof opts.onChange === 'function') opts.onChange(page, pageSize);
    }

    prev.addEventListener('click', function () { if (page > 1) { page -= 1; emit(); } });
    next.addEventListener('click', function () { if (page < pageCount()) { page += 1; emit(); } });
    sel.addEventListener('change', function () {
      pageSize = parseInt(sel.value, 10) || 5;
      page = 1;
      emit();
    });

    render();

    return {
      el: nav,
      /* Update the total when the (filtered) dataset changes; clamps the page. */
      setTotal: function (t) {
        total = t || 0;
        if (page > pageCount()) page = pageCount();
        render();
      },
      /* Back to page 1 (call after a filter/search change). */
      reset: function () { page = 1; render(); },
      page: function () { return page; },
      pageSize: function () { return pageSize; },
      /* Current page's slice of a full (filtered) array. */
      slice: function (items) {
        var start = (page - 1) * pageSize;
        return items.slice(start, start + pageSize);
      }
    };
  }

  window.BloodBuddyPaginate = { create: create };
})();
