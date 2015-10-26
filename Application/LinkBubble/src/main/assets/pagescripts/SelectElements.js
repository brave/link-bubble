;(function () {
  if (window.LinkBubble.selectOption) { return }
  window.LinkBubble.lastSelectFocused = null
  window.LinkBubble.selectOption = function (index) {
    var select = window.LinkBubble.lastSelectFocused
    select.selectedIndex = index
    select.previousElementSibling.textContent = select[index].text
  }
  var positioningProps = ['float', 'position', 'width', 'height', 'left', 'top', 'margin-left', 'margin-top', 'padding-left', 'padding-top', 'border', 'background']
  var els = document.getElementsByTagName('select')
  function maskSelects () {
    /* Remove all previous select masks if the next element is not a select any longer. */
    Array.prototype.forEach.call(document.querySelectorAll('.__link_bubble__select_mask__'), function (mask) {
      if (mask.nextElementSibling && mask.nextElementSibling.nodeName.toLowerCase() === 'select') { return }
      mask.parentNode.removeChild(mask)
    })

    Array.prototype.forEach.call(els, function (select) {
      var mask = select.previousElementSibling
      /* Insert and style for new selects */
      if (!mask || mask.className !== '__link_bubble__select_mask__') {
        mask = document.createElement('div')
        mask.className = '__link_bubble__select_mask__'
        mask.style.webkitAppearance = 'menulist'
        var computedStyle = window.getComputedStyle(select)

        for (var i in positioningProps) {
          var prop = positioningProps[i]
          mask.style[prop] = computedStyle.getPropertyValue(prop)
        }
        select.parentNode.insertBefore(mask, select)
        select.style.display = 'none'

        mask.addEventListener('click', function (e) {
          e.preventDefault()
          window.LinkBubble.lastSelectFocused = select
          var keyAndValues = [select.selectedIndex]
          for (var i = 0; i < select.length; i++) {
            keyAndValues.push(select[i].text)
            keyAndValues.push(select[i].value)
          }
          window.LinkBubble.onSelectElementInteract(JSON.stringify(keyAndValues))
        })
      }
      mask.textContent = select[select.selectedIndex].text
    })
  }
  /* Mask all selects when the script is injected. */
  maskSelects()
  /* Use a mutation observer for dynamic selects added after page load. */
  var MutationObserver = window.MutationObserver || window.WebKitMutationObserver
  var observer = new MutationObserver(function (mutations) {
    mutations.forEach(function (mutation) {
      var changed = false
      var allChangedNodes = [].slice.call(mutation.addedNodes).concat([].slice.call(mutation.removedNodes))
      allChangedNodes.forEach(function (changedNode) {
        if ((changedNode.querySelector && changedNode.querySelector('select')) || changedNode.nodeName.toLowerCase() === 'select') {
          changed = true
        }
      })
      if (changed) {
        maskSelects()
      }
    })
  })
  var config = {attributes: false, childList: true, characterData: false, subtree: true}
  observer.observe(document, config)
})()
