package checks.spring;

import javax.annotation.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class AsyncMethodsOnConfigurationClassCheck {

  @Async // Noncompliant [[sc=3;ec=9]] {{Remove this "@Async" annotation from this method.}}
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
