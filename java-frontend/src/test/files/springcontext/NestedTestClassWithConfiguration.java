package checks.spring.context;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class NestedTestClassWithConfiguration {

  static class InnerTest {

    @Test
    void application_context_is_customized() {
    }

    @Configuration
    static class NestedConfiguration {

      @Bean
      ApplicationContext nestedApplicationContext() {
        return null;
      }
    }
  }
}
