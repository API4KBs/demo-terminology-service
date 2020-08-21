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

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SKOS;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

@Named
public class TermsPublisher {

  @Inject
  @KPServer
  KnowledgeAssetRepositoryService assetRepo;

  public static final URI ENDPOINT_LOCAL_VIRTUOSO     = URI.create("http://localhost:8890/sparql/");
  public static final URI ENDPOINT_DBPEDIA            = URI.create("https://dbpedia.org/sparql");
  public static final String LOCAL_PATH               = "/rdf/eswc20060921.rdf";

  public static final URI ESWC_ONTOLOGY_URI           = URI.create("http://www.eswc2006.org/technologies/ontology");
  public static final UUID ESWC_ASSET_UUID            = UUID.nameUUIDFromBytes(ESWC_ONTOLOGY_URI.toString().getBytes());

  public static final URI DBPEDIA_ONTOLOGY_URI        = URI.create("https://dbpedia.org");
  public static final UUID DBPEDIA_ASSET_UUID         = UUID.nameUUIDFromBytes(DBPEDIA_ONTOLOGY_URI.toString().getBytes());

  public static final String ONTOLOGY_VERSION = "1.0.0";

  public void initializeRepositoryContent() {
    addLocalOntology();
    addDBPediaVocabulary();
  }

  public void addDBPediaVocabulary() {

    UUID artifactUUID = UUID.nameUUIDFromBytes(DBPEDIA_ASSET_UUID.toString().getBytes());

    ResourceIdentifier testOntologyId = SemanticIdentifier.newId(
        DBPEDIA_ASSET_UUID,
        ONTOLOGY_VERSION
    );

    ResourceIdentifier testDocumentId = SemanticIdentifier.newId(
        artifactUUID,
        ONTOLOGY_VERSION
    );

    KnowledgeAsset metadata = new KnowledgeAsset()
        .withAssetId(testOntologyId)
        .withSecondaryId(SemanticIdentifier.newId(DBPEDIA_ONTOLOGY_URI.toString(), ONTOLOGY_VERSION))
        .withName("DBPedia")
        .withDescription("DBPedia Vocabularies")
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Formal_Ontology)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(testDocumentId)
            .withRepresentation(rep(OWL_2,RDF_XML_Syntax,XML_1_1)
                .withLexicon(SKOS))
            .withLocator(ENDPOINT_DBPEDIA)
        );


    assetRepo.publish(metadata,null);
  }


  public void addLocalOntology() {

    UUID artifactUUID = UUID.nameUUIDFromBytes(LOCAL_PATH.getBytes());

    ResourceIdentifier testOntologyId = SemanticIdentifier.newId(
        ESWC_ASSET_UUID,
        ONTOLOGY_VERSION
    );

    ResourceIdentifier testDocumentId = SemanticIdentifier.newId(
        artifactUUID,
        ONTOLOGY_VERSION
    );

    KnowledgeAsset metadata = new KnowledgeAsset()
        .withAssetId(testOntologyId)
        .withSecondaryId(SemanticIdentifier.newId(ESWC_ONTOLOGY_URI, ONTOLOGY_VERSION))
        .withName("Test")
        .withDescription("A test vocabulary")
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Formal_Ontology)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(testDocumentId)
            .withRepresentation(rep(OWL_2,RDF_XML_Syntax,XML_1_1))
            .withLocator(URI.create(LOCAL_PATH))
        );

    KnowledgeCarrier carrier = AbstractCarrier
        .of(TermsPublisher.class.getResourceAsStream(LOCAL_PATH))
        .withAssetId(testOntologyId)
        .withArtifactId(testDocumentId)
        .withRepresentation((metadata.getCarriers().get(0)).getRepresentation());

    assetRepo.publish(metadata,carrier);
  }

}
