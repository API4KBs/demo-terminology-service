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

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.util.Util;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.demo.terms.config.TermsTestConfig;
import org.omg.demo.terms.config.TermsTestHelper;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.UUIDentifier;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TermsTestConfig.class})
@EnableAutoConfiguration
public class TermsClientTest {

  @Inject
  @KPComponent
  TermsApi termsApi;

  @Inject
  TermsTestHelper helper;

  @Test
  void testServer() {
    // Publish a vocabulary to the Asset Repository supporting the Terminology Server
    helper.initializeRepositoryContent();

    // Discover the existing vocabularies
    Optional<List<Pointer>> vocabsAnswer =
        termsApi.listTerminologies().getOptionalValue();

    assertTrue(vocabsAnswer.isPresent());



    Optional<UUIDentifier> assetTag =
        vocabsAnswer.orElse(Collections.emptyList()).stream()
            .map(Pointer::getEntityRef)
            .map(DatatypeHelper::toUUIDentifier)
            .flatMap(Util::trimStream)
            .findFirst(); // we know there's only one

    // List the terms in a given vocabulary
    assertTrue(assetTag.isPresent());
    List<ConceptIdentifier> terms =
        termsApi.getTerms(assetTag.get().getUUID(), "1.0.0", null)
        .orElse(Collections.emptyList());

    assertTrue(terms.stream()
        .map(ConceptIdentifier::getLabel)
        .anyMatch("Class A"::equalsIgnoreCase));

  }


}


