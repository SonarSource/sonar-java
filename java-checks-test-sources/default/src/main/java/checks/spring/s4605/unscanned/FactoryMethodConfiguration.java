package checks.spring.s4605.unscanned;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Lives outside every scanned package, yet neither the configuration class nor the bean its factory method
// registers is reachable by component scanning in the first place, so neither may be reported.
@Configuration
public class FactoryMethodConfiguration {

  @Bean
  public MyService myService() {
    return new MyService();
  }

  public static class MyService {
  }
}
