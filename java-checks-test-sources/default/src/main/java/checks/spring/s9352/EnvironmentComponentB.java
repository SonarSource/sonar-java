package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// See EnvironmentComponentA for context.
@Component
public class EnvironmentComponentB implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
    // not needed for test
  }
}
