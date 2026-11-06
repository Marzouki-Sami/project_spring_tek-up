// Asks for confirmation before submitting any form marked with data-confirm.
// Kept in a file (not inline) so the Content-Security-Policy can forbid inline scripts.
document.addEventListener('submit', function (event) {
    var message = event.target.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});