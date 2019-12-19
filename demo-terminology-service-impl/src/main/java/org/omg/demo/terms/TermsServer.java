/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.omg.demo.terms;

import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Parsed_Knowedge_Expression;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.demo.terms.components.SparqlQueryBinder.OPERATOR_ID;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.knowledgebase.v3.server.IntrospectionApiInternal;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.knowledgebase.v3.server.ReasoningApiInternal;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.ontology.taxonomies.lexicon.LexiconSeries;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.api4kp.terms.v2.server.TermsApiInternal;
import org.omg.demo.terms.internal.TermsBuilder;
import org.omg.demo.terms.internal.TermsQueryType;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.beans.factory.BeanInitializationException;

@Named
@KPServer
public class TermsServer implements TermsApiInternal {

  @Inject
  @KPServer
  // The catalog provides metadata for the terminologies that this server can leverage
  private KnowledgeAssetRepositoryService knowledgeAssetCatalog;

  @Inject
  // Load and perform reasoning with terminology systems
  private KnowledgeBaseApiInternal termsKB;
  @Inject
  private ReasoningApiInternal._askQuery inquirer;

  @Inject
  @KPSupport(SPARQL_1_1)
  // Parse...
  private DeserializeApiInternal._lift sparqlParser;
  @Inject
  // ... and parametrize queries to be submitted to the KB
  private IntrospectionApiInternal._bind binder;

  @Inject
  private TermsBuilder termsBuilder;

  public TermsServer() {
    //
  }


  @Override
  public Answer<List<Pointer>> listTerminologies() {
    return
        knowledgeAssetCatalog.listKnowledgeAssets(
            Formal_Ontology.getTag(),
            null,
            0, -1);
  }

  @Override
  public Answer<List<ConceptIdentifier>> getTerms(
      UUID vocabularyId, String versionTag,
      String labelFilter) {
    return knowledgeAssetCatalog.getVersionedKnowledgeAsset(vocabularyId, versionTag)
        .flatMap(vocabularyMetadata -> getTermsForVocabulary(vocabularyMetadata, labelFilter));
  }

  private Answer<List<ConceptIdentifier>> getTermsForVocabulary(KnowledgeAsset vocabularyMetadata,
      String labelFilter) {
    TermsQueryType queryType = detectQueryType(vocabularyMetadata);
    return termsKB.initKnowledgeBase(vocabularyMetadata)
        .flatMap(kBase ->
            getQuery(vocabularyMetadata, labelFilter, queryType)
                .flatMap(boundQuery ->
                    doQuery(kBase, boundQuery, queryType)));
  }

  private Answer<List<ConceptIdentifier>> doQuery(
      KnowledgeBase kBase,
      KnowledgeCarrier query,
      TermsQueryType queryType) {
    return inquirer.askQuery(null, kBase, query)
        .map(answer -> termsBuilder.buildTerms(answer, queryType));
  }


  private Answer<KnowledgeCarrier> getQuery(
      KnowledgeAsset vocMetadata,
      String labelFilter,
      TermsQueryType queryType) {
    KnowledgeCarrier paramQuery = loadParametricQuery(queryType.getSourceURL());
    return binder.bind(OPERATOR_ID, paramQuery, getBindings(vocMetadata, labelFilter));
  }

  private KnowledgeCarrier loadParametricQuery(String path) {
    KnowledgeCarrier binary = AbstractCarrier
        .of(TermsServer.class.getResourceAsStream(path))
        .withRepresentation(rep(SPARQL_1_1));
    return sparqlParser.lift(binary, Parsed_Knowedge_Expression)
        .orElseThrow(
            () -> new BeanInitializationException("Unable to load necessary query from " + path));
  }

  private Bindings getBindings(KnowledgeAsset vocMetadata, String labelFilter) {
    Bindings bindings = new Bindings();
    if (labelFilter != null) {
      bindings.put("?label", labelFilter);
    }
    bindings.put("?vocabulary",
        ((URIIdentifier) vocMetadata.getSecondaryId().get(0)).getUri());
    return bindings;
  }

  private TermsQueryType detectQueryType(KnowledgeAsset vocMetadata) {
    // TODO: discuss how to generalize this
    ComputableKnowledgeArtifact cka =
        (ComputableKnowledgeArtifact) vocMetadata.getCarriers().get(0);
    Representation representation = cka.getRepresentation();

    return Arrays.stream(TermsQueryType.values())
        .flatMap(queryType -> queryType.appliesTo(cka.getLocator()))
        .findAny()
        .orElse(representation.getLexicon().contains(LexiconSeries.SKOS)
            ? TermsQueryType.SKOS : TermsQueryType.OWL2);
  }


}
