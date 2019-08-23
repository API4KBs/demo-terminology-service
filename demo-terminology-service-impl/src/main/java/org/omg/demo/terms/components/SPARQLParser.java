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

import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.SPARQL_1_1;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.tranx.server.DeserializeApiDelegate;
import edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations._20190801.KnowledgeProcessingOperation;
import edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;
import org.apache.jena.query.ParameterizedSparqlString;
import org.omg.spec.api4kp._1_0.services.ASTCarrier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.ExpressionCarrier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.springframework.http.ResponseEntity;

@Named
@KPOperation(KnowledgeProcessingOperation.Lifting_Task)
@KPOperation(KnowledgeProcessingOperation.Lowering_Task)
public class SPARQLParser implements
    DeserializeApiDelegate {

  @Override
  public ResponseEntity<KnowledgeCarrier> ensureRepresentation(KnowledgeCarrier sourceArtifact,
      SyntacticRepresentation into) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> deserialize(KnowledgeCarrier sourceArtifact,
      SyntacticRepresentation into) {
    return null;
  }

  @Override
  public ResponseEntity<List<SyntacticRepresentation>> getParsableLanguages() {
    return succeed(getSupportedRepresentations());
  }

  @Override
  public ResponseEntity<List<SyntacticRepresentation>> getSerializableLanguages() {
    return succeed(getSupportedRepresentations());
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> lift(KnowledgeCarrier sourceArtifact,
      ParsingLevel level) {
    String sparql;
    switch (sourceArtifact.getLevel()) {
      case Encoded_Knowledge_Expression:
        sparql = new String(((BinaryCarrier) sourceArtifact).getEncodedExpression());
        break;
      case Concrete_Knowledge_Expression:
        sparql = ((ExpressionCarrier) sourceArtifact).getSerializedExpression();
        break;
      default:
        throw new UnsupportedOperationException();
    }
    switch (level) {
      case Concrete_Knowledge_Expression:
        return succeed(new ExpressionCarrier()
            .withSerializedExpression(sparql)
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1, TXT)));
      case Parsed_Knowedge_Expression:
        return succeed(new DocumentCarrier()
            .withStructuredExpression(new ParameterizedSparqlString(sparql))
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1)));
      case Abstract_Knowledge_Expression:
        return succeed(new ASTCarrier()
            .withParsedExpression(new ParameterizedSparqlString(sparql).asQuery())
            .withLevel(level)
            .withRepresentation(rep(SPARQL_1_1)));

      default:
        throw new UnsupportedOperationException();

    }
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> lower(KnowledgeCarrier sourceArtifact,
      ParsingLevel level) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> serialize(KnowledgeCarrier sourceArtifact,
      SyntacticRepresentation into) {
    return null;
  }

  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return
        Collections
            .singletonList(rep(SPARQL_1_1, TXT, Charset.defaultCharset().name()));
  }

}
