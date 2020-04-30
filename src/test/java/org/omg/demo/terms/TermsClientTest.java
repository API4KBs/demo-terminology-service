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

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.mayo.kmdp.terms.impl.model.ConceptDescriptor;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.omg.demo.terms.config.TermsPublisher;
import org.omg.demo.terms.config.TermsServerConfig;
import org.omg.demo.terms.config.TestConfig;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TermsServerConfig.class, TestConfig.class})
public class TermsClientTest {

  @Inject
  @KPServer
  TermsServer termsServer;

  @Test
  void testServer() {

    // Discover the existing vocabularies
    List<Pointer> vocabsAnswer =
        termsServer.listTerminologies()
            .orElse(emptyList());
    assertEquals(2, vocabsAnswer.size());

    for (Pointer entity : vocabsAnswer) {
      System.out.println(
          "vocabsAnswer name: " + entity.getName() + ", Type " + entity.getHref());
    }

    // List the terms in a given vocabulary
    List<ConceptDescriptor> termsLocal = termsServer
        .getTerms(TermsPublisher.ESWC_ASSET_UUID, TermsPublisher.ONTOLOGY_VERSION)
        .orElse(emptyList());

//    List<ConceptIdentifier> termsVirtuoso = termsServer
//        .getTerms(toUUID(assetId.getTag()), assetId.getVersion(), null)
//        .orElse(Collections.emptyList());

    List<ConceptDescriptor> termsDBpedia = termsServer
        .getTerms(TermsPublisher.DBPEDIA_ASSET_UUID, TermsPublisher.ONTOLOGY_VERSION, null)
        .orElse(emptyList());

    /*
     * Print Results:
     */
    System.out.println("\n-------------- Local Terms --------------- ");
    for (ConceptDescriptor conceptIdentifier : termsLocal) {
      System.out.println(
          "ConceptIdentifier label : " + conceptIdentifier.getName() + ". conceptIdentifier Tag : "
              + conceptIdentifier.getTag());
    }

//    System.out.println("\n-------------- Terms from Virtuoso (localhost) --------------- ");
//    for(ConceptIdentifier conceptIdentifier: termsVirtuoso)
//      System.out.println("ConceptIdentifier label : "+conceptIdentifier.getLabel()+". conceptIdentifier Tag : "+conceptIdentifier.getTag());

    System.out.println("\n-------------- Terms from dbpedia.org --------------- ");
    for (ConceptDescriptor conceptIdentifier : termsDBpedia) {
      System.out.println(
          "ConceptIdentifier label : " + conceptIdentifier.getName() + ". conceptIdentifier Tag : "
              + conceptIdentifier.getTag());
    }
  }


}


