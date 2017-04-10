"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widget) {
    var addedWidget = $("<input type='text' />"),
        parent = widget.parent(),
        cardinal = widget.parent().children().length;
    console.log("nombre de widgets : "+cardinal);
    addedWidget
        .val('')
        .addClass('hasLookup form-control ui-autocomplete-input')
        .attr('id', widget.attr('id')+'-'+cardinal)
        .attr('name', widget.attr('name'))
        .attr('hidden', 'false');
    parent.prepend(addedWidget, widget);
    addedWidget.focus();
    return addedWidget;
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}
