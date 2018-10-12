package my.app;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class A {

  public void nullableAttributes() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null) { // Compliant, getRequestAttributes is annotated with @org.springframework.lang.Nullable
    }
  }

  public void alwaysTrue() {
    RequestAttributes attributes = springNonNull();
    if (attributes != null) { // Noncompliant
    }
  }

  @org.springframework.lang.NonNull
  public Object springNonNull() {
    return unknown();
  }

}
