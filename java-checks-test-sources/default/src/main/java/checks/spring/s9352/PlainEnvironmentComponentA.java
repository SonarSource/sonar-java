package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Scenario: a candidate profiled with an operator expression is excluded exactly like a simply profiled one,
// still leaving two ambiguous candidates — issue expected.
// Three candidates of type EnvironmentAware (this class, PlainEnvironmentComponentB,
// ExpressionProfiledEnvironmentComponent), used only by EnvironmentConsumer in this scenario. A distinct
// interface from the other scenarios in this package, so that a whole-module scan does not merge candidate
// pools across scenarios.
@Component
public class PlainEnvironmentComponentA implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
    // not needed for test
  }
}
