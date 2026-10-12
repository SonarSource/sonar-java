package checks.spring.context;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

class TestClassWithNestedConfiguration {

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

  static class NestedHolder {

    @Component
    static class DeeplyNestedComponent {
    }
  }
}
