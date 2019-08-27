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

import edu.mayo.kmdp.util.JenaUtil;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.stereotype.Component;

@Component
public class JenaQuery implements QueryService {

  // This should be its own module, as it knows SPARLQ and Jena
  public Set<Map<String, String>> askQuery(
      KnowledgeCarrier kbase,
      KnowledgeCarrier query) {
    return kbase
        .flatMap(m -> m.asParseTree(Model.class))
        .flatMap(m ->
            query.asParseTree(Query.class)
                .map(q -> JenaUtil.askQueryResults(m, q)))
        .orElse(Collections.emptySet());
  }
}
