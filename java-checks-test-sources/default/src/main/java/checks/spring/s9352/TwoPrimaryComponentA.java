package checks.spring.s9352;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Scenario: two candidates both marked @Primary, still ambiguous, issue expected. A single @Primary candidate
// disambiguates, but two conflicting primaries do not — Spring itself requires exactly one. A distinct interface
// from the other scenarios in this package, so that a whole-module scan does not merge candidate pools across
// scenarios.
@Primary
@Component
public class TwoPrimaryComponentA implements DisposableBean {

  @Override
  public void destroy() {
    // not needed for test
  }
}
