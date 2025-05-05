package checks.tests.AssertionsInTestsCheck;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;

public class ForeignNonAssertingContextConsumer implements ContextConsumer<AssertableApplicationContext> {
  @Override
  public void accept(AssertableApplicationContext context) throws Throwable {
    // do something but no assertions
  }
}
