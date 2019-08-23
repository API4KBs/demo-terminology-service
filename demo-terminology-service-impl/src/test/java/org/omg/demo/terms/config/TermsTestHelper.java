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
package org.omg.demo.terms.config;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.uri;
import static edu.mayo.kmdp.id.helper.DatatypeHelper.vuri;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;
import static edu.mayo.kmdp.util.Util.toUUID;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._20190801.KnowledgeAssetCategory.Terminology_Ontology_And_Assertional_KBs;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.OWL_2;
import static edu.mayo.ontology.taxonomies.krserialization._20190801.KnowledgeRepresentationLanguageSerialization.RDF_XML_Syntax;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.canonicalRepresentationOf;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public class TermsTestHelper {

  private static final URI ontologyURI = URI.create("http://www.test.com/foo");
  private static String ontologyVersion = "1.0.0";

  private static String path = "/fooVocab.rdf";

  private static UUID assetUUID = UUID.nameUUIDFromBytes(ontologyURI.toString().getBytes());
  private static UUID artifactUUID = UUID.nameUUIDFromBytes(path.getBytes());

  private static URIIdentifier testOntologyId = vuri(
      BASE_UUID_URN + assetUUID,
      BASE_UUID_URN + assetUUID + ":" + ontologyVersion
      );

  private static URIIdentifier testDocumentId = vuri(
      BASE_UUID_URN + artifactUUID,
      BASE_UUID_URN + artifactUUID + ":" + ontologyVersion
  );


  @Inject
  KnowledgeAssetRepositoryApi assetRepo;
  @Inject
  KnowledgeAssetCatalogApi catalog;


  public void initializeRepositoryContent() {

    KnowledgeAsset metadata = new KnowledgeAsset()
        .withAssetId(testOntologyId)
        .withSecondaryId(uri("http://www.test.com/foo", "1.0.0"))
        .withName("Test")
        .withDescription("A test vocabulary")
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Formal_Ontology)
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(testDocumentId)
            .withRepresentation(new Representation()
                .withLanguage(OWL_2)
                .withSerialization(RDF_XML_Syntax)
                .withFormat(XML_1_1))
            .withLocator(URI.create(path))
        );

    KnowledgeCarrier carrier = AbstractCarrier
        .of(TermsTestHelper.class.getResourceAsStream(path))
        .withAssetId(testOntologyId)
        .withArtifactId(testDocumentId)
        .withRepresentation(canonicalRepresentationOf(metadata));

    publish(metadata,carrier);
  }

  private void publish(KnowledgeAsset surrogate, KnowledgeCarrier artifact) {

    VersionedIdentifier surrogateId = surrogate.getAssetId();
    VersionedIdentifier artifactId = artifact.getArtifactId();

    catalog.setVersionedKnowledgeAsset(
            toUUID(surrogateId.getTag()),
            surrogateId.getVersion(),
            surrogate);

    assetRepo.setKnowledgeAssetCarrierVersion(
        toUUID(surrogateId.getTag()),
        surrogateId.getVersion(),
        toUUID(artifactId.getTag()),
        artifactId.getVersion(),
        ((BinaryCarrier) artifact).getEncodedExpression());
  }
}
