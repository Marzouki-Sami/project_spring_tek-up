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