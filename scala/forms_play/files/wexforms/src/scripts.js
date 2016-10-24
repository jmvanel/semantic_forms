(function () {
  'use strict';
  // Debug tool.
  window.log = function (m) {
    console.log(m);
  };
  // Create a new WexForm instance.
  new WexForm(() => {
    // Page is ready.
    // Init variables.
    var sfSearchForm = document.getElementById('sfSearchForm');
    var sfSearchString = document.getElementById('sfSearchString');
    var sfSearchResult = document.getElementById('sfSearchResult');
    // Use form submit event.
    sfSearchForm.addEventListener('submit', (e) => {
      // Stop current.
      e.preventDefault();
      // Request semantic forms API.
      wexForm.formDisplay(sfSearchString.value, (xhr) => {
        // Display content.
        sfSearchResult.innerHTML = xhr.responseText;
      });
    });
  });
}());