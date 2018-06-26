package another.foo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AnotherApplication {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}

@Component
public class AnotherFoo { } // Compliant
