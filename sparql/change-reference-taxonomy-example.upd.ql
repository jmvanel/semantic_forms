PREFIX nature: <http://deductions.github.io/nature_observation.owl.ttl#>
DELETE {
  # do not yet delete home grown taxonomic triples, in cas of several observations for the same taxon
  # GRAPH ?taxonGRAPH { ?taxon ?ptaxon ?otaxon .  }

  # delete link from the existing observation to home grown taxon URI
  GRAPH ?obsGRAPH {
    ?obs nature:taxon ?taxon .
  }
}
INSERT {
  # insert taxonomic triples from MNHN for the existing observation
  GRAPH ?MNHN_TAXON {
    ?MNHN_TAXON ?ptaxon ?otaxon .
  }
  # the existing observation is linked to MNHN taxon
  GRAPH ?obsGRAPH {
    ?obs nature:taxon ?MNHN_TAXON .
  }
}
WHERE {
  GRAPH ?obsGRAPH {
    ?obs nature:taxon ?taxon .
  }
  GRAPH ?taxonGRAPH {
    # home grown taxon URI: INPN number
    ?taxon <http://rs.tdwg.org/dwc/terms/INPN-num> ?INPNnum .
    ?taxon ?ptaxon ?otaxon .
    # make URI for MNHN taxon
    BIND ( IRI( CONCAT( 'http://taxref.mnhn.fr/lod/taxon/', STR(?INPNnum), '/12.0' )) AS ?MNHN_TAXON )
  }
}
