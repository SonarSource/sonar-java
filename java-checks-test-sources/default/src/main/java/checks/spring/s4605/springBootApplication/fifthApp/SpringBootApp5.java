package checks.spring.s4605.springBootApplication.fifthApp;

import checks.spring.s4605.springBootApplication.fifthApp.service.ServiceMarker;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
  scanBasePackages = "checks.spring.s4605.springBootApplication.fifthApp.extra",
  scanBasePackageClasses = ServiceMarker.class)
public class SpringBootApp5 {}
