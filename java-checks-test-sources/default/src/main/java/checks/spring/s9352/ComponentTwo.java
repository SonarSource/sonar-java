package checks.spring.s9352;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// See ComponentOne for context.
@Component
public class ComponentTwo implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext ctx) {
    // not needed for test
  }
}
