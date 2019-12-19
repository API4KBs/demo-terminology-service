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
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel.Parsed_Knowedge_Expression;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

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
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@Named
public class TermsKnowledgeBaseManager implements KnowledgeBaseApiInternal {

  @Inject
  @KPSupport(OWL_2)
  private DeserializeApiInternal._lift parser;

  @Inject
  @KPServer
  private KnowledgeAssetRepositoryService assetRepository;


  private Map<VersionedIdentifier, KnowledgeBase> knowledgeBases = new HashMap<>();

  @Override
  public Answer<KnowledgeBase> initKnowledgeBase(KnowledgeAsset asset) {
    VersionedIdentifier vid = DatatypeHelper.toVersionIdentifier(asset.getAssetId());

    if (knowledgeBases.containsKey(vid)) {
      return Answer.of(knowledgeBases.get(vid));
    }

    ComputableKnowledgeArtifact cka =
        (ComputableKnowledgeArtifact) asset.getCarriers().get(0);

    Answer<KnowledgeBase> ans = isLocal(cka)
        ? initLocalKB(vid)
        : initRemoteKB(cka.getLocator());

    if (ans.isSuccess()) {
      knowledgeBases.put(vid,ans.get());
    }

    return ans;
  }

  private Answer<KnowledgeBase> initRemoteKB(URI endpoint) {
    return Answer.of(new KnowledgeBase()
        .withEndpoint(endpoint));
  }

  private Answer<KnowledgeBase> initLocalKB(VersionedIdentifier vid) {
    return loadVocabulary(vid)
        .flatMap(kc -> populateKnowledgeBase(Util.toUUID(vid.getTag()), vid.getVersion(), kc));
  }

  private boolean isLocal(ComputableKnowledgeArtifact asset) {
    return asset.getLocator() == null
        || Util.isEmpty(asset.getLocator().getScheme());
  }


  @Override
  public Answer<KnowledgeBase> populateKnowledgeBase(UUID kbaseId, String versionTag,
      KnowledgeCarrier sourceArtifact) {
    VersionedIdentifier key = vid(kbaseId.toString(), versionTag);
    return this.loadVocabulary(key)
        .map(kc -> new KnowledgeBase()
            .withKbaseId(DatatypeHelper.uri(kbaseId.toString(),versionTag))
            .withManifestation(kc));
  }

  private Answer<KnowledgeCarrier> loadVocabulary(VersionedIdentifier v) {
    return assetRepository
        .getCanonicalKnowledgeAssetCarrier(toUUID(v.getTag()), v.getVersion(), null)
        .flatMap(kc -> parser.lift(kc, Parsed_Knowedge_Expression));
  }

}
