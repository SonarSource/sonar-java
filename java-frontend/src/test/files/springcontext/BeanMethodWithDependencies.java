package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
class BeanMethodWithDependencies {

  @Bean
  Object myBean(ApplicationContext serviceA, Environment serviceB) {
    return new Object();
  }
}
