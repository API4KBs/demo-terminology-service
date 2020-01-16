/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.omg.demo.terms;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.toVersionIdentifier;
import static edu.mayo.kmdp.id.helper.DatatypeHelper.vid;
import static edu.mayo.kmdp.util.Util.toUUID;
import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Parsed_Knowedge_Expression;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@Named
public class TermsKnowledgeBaseManager implements KnowledgeBaseApiInternal {
// TODO Pending more work on the APIs, some of this implementation may be promoted to
//  a parent abstract class "KnowledgeBaseManager"


  @Inject
  @KPSupport(OWL_2)
  private DeserializeApiInternal._lift parser;

  @Inject
  @KPSupport(SPARQL_1_1)
  private _bind sparqlBinder;

  @Inject
  @KPServer
  private KnowledgeAssetRepositoryService assetRepository;


  private Map<VersionedIdentifier, KnowledgeBase> knowledgeBases = new HashMap<>();


  @Override
  public Answer<Pointer> bind(UUID kbaseId, String versionTag, Bindings bindings) {
    return sparqlBinder.bind(kbaseId, versionTag, bindings);
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag) {
    VersionedIdentifier vid = new VersionIdentifier()
        .withTag(kbaseId.toString())
        .withVersion(versionTag);
    return Answer.of(knowledgeBases.get(vid));
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeAsset asset) {
    // TODO: casts will no longer be needed in 6.x
    VersionedIdentifier vid = toVersionIdentifier(asset.getAssetId());

    if (!knowledgeBases.containsKey(vid)) {
      ComputableKnowledgeArtifact cka =
          (ComputableKnowledgeArtifact) asset.getCarriers().get(0);

      Answer<KnowledgeBase> ans = isLocal(cka)
          ? initLocalKB(vid)
          : initRemoteKB(cka.getLocator());

      if (ans.isSuccess()) {
        knowledgeBases.put(vid, ans.get());
      }
      return ans.map(kb -> new Pointer()
          .withEntityRef(kb.getKbaseId()));
    }
    return Answer.of(new Pointer()
        .withEntityRef(asset.getAssetId()));
  }

  private Answer<KnowledgeBase> initRemoteKB(URI endpoint) {
    return Answer.of(new KnowledgeBase()
        .withEndpoint(endpoint));
  }

  private Answer<KnowledgeBase> initLocalKB(VersionedIdentifier vid) {
    return loadVocabulary(vid)
        .flatMap(kc -> populateKnowledgeBase(Util.toUUID(vid.getTag()), vid.getVersion(), kc)
            .map(DatatypeHelper::deRef)
            .flatMap(kbId -> getKnowledgeBase(uuid(kbId.getTag()), kbId.getVersion())));
  }

  private boolean isLocal(ComputableKnowledgeArtifact asset) {
    return asset.getLocator() == null
        || Util.isEmpty(asset.getLocator().getScheme());
  }


  @Override
  public Answer<Pointer> populateKnowledgeBase(UUID kbaseId, String versionTag,
      KnowledgeCarrier sourceArtifact) {
    VersionedIdentifier key = vid(kbaseId.toString(), versionTag);
    return this.loadVocabulary(key)
        .map(kc -> new KnowledgeBase()
            .withKbaseId(DatatypeHelper.uri(kbaseId.toString(), versionTag))
            .withManifestation(kc))
        .map(kb -> knowledgeBases.put(key, kb))
        .map(KnowledgeBase::getKbaseId)
        .map(kbId -> new Pointer()
            .withEntityRef(kbId));
  }

  private Answer<KnowledgeCarrier> loadVocabulary(VersionedIdentifier v) {
    return assetRepository
        .getCanonicalKnowledgeAssetCarrier(toUUID(v.getTag()), v.getVersion(), null)
        .flatMap(kc -> parser.lift(kc, Parsed_Knowedge_Expression));
  }

}
