package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class AutowiredConstructorWithUnannotatedConstructor {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  @Autowired
  AutowiredConstructorWithUnannotatedConstructor(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }

  // Spring ignores this constructor — its parameters must not appear as dependencies
  AutowiredConstructorWithUnannotatedConstructor(ApplicationContext ignoredContext) {
    this.applicationContext = ignoredContext;
    this.environment = null;
  }
}
