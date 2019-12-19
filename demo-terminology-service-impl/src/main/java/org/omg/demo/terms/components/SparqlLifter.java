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


import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries;
import edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevel;
import javax.inject.Named;
import org.apache.jena.query.ParameterizedSparqlString;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.ASTCarrier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.ExpressionCarrier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@Named
@KPOperation(KnowledgeProcessingOperationSeries.Lifting_Task)
@KPOperation(KnowledgeProcessingOperationSeries.Lowering_Task)
@KPSupport(SPARQL_1_1)
public class SparqlLifter implements
    DeserializeApiInternal._lift {


  @Override
  public Answer<KnowledgeCarrier> lift(KnowledgeCarrier sourceArtifact,
      ParsingLevel level) {
    String sparql;
    switch (sourceArtifact.getLevel().asEnum()) {
      case Encoded_Knowledge_Expression:
        sparql = new String(((BinaryCarrier) sourceArtifact).getEncodedExpression());
        break;
      case Concrete_Knowledge_Expression:
        sparql = ((ExpressionCarrier) sourceArtifact).getSerializedExpression();
        break;
      default:
        throw new UnsupportedOperationException();
    }
    switch (level.asEnum()) {
      case Concrete_Knowledge_Expression:
        return Answer.of(new ExpressionCarrier()
            .withSerializedExpression(sparql)
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1, TXT)));
      case Parsed_Knowedge_Expression:
        return Answer.of(new DocumentCarrier()
            .withStructuredExpression(new ParameterizedSparqlString(sparql))
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1)));
      case Abstract_Knowledge_Expression:
        return Answer.of(new ASTCarrier()
            .withParsedExpression(new ParameterizedSparqlString(sparql).asQuery())
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1)));

      default:
        throw new UnsupportedOperationException();

    }
  }


}
