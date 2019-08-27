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
package org.omg.demo.terms.temp;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.vid;
import static edu.mayo.kmdp.util.Util.toUUID;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel.Parsed_Knowedge_Expression;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.tranx.DeserializeApi;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.stereotype.Component;

@Component
public class TermsKnowledgeBase implements KnowledgeBaseService {

  @Inject
  @KPComponent
  private DeserializeApi parser;

  @Inject
  private QueryService inquirer;

  @Inject
  @KPComponent
  private KnowledgeAssetRepositoryApi repository;

  private Map<VersionedIdentifier, Optional<KnowledgeCarrier>> knowledgeBases = new HashMap<>();

  @Override
  public Set<Map<String, String>> askQuery(
      UUID kbaseId, String versionTag,
      KnowledgeCarrier query) {
    return ensureLoaded(kbaseId,versionTag)
        .map(kbase -> inquirer.askQuery(kbase, query))
        .orElse(Collections.emptySet());
  }

  // These belong in a KnowledgeBaseManager API
  private Optional<KnowledgeCarrier> ensureLoaded(UUID assetId, String versionTag) {
    return knowledgeBases.computeIfAbsent(
        vid(assetId.toString(), versionTag),
        this::loadVocabulary);
  }

  private Optional<KnowledgeCarrier> loadVocabulary(VersionedIdentifier v) {
    return repository
        .getCanonicalKnowledgeAssetCarrier(toUUID(v.getTag()), v.getVersion(), null)
        .flatMap(kc -> parser.lift(kc, Parsed_Knowedge_Expression))
        .getOptionalValue();
  }

}
