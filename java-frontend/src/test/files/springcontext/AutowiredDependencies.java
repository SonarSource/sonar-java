package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class AutowiredDependencies {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private Environment environment;
}
