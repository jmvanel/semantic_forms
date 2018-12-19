package deductions.runtime.jena.lucene

import deductions.runtime.jena.ImplementationSettings
import org.apache.jena.query.text.{DatasetGraphText, TextIndexLucene}
import org.apache.lucene.index.{DirectoryReader, MultiFields}
import org.apache.lucene.search.spell.Dictionary
import org.apache.lucene.search.suggest.InputIterator
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester
import org.apache.lucene.util.Version

/** UNUSED yet !!!! */
trait TextIndexerWeight extends LuceneIndex {

  /**
   * add weights to existing Lucene Documents from Jena-text module:
   *  count of triples <S> ?p ?o for each <S> in RDF dataset,
   * (which are mirrored in Lucene Documents)
   *
   * inspired by
   * http://stackoverflow.com/questions/24968697/how-to-implements-auto-suggest-using-lucenes-new-analyzinginfixsuggester-api
   * See also:
   * http://lucene.apache.org/core/6_3_0/core/overview-summary.html#overview.description
   */
  def addRDFWeights(dataset: ImplementationSettings.DATASET, useTextQuery: Boolean) = {
    val datasetLucene = configureLuceneIndex(dataset, useTextQuery, false)
    if (datasetLucene != dataset) {
      val dsg = datasetLucene.asDatasetGraph()
      datasetLucene match {
        case dsgText: DatasetGraphText =>
          val textIndex = dsgText.getTextIndex
          textIndex match {
            case textIndex: TextIndexLucene =>
              val directory = textIndex.getDirectory
              val analyzer = textIndex.getAnalyzer

              // add weight to existing Lucene Documents
              val suggester = new AnalyzingInfixSuggester(
                directory, analyzer);
              val indexReader = DirectoryReader.open(directory);

              // Note cannot inherit from LuceneDictionary, have to copy its code:
              val dict: Dictionary = new Dictionary {
                override def getEntryIterator(): InputIterator = {
                  // throws IOException;
                  val terms = MultiFields.getTerms(indexReader, "label")
                  if (terms != null) {
                    new InputIterator.InputIteratorWrapper(terms.iterator()) {
                      override def weight(): Long = {
                        1 // TODO
                      }
                    }
                  } else {
                    InputIterator.EMPTY
                  }
                }
              }
              suggester.build(dict)

            /* TODO
           * 1. compute weight = count of triples <S> ?p ?o . and ?s ?p <S> .
           * 2. need to do the weight adding for all properties configured, not just rdfs:label 
           */

            // other possibiliy:
            // suggester.build(new ProductIterator(products.iterator()));
            // cf http://stackoverflow.com/questions/24968697/how-to-implements-auto-suggest-using-lucenes-new-analyzinginfixsuggester-api/25301811#25301811
          }
      }
    }
  }
}