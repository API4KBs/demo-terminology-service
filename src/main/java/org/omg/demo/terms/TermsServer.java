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
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.inference.v3.server.QueryApiInternal._askQuery;
import edu.mayo.kmdp.knowledgebase.v3.server.BindingApiInternal._bind;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.terms.v4.server.TermsApiInternal;
import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.lexicon.LexiconSeries;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.demo.terms.config.TermsPublisher;
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
  private KnowledgeBaseApiInternal termsKBManager;

  @Inject
  @KPSupport(SPARQL_1_1)
  private _bind binder;

  @Inject
  private _askQuery inquirer;

  @Inject
  @KPSupport(SPARQL_1_1)
  // Parse...
  private DeserializeApiInternal._lift sparqlParser;

  @Inject
  private TermsBuilder termsBuilder;

  @Inject
  private TermsPublisher initializer;

  public TermsServer() {
    //
  }

  @PostConstruct
  public void populateOnInit() {
    initializer.initializeRepositoryContent();
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
  public Answer<Void> relatesTo(UUID vocabularyId, String versionTag, String conceptId,
      String relationshipId) {
    return Answer.unsupported();
  }

  @Override
  public Answer<ConceptIdentifier> getTerm(UUID vocabularyId, String versionTag, String conceptId) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<ConceptIdentifier>> getTerms(
      UUID vocabularyId, String versionTag,
      String labelFilter) {
    return knowledgeAssetCatalog.getVersionedKnowledgeAsset(vocabularyId, versionTag)
        .flatMap(vocabularyMetadata -> getTermsForVocabulary(vocabularyMetadata, labelFilter));
  }

  @Override
  public Answer<KnowledgeCarrier> getVocabulary(UUID vocabularyId, String versionTag,
      String xAccept) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Void> isMember(UUID vocabularyId, String versionTag, String conceptExpression) {
    return Answer.unsupported();
  }


  private Answer<List<ConceptIdentifier>> getTermsForVocabulary(KnowledgeAsset vocabularyMetadata,
      String labelFilter) {
    TermsQueryType queryType = detectQueryType(vocabularyMetadata);
    return termsKBManager.initKnowledgeBase(vocabularyMetadata)
        .map(DatatypeHelper::deRef)  // TODO this will no longer necessary in 6.x
        .flatMap(kBaseId ->
            getQuery(vocabularyMetadata, labelFilter, queryType)
                .flatMap(boundQuery ->
                    doQuery(kBaseId, boundQuery, queryType)));
  }


  private Answer<List<ConceptIdentifier>> doQuery(
      VersionedIdentifier kBaseId,
      KnowledgeCarrier query,
      TermsQueryType queryType) {
    return inquirer.askQuery(Util.toUUID(kBaseId.getTag()), kBaseId.getVersion(), query)
        .map(answer -> termsBuilder.buildTerms(answer, queryType));
  }


  private Answer<KnowledgeCarrier> getQuery(
      KnowledgeAsset vocMetadata,
      String labelFilter,
      TermsQueryType queryType) {
    KnowledgeCarrier paramQuery = loadParametricQuery(queryType.getSourceURL());
    Bindings bindings = getBindings(vocMetadata, labelFilter);

    //TODO Should binding variables to a query more lightweight than setting up a KB?
    // Or should the parametric query be kept as a named KB, and bound & returned each time? <-- pref.

    VersionedIdentifier kbId = paramQuery.getAssetId();
    Answer<KnowledgeBase> paramQueryKb =
        termsKBManager.getKnowledgeBase(Util.toUUID(kbId.getTag()), kbId.getVersion());

    if (!paramQueryKb.isSuccess()) {
      termsKBManager.initKnowledgeBase(new KnowledgeAsset().withAssetId(paramQuery.getAssetId()))
          .map(DatatypeHelper::deRef)
          .flatMap(newKbId ->
              termsKBManager.populateKnowledgeBase(Util.toUUID(newKbId.getTag()), newKbId.getVersion(), paramQuery));
    }

    // TODO fix the identifiers so that this chain is simpler and smoother
    return binder.bind(Util.toUUID(kbId.getTag()), kbId.getVersion(), bindings)
        .map(DatatypeHelper::deRef)
        .flatMap(queryBasekbId -> termsKBManager
            .getKnowledgeBase(Util.toUUID(queryBasekbId.getTag()),queryBasekbId.getVersion()))
        .map(KnowledgeBase::getManifestation);
  }

  private KnowledgeCarrier loadParametricQuery(String path) {
    KnowledgeCarrier binary = AbstractCarrier
        .of(TermsServer.class.getResourceAsStream(path))
        .withRepresentation(rep(SPARQL_1_1))
        .withAssetId(DatatypeHelper.uri(Registry.BASE_UUID_URN, Util.uuid(path).toString(),"0.0.0"));

    return sparqlParser.lift(binary, Parsed_Knowedge_Expression)
        // TODO : carrying over the IDs is a responsibility of the lifter
        .map(kc -> kc.withAssetId(binary.getAssetId()))
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
