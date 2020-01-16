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

import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries;
import edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevel;
import java.io.ByteArrayInputStream;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.ExpressionCarrier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@Named
@KPOperation(KnowledgeProcessingOperationSeries.Lifting_Task)
@KPOperation(KnowledgeProcessingOperationSeries.Lowering_Task)
@KPSupport(OWL_2)
public class JenaOwlRdfLifter implements
    DeserializeApiInternal._lift {


  @Override
  public Answer<KnowledgeCarrier> lift(KnowledgeCarrier sourceArtifact,
      ParsingLevel level) {
    switch (sourceArtifact.getLevel().asEnum()) {
      case Encoded_Knowledge_Expression:
        switch (level.asEnum()) {
          case Encoded_Knowledge_Expression:
            return Answer.of(sourceArtifact);
          case Concrete_Knowledge_Expression:
            String str = new String(((BinaryCarrier) sourceArtifact).getEncodedExpression());
            return Answer.of(new ExpressionCarrier()
                .withSerializedExpression(str)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage(),
                    sourceArtifact.getRepresentation().getSerialization(),
                    TXT)));
          case Parsed_Knowedge_Expression:
            Model m = sourceArtifact.getRepresentation().getLanguage()
                == OWL_2
                ? ModelFactory.createOntologyModel()
                : ModelFactory.createDefaultModel();
            m.read(
                new ByteArrayInputStream(((BinaryCarrier) sourceArtifact).getEncodedExpression()),
                null);
            return Answer.of(new DocumentCarrier()
                .withStructuredExpression(m)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage())));
          case Abstract_Knowledge_Expression:
          default:
            throw new UnsupportedOperationException();
        }
      case Concrete_Knowledge_Expression:
        switch (level.asEnum()) {
          case Encoded_Knowledge_Expression:
            throw new IllegalStateException();
          case Concrete_Knowledge_Expression:
            return Answer.of(sourceArtifact);
          case Parsed_Knowedge_Expression:
            Model m = sourceArtifact.getRepresentation().getLanguage()
                == OWL_2
                ? ModelFactory.createOntologyModel()
                : ModelFactory.createDefaultModel();
            m.read(
                new ByteArrayInputStream(
                    ((ExpressionCarrier) sourceArtifact).getSerializedExpression().getBytes()),
                null);
            return Answer.of(new DocumentCarrier()
                .withStructuredExpression(m)
                .withLevel(level)
                .withRepresentation(rep(sourceArtifact.getRepresentation().getLanguage())));
          case Abstract_Knowledge_Expression:
          default:
            throw new UnsupportedOperationException();
        }
      case Parsed_Knowedge_Expression:
        return Answer.of(sourceArtifact);
      default:
        throw new UnsupportedOperationException();
    }
  }


}
