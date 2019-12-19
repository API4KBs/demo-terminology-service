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
package org.omg.demo.terms.components;

import edu.mayo.kmdp.knowledgebase.v3.server.IntrospectionApiInternal;
import java.net.URI;
import java.util.UUID;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.stereotype.Component;

@Component
public class SparqlQueryBinder implements IntrospectionApiInternal._bind {

  public static UUID OPERATOR_ID = UUID.fromString("73d9abfb-5192-45e8-8368-3ac7f5067e9b");

  @Override
  public Answer<KnowledgeCarrier> bind(UUID lambdaId, KnowledgeCarrier paramQuery,
      Bindings bindings) {
    if (! OPERATOR_ID.equals(lambdaId)) {
      return Answer.failed(new IllegalArgumentException());
    }

    ParameterizedSparqlString paramQ = (ParameterizedSparqlString) ((DocumentCarrier) paramQuery)
        .getStructuredExpression();
    bindings.forEach((key, value) -> {
      if (value instanceof URI) {
        paramQ.setParam(key, ResourceFactory.createResource(value.toString()));
      } else {
        paramQ.setLiteral(key, value.toString());
      }
    });
    return Answer.of(new DocumentCarrier()
        .withStructuredExpression(paramQ.asQuery())
        .withRepresentation(paramQuery.getRepresentation()));
  }

}
