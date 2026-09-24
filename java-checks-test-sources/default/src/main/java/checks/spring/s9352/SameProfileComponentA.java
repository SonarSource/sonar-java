package checks.spring.s9352;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Scenario: two candidates declared on the very same profile are both active when it is, and are then
// ambiguous — issue expected on SameProfileConsumer.
@Profile("test")
@Component
public class SameProfileComponentA implements InitializingBean {

  @Override
  public void afterPropertiesSet() {
    // not needed for test
  }
}
