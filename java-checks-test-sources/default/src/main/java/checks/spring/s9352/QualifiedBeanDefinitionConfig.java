package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Two @Bean methods both returning ApplicationContext; one carries @Qualifier("primaryCtx") whose value differs
// from the method name ("getPrimary"). A consumer that qualifies the injection point by "primaryCtx" should be
// disambiguated by the bean's @Qualifier, not raise a false positive.
@Configuration
public class QualifiedBeanDefinitionConfig {

  @Bean
  @Qualifier("primaryCtx")
  public ApplicationContext getPrimary() {
    return null;
  }

  @Bean
  public ApplicationContext getSecondary() {
    return null;
  }
}
