package checks.spring.s9352;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

// See FallbackRegularComponent for context.
@Fallback
@Component
class FallbackComponent implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext ctx) {
    // not needed for test
  }
}
