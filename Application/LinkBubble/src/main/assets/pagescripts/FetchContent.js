(function() {
    if (shouldFetchContent) {
        window.LinkBubble.fetchHtml(document.documentElement.outerHTML);
    }
})();
