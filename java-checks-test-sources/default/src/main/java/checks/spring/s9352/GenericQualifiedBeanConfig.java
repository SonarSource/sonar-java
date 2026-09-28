package checks.spring.s9352;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Two @Bean methods with the same parameterized return type, so resolving generics cannot tell them apart;
// only their @Qualifier can. Mirrors the qualified generic beans reported in SONARJAVA-6926.
@Configuration
public class GenericQualifiedBeanConfig {

  @Bean
  @Qualifier("byId")
  Map<Integer, ApplicationContextAware> firstIndex() {
    return null;
  }

  @Bean
  @Qualifier("byName")
  Map<Integer, ApplicationContextAware> secondIndex() {
    return null;
  }
}
