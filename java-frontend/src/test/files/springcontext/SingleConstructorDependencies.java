package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class SingleConstructorDependencies {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  SingleConstructorDependencies(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }
}
