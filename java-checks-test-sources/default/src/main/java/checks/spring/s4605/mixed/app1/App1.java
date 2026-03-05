package checks.spring.s4605.mixed.app1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "checks.spring.s4605.mixed.app1.smth")})
public class App1 {
  static void main(String[] args) {
    SpringApplication.run(checks.spring.s4605.mixed.app1.App1.class, args);
  }
}
