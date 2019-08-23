/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.omg.demo.terms.components;

import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.RDF_1_1;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.TXT;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.tranx.server.DeserializeApiDelegate;
import edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations._20190801.KnowledgeProcessingOperation;
import edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
public class JenaOWLRDFParser implements
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
    switch (sourceArtifact.getLevel()) {
      case Encoded_Knowledge_Expression:
        switch (level) {
          case Encoded_Knowledge_Expression:
            return succeed(sourceArtifact);
          case Concrete_Knowledge_Expression:
            String str = new String(((BinaryCarrier) sourceArtifact).getEncodedExpression());
            return succeed(new ExpressionCarrier()
                .withSerializedExpression(str)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage(),
                    sourceArtifact.getRepresentation().getSerialization(),
                    TXT)));
          case Parsed_Knowedge_Expression:
            Model m = sourceArtifact.getRepresentation().getLanguage()
                == KnowledgeRepresentationLanguage.OWL_2
                ? ModelFactory.createOntologyModel()
                : ModelFactory.createDefaultModel();
            m.read(
                new ByteArrayInputStream(((BinaryCarrier) sourceArtifact).getEncodedExpression()),
                null);
            return succeed(new DocumentCarrier()
                .withStructuredExpression(m)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage())));
          case Abstract_Knowledge_Expression:
          default:
            throw new UnsupportedOperationException();
        }
      case Concrete_Knowledge_Expression:
        switch (level) {
          case Encoded_Knowledge_Expression:
            throw new IllegalStateException();
          case Concrete_Knowledge_Expression:
            return succeed(sourceArtifact);
          case Parsed_Knowedge_Expression:
            Model m = sourceArtifact.getRepresentation().getLanguage()
                == KnowledgeRepresentationLanguage.OWL_2
                ? ModelFactory.createOntologyModel()
                : ModelFactory.createDefaultModel();
            m.read(
                new ByteArrayInputStream(((ExpressionCarrier) sourceArtifact).getSerializedExpression().getBytes()),
                null);
            return succeed(new DocumentCarrier()
                .withStructuredExpression(m)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage())));
          case Abstract_Knowledge_Expression:
          default:
            throw new UnsupportedOperationException();
        }
      case Parsed_Knowedge_Expression:
        return succeed(sourceArtifact);
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
        Collections.singletonList(
            rep(KnowledgeRepresentationLanguage.OWL_2, RDF_1_1));
  }

}
