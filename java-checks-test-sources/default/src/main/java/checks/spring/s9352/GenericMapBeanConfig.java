package checks.spring.s9352;

import java.util.Map;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

// Two @Bean methods returning maps that erasure collapses onto java.util.Map. An Integer key keeps a consumer
// of either one a single-bean dependency rather than the multi-bean shape.
@Configuration
public class GenericMapBeanConfig {

  @Bean
  Map<Integer, ApplicationContextAware> contextAwaresById() {
    return null;
  }

  @Bean
  Map<Integer, ResourceLoader> resourceLoadersById() {
    return null;
  }
}
