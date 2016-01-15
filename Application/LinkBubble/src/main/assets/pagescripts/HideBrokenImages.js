/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

(function () {
  var images = document.querySelectorAll('img');

  Array.from(images).forEach(img => {
    if (!img.complete || typeof img.naturalWidth === "undefined" || img.naturalWidth === 0) {
      img.style.visibility = 'hidden';
    }
  });
})();