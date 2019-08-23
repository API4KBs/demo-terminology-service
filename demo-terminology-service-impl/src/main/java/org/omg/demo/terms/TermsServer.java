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

import static edu.mayo.kmdp.id.helper.DatatypeHelper.vid;
import static edu.mayo.kmdp.util.Util.toUUID;
import static edu.mayo.kmdp.util.ws.ResponseHelper.asResponse;
import static edu.mayo.kmdp.util.ws.ResponseHelper.attempt;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel.Parsed_Knowedge_Expression;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.SPARQL_1_1;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.canonicalRepresentationOf;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.ontology.taxonomies.lexicon._20190801.Lexicon;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.omg.demo.terms.server.TermsApiDelegate;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.http.ResponseEntity;

@KPServer
public class TermsServer implements TermsApiDelegate {

  @Inject
  private DeserializeApi parser;

  @Inject
  private KnowledgeAssetRepositoryApi repository;
  @Inject
  private KnowledgeAssetCatalogApi catalog;

  private Map<VersionedIdentifier, Optional<KnowledgeCarrier>> knowledgeBases = new HashMap<>();

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
                askQuery(vocabularyId, versionTag, getQuery(vocMetadata, labelFilter)))
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


  // These belong in a KnowledgeBaseManager API
  private Optional<KnowledgeCarrier> ensureLoaded(UUID vocabularyId, String versionTag) {
    return knowledgeBases.computeIfAbsent(
        vid(vocabularyId.toString(), versionTag),
        this::loadVocabulary);
  }

  private Optional<KnowledgeCarrier> loadVocabulary(VersionedIdentifier v) {
    return repository
        .getCanonicalKnowledgeAssetCarrier(toUUID(v.getTag()), v.getVersion(), null)
        .flatMap(kc -> parser.lift(kc, Parsed_Knowedge_Expression))
        .getOptionalValue();
  }

  // This should be its own module, as it knows SPARLQ and Jena
  private KnowledgeCarrier bind(KnowledgeCarrier paramQuery, Map<String, Object> bindings) {
    ParameterizedSparqlString paramQ = (ParameterizedSparqlString) ((DocumentCarrier)paramQuery).getStructuredExpression();
    bindings.forEach((key, value) -> {
      if (value instanceof URI) {
        paramQ.setParam(key, ResourceFactory.createResource(value.toString()));
      } else {
        paramQ.setLiteral(key, value.toString());
      }
    });
    return new DocumentCarrier()
        .withStructuredExpression(paramQ.asQuery())
        .withRepresentation(paramQuery.getRepresentation());
  }

  // This should be its own module, as it knows SPARLQ and Jena
  private Set<Map<String, String>> askQuery(
      UUID vocabularyId,
      String versionTag,
      KnowledgeCarrier query) {
    return ensureLoaded(vocabularyId, versionTag)
        .flatMap(m -> m.asParseTree(Model.class))
        .flatMap(m ->
            query.asParseTree(Query.class)
                .map(q -> JenaUtil.askQueryResults(m, q)))
        .orElse(Collections.emptySet());
  }



    // This is proper business logic of the Terms Server
  private KnowledgeCarrier loadQuery(String path) {
    KnowledgeCarrier binary = AbstractCarrier.of(TermsServer.class.getResourceAsStream(path))
        .withRepresentation(rep(SPARQL_1_1));
    return parser.lift(binary, Parsed_Knowedge_Expression)
        .orElseThrow(() -> new BeanInitializationException("Unable to load necessary query from " + path));
  }

  private KnowledgeCarrier getQuery(KnowledgeAsset vocMetadata, String labelFilter) {
    KnowledgeCarrier paramQuery = selectParametricQuery(vocMetadata);
    return bind(paramQuery,getBindings(vocMetadata,labelFilter));
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

  private List<ConceptIdentifier> buildTerms(Set<Map<String, String>> uriToLabelMap) {
    return uriToLabelMap.stream()
        .map(m -> new ConceptIdentifier()
            .withConceptId(URI.create(m.get("uri")))
            .withTag(NameUtils.getTrailingPart(m.get("uri")))
            .withLabel(m.get("label")))
        .collect(Collectors.toList());
  }

}
