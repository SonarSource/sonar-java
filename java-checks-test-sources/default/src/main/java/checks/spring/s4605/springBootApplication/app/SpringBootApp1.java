package checks.spring.s4605.springBootApplication.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "checks.spring.s4605.springBootApplication.app.smth")})
public class SpringBootApp1 {

  public static void main(String[] args) {
    SpringApplication.run(SpringBootApp1.class, args);
  }
}

@Component
class App1 { // Compliant
}
