package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class MixedInjectionDependencies {

  // Injected via implicit single-constructor injection
  private final ApplicationContext applicationContext;

  // Injected via field injection
  @Autowired
  private Environment environment;

  MixedInjectionDependencies(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }
}
