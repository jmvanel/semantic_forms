Example of running a SPARQL update request:

```shell
$HOME/src/semantic_forms/scala/forms_play/dist/scripts/rupdate.sh \
  "`cat remove-Resource-range.upd.ql`" \
  http://localhost:9000
```

The files named  *.rq are SPARQL queries, and *.upd.rq are SPARQL update files.
