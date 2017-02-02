"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widget) {
    var addedWidget = widget.clone(true),
        parent = widget.parent();
    addedWidget.val('');
    parent.prepend(addedWidget, widget);
    console.log(widget.parent().children.length);
    addedWidget.attr('id', widget.attr('id')+'-1')
    addedWidget.focus();
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}
