package checks.spring.context;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@TestConfiguration
class TestConfigurationWithNestedConfiguration {

  @Configuration
  static class NestedConfiguration {

    @Bean
    ApplicationContext nestedApplicationContext() {
      return null;
    }
  }
}
