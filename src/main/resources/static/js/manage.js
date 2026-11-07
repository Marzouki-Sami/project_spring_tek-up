// Back-office behaviour: SKU suggestions, the variants table and its bulk actions.
// Kept in a file (not inline) so the Content-Security-Policy can forbid inline scripts.
(function () {
    // Suggest a SKU from the product name until the manager types their own.
    var sku = document.querySelector('[data-sku-from]');
    if (sku) {
        var source = document.getElementById(sku.getAttribute('data-sku-from'));
        var touched = sku.value !== '';
        sku.addEventListener('input', function () { touched = sku.value !== ''; });
        if (source) {
            source.addEventListener('input', function () {
                if (touched) { return; }
                sku.value = source.value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase()
                    .replace(/[^A-Z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40).replace(/-+$/, '');
            });
        }
    }

    // Selects that filter a table apply themselves.
    document.querySelectorAll('[data-autosubmit]').forEach(function (select) {
        select.addEventListener('change', function () { select.form.requestSubmit(); });
    });

    // A colour picker only makes sense for options shown as swatches.
    document.querySelectorAll('[data-color-toggle]').forEach(function (select) {
        var field = select.form.querySelector('[data-color-field]');
        function sync() {
            var chosen = select.options[select.selectedIndex];
            field.classList.toggle('d-none', !chosen || chosen.getAttribute('data-display') !== 'COLOR');
        }
        select.addEventListener('change', sync);
        sync();
    });

    // Copy the text of an element (recovery codes) to the clipboard.
    document.querySelectorAll('[data-copy]').forEach(function (button) {
        button.addEventListener('click', function () {
            var target = document.querySelector(button.getAttribute('data-copy'));
            if (!target || !navigator.clipboard) { return; }
            var text = Array.prototype.map.call(target.querySelectorAll('code'), function (c) { return c.textContent; }).join('\n');
            navigator.clipboard.writeText(text).then(function () {
                if (window.shopToast) { window.shopToast('success', 'Copied to the clipboard'); }
            });
        });
    });

    // Bulk actions on the variants table.
    var bulk = document.querySelector('[data-bulk]');
    if (!bulk) { return; }
    var checks = Array.prototype.slice.call(document.querySelectorAll('.row-check'));
    var all = document.querySelector('[data-select-all]');
    var count = bulk.querySelector('[data-bulk-count]');
    var action = bulk.querySelector('[data-bulk-action]');
    var scope = bulk.querySelector('[name="scope"]');

    function selected() { return checks.filter(function (c) { return c.checked; }).length; }
    function refresh() {
        var n = selected();
        count.textContent = n + ' selected';
        if (all) {
            all.checked = n > 0 && n === checks.length;
            all.indeterminate = n > 0 && n < checks.length;
        }
    }
    function showInputs() {
        bulk.querySelectorAll('[data-bulk-for]').forEach(function (field) {
            field.classList.toggle('d-none', field.getAttribute('data-bulk-for') !== action.value);
        });
    }
    checks.forEach(function (c) { c.addEventListener('change', refresh); });
    if (all) {
        all.addEventListener('change', function () {
            checks.forEach(function (c) { c.checked = all.checked; });
            refresh();
        });
    }
    action.addEventListener('change', showInputs);
    bulk.addEventListener('submit', function (event) {
        var problem = null;
        if (!action.value) {
            problem = 'Choose an action first.';
        } else if (scope.value === 'SELECTED' && selected() === 0) {
            problem = 'Tick at least one variant, or apply the action to all matching ones.';
        }
        if (problem) {
            event.preventDefault();
            if (window.shopToast) { window.shopToast('warning', problem); }
            return;
        }
        if (action.value === 'DELETE' && !window.confirm('Delete the chosen variants? Ordered ones are kept.')) {
            event.preventDefault();
        }
    });
    refresh();
    showInputs();
})();
