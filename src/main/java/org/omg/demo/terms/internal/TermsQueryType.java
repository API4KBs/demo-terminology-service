package org.omg.demo.terms.internal;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public enum TermsQueryType {
  OWL2("/sparql/queryTermsOWL.sparql"),
  SKOS("/sparql/queryTermsSKOS.sparql"),
  VIRTUOSO("/sparql/eswc.sparql"),
  DBPEDIA("/sparql/dbpedia.sparql", "https://dbpedia.org/sparql");

  String sourceURL;
  String targetURL;

  TermsQueryType(String url) {
    this.sourceURL = url;
  }

  TermsQueryType(String url, String tgtUrl) {
    this.sourceURL = url;
    this.targetURL = tgtUrl;
  }

  public String getSourceURL() {
    return sourceURL;
  }

  public Optional<String> getTargetURL() {
    return Optional.ofNullable(targetURL);
  }

  public Stream<TermsQueryType> appliesTo(URI targetEndpoint) {
    if (targetURL != null && URI.create(targetURL).equals(targetEndpoint)) {
      return Stream.of(this);
    }
    return Stream.empty();
  }
}