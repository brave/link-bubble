(function() {
    var themeColorTag = document.getElementsByTagName('meta')['theme-color'];
    if (themeColorTag) {
        LinkBubble.onThemeColor(themeColorTag.getAttribute('content'));
    }
})();
