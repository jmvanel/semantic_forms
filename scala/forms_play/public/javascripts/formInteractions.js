"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widgetID) {
    var existingWidget =  window.document.getElementById(widgetID),
        addedWidget = existingWidget.cloneNode(true),
        parent = existingWidget.parentNode;
    addedWidget.value = '';
    parent.insertBefore(addedWidget, existingWidget);
    if( existingWidget.hasLookup )
      addDBPediaLookup( '#' + addedWidget.id );
    addedWidget.focus();
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}

