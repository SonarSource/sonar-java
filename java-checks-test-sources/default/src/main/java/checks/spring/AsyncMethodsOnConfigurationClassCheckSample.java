package checks.spring;

import javax.annotation.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class AsyncMethodsOnConfigurationClassCheckSample {

  @Async // Noncompliant [[quickfixes=qf1]] {{Remove this "@Async" annotation from this method.}}
//^^^^^^
  // fix@qf1 {{Remove "@Async"}}
  // edit@qf1 [[sc=3;ec=9]] {{}}
  public void asyncMethod() {
  }

  public void method() { // Compliant
  }

  @Nullable
  @Async // Noncompliant
  public void someMethod() {
  }

}

class NotAConfigurationClass {

  @Async // Compliant
  public void asyncMethod() {
  }

}
