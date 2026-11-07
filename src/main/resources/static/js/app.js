// Asks for confirmation before submitting any form marked with data-confirm.
// Kept in a file (not inline) so the Content-Security-Policy can forbid inline scripts.
document.addEventListener('submit', function (event) {
    var message = event.target.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});
// Light/dark switch in the navigation bar.
document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-theme-toggle]');
    if (!button) { return; }
    var next = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-bs-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) { /* storage unavailable */ }
});
// Colour swatches: the colour comes from a data attribute and is applied here, because the
// Content-Security-Policy forbids inline style attributes in the markup.
document.querySelectorAll('[data-color]').forEach(function (element) {
    element.style.backgroundColor = element.getAttribute('data-color');
});
// Product gallery: keep the highlighted thumbnail in step with the visible slide.
(function () {
    var gallery = document.getElementById('gallery');
    if (!gallery) { return; }
    gallery.addEventListener('slid.bs.carousel', function (event) {
        document.querySelectorAll('.gallery-thumb').forEach(function (thumb, index) {
            thumb.classList.toggle('active', index === event.to);
        });
    });
})();
// Tooltips: any element with data-bs-toggle="tooltip" and a title explains itself on hover or focus.
document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
    if (window.bootstrap) { window.bootstrap.Tooltip.getOrCreateInstance(element, { trigger: 'hover focus' }); }
});
// Toast notifications. The server renders its messages into #toast-area; this shows them, and
// window.shopToast(kind, message) lets scripts raise the same kind of message (success, info, warning, error).
(function () {
    var area = document.getElementById('toast-area');
    if (!area || !window.bootstrap) { return; }

    function show(element) {
        element.addEventListener('hidden.bs.toast', function () { element.remove(); });
        window.bootstrap.Toast.getOrCreateInstance(element).show();
    }

    area.querySelectorAll('.toast').forEach(show);

    var icons = { success: 'bi-check-circle-fill', info: 'bi-info-circle-fill',
        warning: 'bi-exclamation-triangle-fill', error: 'bi-x-octagon-fill' };
    var delays = { success: 4000, info: 5000, warning: 7000, error: 9000 };

    window.shopToast = function (kind, message) {
        var toast = document.createElement('div');
        toast.className = 'toast app-toast app-toast-' + kind;
        toast.setAttribute('role', kind === 'error' || kind === 'warning' ? 'alert' : 'status');
        toast.setAttribute('data-bs-delay', String(delays[kind] || 5000));
        var body = document.createElement('div');
        body.className = 'toast-body d-flex align-items-start gap-2';
        var icon = document.createElement('i');
        icon.className = 'bi app-toast-icon ' + (icons[kind] || icons.info);
        var text = document.createElement('span');
        text.className = 'flex-grow-1';
        text.textContent = message;
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'btn-close';
        close.setAttribute('data-bs-dismiss', 'toast');
        close.setAttribute('aria-label', 'Close');
        body.append(icon, text, close);
        toast.append(body);
        area.append(toast);
        show(toast);
    };
})();