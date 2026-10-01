package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// See PlainEnvironmentComponentA for context.
@Component
public class PlainEnvironmentComponentB implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
    // not needed for test
  }
}
