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

import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import javax.inject.Inject;
import org.omg.demo.terms.TermsApi;
import org.omg.demo.terms.config.TermsTestHelper;
import org.omg.demo.terms.server.TermsApiDelegate;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@PropertySource(value={"classpath:application.properties"})
public class TermsTestConfig {

  @Bean
  @Inject
  @KPComponent
  public TermsApi termsApi(@KPServer TermsApiDelegate termsServer) {
    return TermsApi.newInstance(termsServer);
  }

  @Bean
  @Inject
  @KPComponent
  public KnowledgeAssetRepositoryApi repositoryApi(@KPServer KnowledgeAssetRepository repo) {
    return KnowledgeAssetRepositoryApi.newInstance(repo);
  }

  @Bean
  @Inject
  @KPComponent
  public KnowledgeAssetCatalogApi catalogApi(@KPServer KnowledgeAssetRepository repo) {
    return KnowledgeAssetCatalogApi.newInstance(repo);
  }

  @Bean
  public TermsTestHelper helper() {
    return new TermsTestHelper();
  }

}
