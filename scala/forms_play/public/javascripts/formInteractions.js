"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with (TODO) an empty content */
function cloneWidget(widgetName) {
    var existingWidget =  window.document.getElementsByName(widgetName)[0],
        addWidget = existingWidget.cloneNode(true),
        parent = existingWidget.parentNode;
    parent.insertBefore(addWidget, existingWidget);
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + uri;
    window.document.location.assign( url );
}

function backlinks_old(uri) {
    var req = new XMLHttpRequest(),
        url = window.document.location.origin + '/backlinks?q=' + uri;
    req.open('GET', url, false);
    req.setRequestHeader('Accept', 'text/html');
    console.log('Sending backlinks HTTP req ' + url);
    req.send(null);
    if (req.status === 200) {
        window.document.documentElement.innerHTML = req.responseText;
    } else {
        console.log('req.status ' + req.status);
        window.alert('backlinks( ' + uri + ') failed: status ' + req.status +
            '\n\t' + url +
            '\n\t' + req.statusText +
            '\n\t' + req.getAllResponseHeaders()
            );
    }
}

