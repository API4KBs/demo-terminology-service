package org.omg.demo.terms.config;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

  @Bean
  @KPServer
  public KnowledgeAssetRepositoryService embeddedAssetRepository() {
    return KnowledgeAssetRepositoryService.selfContainedRepository();
  }

}
