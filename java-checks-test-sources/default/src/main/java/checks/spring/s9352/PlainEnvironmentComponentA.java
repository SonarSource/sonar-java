package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PlainEnvironmentComponentA implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
  }
}
