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

import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.parsers.OWLParser;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepository;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.kmdp.tranx.DetectApi;
import edu.mayo.kmdp.tranx.TransxionApi;
import edu.mayo.kmdp.tranx.server.DeserializeApiDelegate;
import edu.mayo.kmdp.tranx.server.DetectApiDelegate;
import edu.mayo.kmdp.tranx.server.TransxionApiDelegate;
import javax.inject.Inject;
import org.omg.demo.terms.TermsServer;
import org.omg.demo.terms.components.SPARQLParser;
import org.omg.demo.terms.server.TermsApiDelegate;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(
    basePackageClasses = {LanguageDeSerializer.class,SPARQLParser.class},
    excludeFilters = {@Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {OWLParser.class})})
@PropertySource("classpath:application.properties")
@EnableAutoConfiguration
public class TermsServerConfig implements WebMvcConfigurer {

  @Bean
  @KPServer
  public TermsApiDelegate server() {
    return new TermsServer();
  }


  @Bean
  @KPServer
  public KnowledgeAssetRepository assetRepository() {
    //TODO check
    return KnowledgeAssetRepository.newRepository();
  }


  @Bean
  @KPComponent
  @Inject
  public DetectApi detectApi(@KPServer DetectApiDelegate detector) {
    return DetectApi.newInstance(detector);
  }

  @Bean
  @KPComponent
  @Inject
  public TransxionApi executionApi(@KPServer TransxionApiDelegate txor) {
    return TransxionApi.newInstance(txor);
  }

  @Bean
  @KPComponent
  @Inject
  public DeserializeApi deserializeApi(@KPServer DeserializeApiDelegate parser) {
    return DeserializeApi.newInstance(parser);
  }



}
