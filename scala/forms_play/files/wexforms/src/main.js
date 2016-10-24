(function () {
  'use strict';
  // Debug.
  window.log = function (m) {
    console.log(m);
  };
  // Create a new WexForm instance.
  window.wexForm = new WexForm();
  // Wait for document loaded.
  document.addEventListener('DOMContentLoaded', () => {

    document.getElementById('getTest').addEventListener('click', () => {
      wexForm.formDisplay('http://jmvanel.free.fr/jmv.rdf#me', (xhr) => {
        document.getElementById('resultTest').innerHTML = xhr.responseText;
      });
    });
  });
}());