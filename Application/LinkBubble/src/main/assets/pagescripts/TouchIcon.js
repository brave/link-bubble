(function() {
	var links = document.head.getElementsByTagName('link');
	var linksArray = null;
	var linksCount = 0;
	for(var link in links){
        if(links.hasOwnProperty(link)){
            var l = links[link];
            if (l.rel != null && l.rel.indexOf('apple-touch-icon') != -1) {
                if (linksArray == null) {
                    linksArray = new Array();
                }
                var s = "@@@" + l.rel + "," + l.href + "," + l.sizes + "###";
                linksArray[linksCount] = s;
                linksCount++;
            }
        }
	}
	if (linksCount > 0) {
		LinkBubble.onTouchIconLinks(linksArray.toString());
	}
})();
