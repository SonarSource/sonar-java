package checks.spring.s4605.springBootApplication.fourthApp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import static checks.spring.s4605.springBootApplication.fourthApp.SpringBootApp4.PACKAGE_NAME2;

// wrong package... the component is in package "controller", not "domain" not "utility"
@SpringBootApplication(scanBasePackages = {SpringBootApp4.PACKAGE_NAME, PACKAGE_NAME2})
public class SpringBootApp4 {

  public static final String PACKAGE_NAME = "checks.spring.s4605.springBootApplication.fourthApp.domain";
  public static final String PACKAGE_NAME2 = "checks.spring.s4605.springBootApplication.fourthApp.utility";
}
