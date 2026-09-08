package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Profile("prod")
@Component
class QualifiedFieldDependencies {

  @Autowired
  @Qualifier("primaryContext")
  private ApplicationContext applicationContext;

  @Autowired
  private Environment environment;
}
