package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
class BlankQualifierDependency {

  @Autowired
  @Qualifier("")
  private ApplicationContext applicationContext;
}
