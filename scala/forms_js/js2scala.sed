# cf https://www.scala-js.org/doc/sjs-for-js/es6-to-scala-part3.html
# http://www.mikaelmayer.com/2015/08/07/converting-javascript-to-scala-on-scala-js/
# https://www.tutorialspoint.com/unix/unix-regular-expressions.htm

s/var /val /
s/const /val /
s/function()/ () => /
s/function *( *\([[:alnum:]]\+\) *)/ (\1: Object) => /
s/function *( *\([[:alnum:]]\+\), *\([[:alnum:]]\+\) *)/ (\1: Object, \2: Object) => /
s/function /def /

s/\[\]/Array()/
s/switch /match /
s/reduce(/foldLeft(/
s/\.then(/.foreach(/
s///

