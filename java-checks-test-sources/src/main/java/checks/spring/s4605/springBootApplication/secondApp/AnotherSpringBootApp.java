package checks.spring.s4605.springBootApplication.secondApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AnotherSpringBootApp {

  public static void main(String[] args) {
    SpringApplication.run(AnotherSpringBootApp.class, args);
  }
}

@Component
class AnotherFoo { } // Compliant
