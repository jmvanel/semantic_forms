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
        .attr('class',widget.attr('class'))
        //.addClass( widget.attr('id'))
        .attr('id', widget.attr('id')+'-'+cardinal)
        .attr('name', widget.attr('name'))
        .attr('title', widget.attr('title'))
        .attr('hidden', 'false');
    parent.prepend(addedWidget, widget);
    addedWidget.focus();
    return addedWidget;
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}
