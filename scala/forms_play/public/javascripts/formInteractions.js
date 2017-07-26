"use strict";
/*jslint browser: true*/

/** button with an action to duplicate the original HTML widget with an empty content */
function cloneWidget(widget,button) {

    var addedWidget = $("<input type='text' />"),
        parent = widget.parent(),
        cardinal = widget.parent().children().length;
    console.log("nombre de widgets : "+cardinal);

    var widgetName = decodeURIComponent(widget.attr("name")).split("+")
    if(widgetName[0] !== 'undefined'){
        if (widgetName[2][0] == '<' )
            widgetName = widgetName[0] +'+'+widgetName[1]+'+<>+.'
        else
            widgetName = widgetName[0] +'+'+widgetName[1]+'+""+.'

        addedWidget
            .val('')
            .attr('class',widget.attr('class'))
            //.addClass( widget.attr('id'))
            .attr('id', widget.attr('id')+'-'+cardinal)
            .attr('name', widgetName)
            .attr('title', widget.attr('title'))
            .attr('hidden', 'false');
        parent.prepend(addedWidget, widget);
        addedWidget.focus();
    }
    else {
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
        addedWidget.focus();
    }

    return addedWidget;
}

function backlinks(uri) {
    var url = window.document.location.origin + '/backlinks?q=' + encodeURIComponent(uri);
    window.document.location.assign( url );
}
