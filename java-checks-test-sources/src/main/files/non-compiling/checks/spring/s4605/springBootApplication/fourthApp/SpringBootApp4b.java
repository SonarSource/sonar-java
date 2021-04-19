package checks.spring.s4605.springBootApplication.fourthApp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

public class SpringBootApp4b {

  public final String PACKAGE_NAME = "checks.spring.s4605.springBootApplication.fourthApp.other";

  // according to ecj, this reference to PACKAGE_NAME does not compile, as it is not static
  // wrong package... the component is in package "controller", not "other"
  @SpringBootApplication(scanBasePackages = PACKAGE_NAME)
  public class InnerSpringBootApp {
  }
}
