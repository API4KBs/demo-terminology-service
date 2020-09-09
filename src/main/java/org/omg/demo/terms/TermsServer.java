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

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.toRuntimeSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SKOS;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.util.Util;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.demo.terms.config.TermsPublisher;
import org.omg.demo.terms.internal.TermsBuilder;
import org.omg.demo.terms.internal.TermsQueryType;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.QueryApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._bind;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
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
  private DeserializeApiInternal._applyLift sparqlParser;

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
            null,
            0, -1);
  }

  @Override
  public Answer<ConceptDescriptor> getTerm(UUID vocabularyId, String versionTag, String conceptId) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<ConceptDescriptor>> getTerms(
      UUID vocabularyId, String versionTag,
      String labelFilter) {
    return knowledgeAssetCatalog.getKnowledgeAssetVersion(vocabularyId, versionTag)
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


  private Answer<List<ConceptDescriptor>> getTermsForVocabulary(KnowledgeAsset vocabularyMetadata,
      String labelFilter) {
    TermsQueryType queryType = detectQueryType(vocabularyMetadata);
    return termsKBManager.initKnowledgeBase(toRuntimeSurrogate(vocabularyMetadata))
        .flatMap(kBaseId ->
            getQuery(vocabularyMetadata, labelFilter, queryType)
                .flatMap(boundQuery -> doQuery(kBaseId, boundQuery, queryType)));
  }


  private Answer<List<ConceptDescriptor>> doQuery(
      ResourceIdentifier kBaseId,
      KnowledgeCarrier query,
      TermsQueryType queryType) {
    return inquirer.askQuery(kBaseId.getUuid(), kBaseId.getVersionTag(), query)
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

    ResourceIdentifier kbId = paramQuery.getAssetId();
    Answer<KnowledgeBase> paramQueryKb =
        termsKBManager.getKnowledgeBase(kbId.getUuid(), kbId.getVersionTag());

    if (!paramQueryKb.isSuccess()) {
      termsKBManager.initKnowledgeBase(paramQuery);
    }

    // TODO fix the identifiers so that this chain is simpler and smoother
    return termsKBManager.bind(kbId.getUuid(), kbId.getVersionTag(), bindings)
        .flatMap(queryBasekbId -> termsKBManager
            .getKnowledgeBaseManifestation(queryBasekbId.getUuid(),queryBasekbId.getVersionTag()));
  }

  private KnowledgeCarrier loadParametricQuery(String path) {
    KnowledgeCarrier binary = AbstractCarrier
        .of(TermsServer.class.getResourceAsStream(path))
        .withRepresentation(rep(SPARQL_1_1))
        .withAssetId(SurrogateBuilder.assetId(Util.uuid(path), VERSION_ZERO));

    return sparqlParser.applyLift(binary, Concrete_Knowledge_Expression,null,null)
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
        vocMetadata.getSecondaryId().get(0).getResourceId());
    return bindings;
  }

  private TermsQueryType detectQueryType(KnowledgeAsset vocMetadata) {
    // TODO: discuss how to generalize this
    KnowledgeArtifact cka = vocMetadata.getCarriers().get(0);
    SyntacticRepresentation representation = cka.getRepresentation();

    return Arrays.stream(TermsQueryType.values())
        .flatMap(queryType -> queryType.appliesTo(cka.getLocator()))
        .findAny()
        .orElse(representation.getLexicon().stream().anyMatch(lex -> lex.sameAs(SKOS))
            ? TermsQueryType.SKOS : TermsQueryType.OWL2);
  }


}
