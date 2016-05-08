function popupgraph(uri) {
  var popupWindow = window.open('', 'drawgraph',
    'height=500,width=500,resizable=yes,modal=yes');
  var doc = popupWindow.document;

  var htmlContent = `<html><head>
    <script src="javascripts/jquery-2.2.3.min.js" type="text/javascript"></script>
    <script src="assets/rdfviewer/rdf.js" type="text/javascript"></script>
    <script src="assets/rdfviewer/md5.min.js" type="text/javascript"></script>
    <script type="text/javascript" src="http://mbostock.github.com/d3/d3.v2.js" type="text/javascript"></script>
    <link rel="stylesheet" href="assets/rdfviewer/basic.css"></link>
    <link rel="stylesheet" href="assets/rdfviewer/rdfviewer.css"></link>
	</head><body>
	<button id="exitButton">DISMISS</button><br/>
	<svg id="chart"></svg>
	<script type="text/javascript">
		var exitButton = window.getElementById("exitButton");
		exitButton.onclick = function() { window.close(); };
	</script>
	</body></html>
	`
  console.log( htmlContent );  
  doc.write( htmlContent );
  doc.close();
	popupWindow.setTimeout( function() {
		// Wait 2 second, in production you want to hook to a proper load event.
		viewrdf("#chart", 1000,1000, uri, 300);	
	}, 2000 );

  // download and parse JSON-LD from given URI, e.g.
  // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
  // setting the HTTP header Accept: text/json-ld
  // using RDF-Ext or N3.js ?
  // var listOfTriples = []; drawGraph( listOfTriples , containerId );
};

