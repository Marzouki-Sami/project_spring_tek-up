// Runs in <head> before the page paints, so dark mode never flashes white.
// Uses the saved choice, otherwise the operating system preference.
(function () {
    var saved = null;
    try { saved = localStorage.getItem('theme'); } catch (e) { /* storage unavailable */ }
    var dark = saved ? saved === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-bs-theme', dark ? 'dark' : 'light');
})();