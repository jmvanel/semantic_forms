$(function () {
  // Password zone.
  var $passwordSection = $('.input-group-password');
  $passwordSection.each(function () {
    // Find "view password" button.
    $passwordSection
      .find('.auth-see-password')
      .click(function () {
        // Find input password.
        var $passwordInput = $passwordSection.parent().find('.input-password');
        // Toggle.
        $passwordInput.attr('type', $passwordInput.attr('type') === 'password' ? 'text' : 'password');
      });
  });
});
