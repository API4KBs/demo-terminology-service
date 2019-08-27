/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omg.demo.terms;

import static edu.mayo.kmdp.util.ws.ResponseHelper.asResponse;
import static edu.mayo.kmdp.util.ws.ResponseHelper.attempt;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel.Parsed_Knowedge_Expression;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.SPARQL_1_1;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.canonicalRepresentationOf;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.ontology.taxonomies.lexicon._20190801.Lexicon;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.omg.demo.terms.server.TermsApiDelegate;
import org.omg.demo.terms.temp.KnowledgeBaseService;
import org.omg.demo.terms.temp.QueryBinder;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.http.ResponseEntity;

@KPServer
public class TermsServer implements TermsApiDelegate {

  @Inject
  @KPComponent
  private DeserializeApi parser;

  @Inject
  @KPComponent
  private KnowledgeAssetCatalogApi catalog;

  @Inject
  private KnowledgeBaseService termsKB;

  @Inject
  private QueryBinder binder;

  private KnowledgeCarrier listTermsQueryOWL2;
  private KnowledgeCarrier listTermsQuerySKOS;

  public TermsServer() {
    //
  }

  @PostConstruct
  protected void init() {
    listTermsQueryOWL2 = loadQuery("/sparql/queryTermsOWL.sparql");
    listTermsQuerySKOS = loadQuery("/sparql/queryTermsSKOS.sparql");
  }

  @Override
  public ResponseEntity<List<ConceptIdentifier>> getTerms(
      UUID vocabularyId, String versionTag, String labelFilter) {
    return attempt(
        catalog.getVersionedKnowledgeAsset(vocabularyId, versionTag)
            .map(vocMetadata ->
                termsKB.askQuery(
                    vocabularyId, versionTag,
                    getQuery(vocMetadata, labelFilter)))
            .map(this::buildTerms)
            .orElse(null)
    );
  }

  @Override
  public ResponseEntity<List<Pointer>> listTerminologies() {
    return asResponse(
        catalog.listKnowledgeAssets(
            Formal_Ontology.getTag(),
            null,
            0, -1));
  }


  private List<ConceptIdentifier> buildTerms(Set<Map<String, String>> uriToLabelMap) {
    return uriToLabelMap.stream()
        .map(m -> new ConceptIdentifier()
            .withConceptId(URI.create(m.get("uri")))
            .withTag(NameUtils.getTrailingPart(m.get("uri")))
            .withLabel(m.get("label")))
        .collect(Collectors.toList());
  }

  private KnowledgeCarrier loadQuery(String path) {
    KnowledgeCarrier binary = AbstractCarrier
        .of(TermsServer.class.getResourceAsStream(path))
        .withRepresentation(rep(SPARQL_1_1));
    return parser.lift(binary, Parsed_Knowedge_Expression)
        .orElseThrow(
            () -> new BeanInitializationException("Unable to load necessary query from " + path));
  }

  private KnowledgeCarrier getQuery(KnowledgeAsset vocMetadata, String labelFilter) {
    KnowledgeCarrier paramQuery = selectParametricQuery(vocMetadata);
    return binder.bind(paramQuery,getBindings(vocMetadata,labelFilter));
  }

  private Map<String,Object> getBindings(KnowledgeAsset vocMetadata, String labelFilter) {
    Map<String,Object> bindings = new HashMap<>();
    if (labelFilter != null) {
      bindings.put("?label", labelFilter);
    }
    bindings.put("?vocabulary",
        ((URIIdentifier) vocMetadata.getSecondaryId().get(0)).getUri());
    return bindings;
  }

  private KnowledgeCarrier selectParametricQuery(KnowledgeAsset vocMetadata) {
    return canonicalRepresentationOf(vocMetadata).getLexicon().contains(Lexicon.SKOS)
        ? listTermsQuerySKOS
        : listTermsQueryOWL2;
  }


}
