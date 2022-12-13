Currently activated in Play! app.

To build JavaScript from Scala:

	stb fastOptJS

Then add the resulting JS to the web app.:

	JSDIR=../forms_play/public/javascripts
	cp target/scala-2.13/forms_js-fastopt.js $JSDIR
	rm $JSDIR/formInteractions.js

See http://www.scala-js.org/tutorial/basic/

For testing, get the local IP adress with `ifconfig`

