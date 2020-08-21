package org.omg.demo.terms.internal;

import edu.mayo.kmdp.util.NameUtils;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

@Named
public class TermsBuilder {

  public List<ConceptDescriptor> buildTerms(
      List<Bindings> uriToLabelMap,
      TermsQueryType queryType) {
    switch (queryType) {
      case DBPEDIA:
        return uriToLabelMap.stream()
            .map(m -> (ConceptDescriptor) new ConceptDescriptor()
                .withName(m.get("Concept").toString()))
            .collect(Collectors.toList());
      case VIRTUOSO: // case Virtuoso
        return uriToLabelMap.stream()
            .map(m -> (ConceptDescriptor) new ConceptDescriptor()
                .withTag(NameUtils.getTrailingPart(m.get("role").toString()))
                .withName(m.get("label").toString()))
            .collect(Collectors.toList());
      case OWL2:
      case SKOS:
        return uriToLabelMap.stream()
            .map(m -> (ConceptDescriptor) new ConceptDescriptor()
                .withResourceId(URI.create(m.get("uri").toString()))
                .withTag(NameUtils.getTrailingPart(m.get("uri").toString()))
                .withName(m.get("label").toString()))
            .collect(Collectors.toList());
      default:
        throw new UnsupportedOperationException();
    }
  }

}
