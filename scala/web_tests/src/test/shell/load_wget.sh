wget --method=POST --output-file=FICHIER --save-headers --body-data '
{
  "@context": "https://deductions.github.io/drivers.context.jsonld",
  "@id": "point:/imei:1234/2017-05-31T12:43:00.000",
  "mobile": "imei:863977030715952",
  "lat": "48.83763",
  "long": "2.3348699",
  "date": "2017-05-31T12:43:00.000",
  "precedingPoint": "point:/imei:1234/2017-05-31T12:44:44.444"
}
' http://localhost:9000/load
