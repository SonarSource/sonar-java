package checks.spring;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class AutowiredOnMultipleConstructorsCheckSample {

  private final Object myService;

  public AutowiredOnMultipleConstructorsCheckSample(Integer i, Object myService) {
    // ...
    this.myService = myService;
  }

  @Autowired
  public AutowiredOnMultipleConstructorsCheckSample(Object myService) {
    this.myService = myService;
  }

  @Autowired // Noncompliant [[sc=3;ec=13]] {{Remove this "@Autowired" annotation.}}
  public AutowiredOnMultipleConstructorsCheckSample(List<Object> list, Object myService) {
    // ...
    this.myService = myService;
  }

  public AutowiredOnMultipleConstructorsCheckSample(Long l, Object myService) {
    // ...
    this.myService = myService;
  }

  @Autowired // Noncompliant
  public AutowiredOnMultipleConstructorsCheckSample(Double d, Object myService) {
    // ...
    this.myService = myService;
  }

}

class Test {

  private final Object myService;

  @Autowired // Compliant
  public Test(Object myService) {
    this.myService = myService;
  }

}

class MyComponent {
  private final Object myService;

  @Autowired
  public MyComponent(Object myService) {
    this.myService = myService;
    // ...
  }

  @Autowired // Noncompliant
  public MyComponent(Object myService, Integer i) {
    this.myService = myService;
    // ...
  }

  @Autowired(required = true) // Noncompliant
  public MyComponent(Object myService, Integer i, String s) {
    this.myService = myService;
    // ...
  }

  public MyComponent(Object myService, int i) {
    this.myService = myService;
    // ...
  }

  @Autowired(required = false) // Compliant
  public MyComponent(Object myService, int i, String s) {
    this.myService = myService;
    // ...
  }
}
