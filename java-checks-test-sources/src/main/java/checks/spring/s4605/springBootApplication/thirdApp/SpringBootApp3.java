package checks.spring.s4605.springBootApplication.thirdApp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// wrong package... the component is in package "controller", not "domain"
@SpringBootApplication(scanBasePackages = "checks.spring.s4605.springBootApplication.thirdApp.domain")
public class SpringBootApp3 {}
