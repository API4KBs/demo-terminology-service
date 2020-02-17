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

import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.language.parsers.sparql.SparqlLifter;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig;
import org.omg.demo.terms.TermsServer;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(
    basePackageClasses = {
        TermsServer.class,
        SparqlLifter.class},
    excludeFilters = {@Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {OWLParser.class})})
@PropertySource("classpath:application.properties")
@EnableAutoConfiguration
public class TermsServerConfig implements WebMvcConfigurer {

  @Bean
  @KPServer
  public KnowledgeArtifactRepositoryService artifactRepo() {
    return KnowledgeArtifactRepositoryService.inMemoryArtifactRepository();
  }

  @Bean
  public KnowledgeAssetRepositoryServerConfig config() {
    return new KnowledgeAssetRepositoryServerConfig();
  }

}
