function popupgraph(uri) {
  var popupWindow = window.open('', 'drawgraph',
    'height=500,width=500,resizable=yes,modal=yes');

  var exitButton = popupWindow.document.createElement( "button" );
  exitButton.textContent = "DISMISS";
  popupWindow.document.body.appendChild( exitButton );
  exitButton.onclick = function() { popupWindow.close(); };

  var graphContainer = popupWindow.document.createElement( "div" );
  graphContainer.id = "containerId";

  // TODO download and parse JSON-LD from
  // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
  drawGraph( listOfTriples , containerId );

};

