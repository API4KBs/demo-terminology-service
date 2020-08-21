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

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;

@Named
public class TermsKnowledgeBaseManager extends KnowledgeBaseProvider {

  @Inject
  @KPSupport(OWL_2)
  private DeserializeApiInternal._applyLift ontologyParser;

  @Autowired
  public TermsKnowledgeBaseManager(
      @KPServer KnowledgeAssetRepositoryApiInternal assetRepository) {
    super(assetRepository);
  }


  protected KnowledgeBase configureAsLocalKB(KnowledgeBase kBase, KnowledgeCarrier kc) {
    if (kc.getRepresentation().getLanguage().sameAs(OWL_2)) {
      return super.configureAsLocalKB(kBase,
          ontologyParser.applyLift(kc, Concrete_Knowledge_Expression, null, null)
              .orElseThrow(IllegalArgumentException::new));
    }
    return super.configureAsLocalKB(kBase, kc);
  }


}
