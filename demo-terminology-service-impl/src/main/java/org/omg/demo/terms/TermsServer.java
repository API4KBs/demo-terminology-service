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

import edu.mayo.kmdp.util.ws.ResponseHelper;
import java.util.Collections;
import java.util.List;
import org.omg.demo.terms.server.TermsApiDelegate;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.http.ResponseEntity;

@KPServer
public class TermsServer implements TermsApiDelegate {

  @Override
  public ResponseEntity<List<ConceptIdentifier>> getTerms(String vocabularyId, String label) {
    return ResponseHelper.succeed(Collections.emptyList());
  }

  @Override
  public ResponseEntity<List<Pointer>> listTerminologies() {
    return ResponseHelper.succeed(Collections.emptyList());
  }
}
