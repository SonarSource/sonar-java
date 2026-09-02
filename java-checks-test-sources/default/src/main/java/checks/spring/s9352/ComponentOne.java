package checks.spring.s9352;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// Scenario: ambiguous dependency, issue expected.
// Two candidates of type ApplicationContextAware (this class and ComponentTwo), neither @Primary,
// used only by UnresolvedConsumer in this scenario.
@Component
public class ComponentOne implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext ctx) {
    // not needed for test
  }
}
