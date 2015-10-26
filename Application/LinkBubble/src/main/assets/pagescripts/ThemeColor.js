(function () {
  var themeColorTag = document.getElementsByTagName('meta')['theme-color'];
  if (themeColorTag) {
    window.LinkBubble.onThemeColor(themeColorTag.getAttribute('content'));
  }
})();
