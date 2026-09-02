package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Scenario: @Qualifier disambiguates, no issue expected.
// Two candidates of type EnvironmentAware (this class and EnvironmentComponentB), used only by
// QualifierConsumer in this scenario. A distinct interface from the other scenarios in this package, so that a
// whole-module scan does not merge candidate pools across scenarios.
@Component
public class EnvironmentComponentA implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
    // not needed for test
  }
}
