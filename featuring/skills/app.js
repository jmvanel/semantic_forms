var port = 3000;
var express = require('express');
var path = require('path');
var app = express();
app.use(express.logger('dev'));
app.use(express.static(path.join(__dirname, 'public')));
app.get("*", function(req, res) {
  res.send("", 204);
});
app.listen(port, function() {
  console.log("Server started on port " + port);
});