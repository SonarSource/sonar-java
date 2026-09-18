package checks.spring.s9352;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Profile("dev & !test")
@Component
public class ExpressionProfiledEnvironmentComponent implements EnvironmentAware {

  @Override
  public void setEnvironment(Environment environment) {
  }
}
