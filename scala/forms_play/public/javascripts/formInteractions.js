"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widgetName) {
//    var existingWidget =  window.document.getElementsByName(widgetName)[0],
    var existingWidget =  window.document.getElementById(widgetName),
        addWidget = existingWidget.cloneNode(true),
        parent = existingWidget.parentNode;
    addWidget.value = '';
    parent.insertBefore(addWidget, existingWidget);
    addWidget.focus();
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}

