package checks.spring.context;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class QualifiedBeanMethod {

  @Bean
  @Qualifier("myAlias")
  ApplicationContext myBean() {
    return null;
  }
}
