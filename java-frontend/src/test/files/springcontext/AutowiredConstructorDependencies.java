package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class AutowiredConstructorDependencies {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  @Autowired
  AutowiredConstructorDependencies(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }
}
