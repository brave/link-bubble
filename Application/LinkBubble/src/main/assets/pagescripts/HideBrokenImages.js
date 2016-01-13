(function () {
  var images = document.querySelectorAll('img');

  Array.from(images).forEach(img => {
    if (!img.complete || typeof img.naturalWidth === "undefined" || img.naturalWidth === 0) {
      img.style.visibility = 'hidden';
    }
  });
})();