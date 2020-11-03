// "use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widget0, button) {
	if (widget0.parent == undefined) // plain JavaScript, not JQuery
		widget = jQuery(widget0)
	else
		widget = widget0

    var addedWidget = $("<input type='text' />"),
        parent = widget.parent(),
        cardinal = parent.children().length;
    console.log("nombre de widgets : "+cardinal);
    console.log("addedWidget 1: " + JSON.stringify( addedWidget , null, 4));

    var widgetName = decodeURIComponent(widget.attr("name")).split("+")
    console.log("widget name: " + widgetName);
    if(widgetName[0] !== 'undefined'){
        if (widgetName[2][0] == '<' )
            widgetName = widgetName[0] +'+'+widgetName[1]+'+<>+.'
        else
            widgetName = widgetName[0] +'+'+widgetName[1]+'+""+.'

        addedWidget
            .val('')
            .attr('class',widget.attr('class'))
            .attr('id', widget.attr('id')+'-'+cardinal)
            .attr('name', widgetName)
            .attr('title', widget.attr('title'))
            .attr('hidden', 'false')
            .attr('data-rdf-subject', widget.attr('data-rdf-subject'))
            .attr('data-rdf-property', widget.attr('data-rdf-property'))
            .attr('data-rdf-object', widget.attr('data-rdf-object'))
            .attr('data-rdf-type', widget.attr('data-rdf-type'))
        parent.prepend(addedWidget, widget);
    }
    else {
      if( button != undefined ) {
        widgetName = decodeURIComponent(button.attr('input-name')).split("+")
        if (widgetName[2][0] == '<' )
            widgetName = widgetName[0] +'+'+widgetName[1]+'+<>+.'
        else
            widgetName = widgetName[0] +'+'+widgetName[1]+'+""+.'

        addedWidget
            .val('')
            .attr('class',button.attr('input-class'))
            //.addClass( widget.attr('id'))
            .attr('id','new')
            .attr('name', widgetName)
            .attr('title', button.attr('input-title'))
            .attr('hidden', 'false');
        button.parent().parent().find('.sf-value-block').first().prepend(addedWidget, widget);
      }
    }
    addedWidget.focus();

    // console.log("addedWidget: " + JSON.stringify( addedWidget , null, 4));
    return addedWidget;
}

// UNUSED !
function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}
