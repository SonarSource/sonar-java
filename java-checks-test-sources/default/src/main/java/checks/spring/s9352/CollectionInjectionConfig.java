package checks.spring.s9352;

import java.util.List;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Same as CollectionInjectionConsumer, but for the injection points a @Bean factory method declares through its
// own parameters, which is the shape reported in SONARJAVA-6926. No issue expected.
@Configuration
public class CollectionInjectionConfig {

  @Bean
  public Runnable task(List<ApplicationContextAware> contextAwares, ApplicationContextAware[] moreContextAwares) {
    return () -> {
    };
  }
}
