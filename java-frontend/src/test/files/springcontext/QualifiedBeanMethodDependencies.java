package checks.spring.context;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
class QualifiedBeanMethodDependencies {

  @Bean
  Object myBean(
    @Qualifier("primaryContext") ApplicationContext applicationContext,
    Environment environment) {
    return new Object();
  }
}
