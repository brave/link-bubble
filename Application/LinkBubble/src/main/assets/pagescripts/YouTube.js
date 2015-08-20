(function() {
    var YOUTUBE_EMBED_PREFIX = "//www.youtube.com/embed/";
    var elems = document.getElementsByTagName('*'), i;
    var resultArray = null;
    var resultCount = 0;
    for (i in elems) {
       var elem = elems[i];
       if (elem.src != null && elem.src.indexOf(YOUTUBE_EMBED_PREFIX) != -1) {
           if (resultArray == null) {
               resultArray = new Array();
           }
           resultArray[resultCount] = elem.src;
           resultCount++;
       }
    }
    if (resultCount > 0) {
        LinkBubble.onYouTubeEmbeds(resultArray.toString());
    }
})();
