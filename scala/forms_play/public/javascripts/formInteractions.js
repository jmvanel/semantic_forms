/** button with an action to duplicate the original HTML widget with (TODO) an empty content */
function  cloneWidget( widgetName ) {
         var existingWidget = document.getElementsByName( widgetName ) [0];
          var addWidget = existingWidget.cloneNode(true);
          var parent = existingWidget.parentNode;
          parent.insertBefore( addWidget, existingWidget );
}
