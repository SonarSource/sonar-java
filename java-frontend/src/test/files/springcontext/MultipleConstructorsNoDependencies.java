package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class MultipleConstructorsNoDependencies {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  MultipleConstructorsNoDependencies(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    this.environment = null;
  }

  MultipleConstructorsNoDependencies(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }
}
